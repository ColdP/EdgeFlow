package btm.m.edgeflow.engine

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages privilege detection and shell execution.
 *
 * ## Root-first strategy:
 *  1. On init, probe for `su` binary. If found → activate Root mode immediately.
 *  2. Only if no Root → initialize Shizuku listeners and request permission.
 */
@Singleton
class PrivilegeManager @Inject constructor() {

    companion object {
        private const val TAG = "PrivilegeManager"
    }

    private val _shizukuAvailable = MutableStateFlow(false)
    val shizukuAvailable: StateFlow<Boolean> = _shizukuAvailable.asStateFlow()

    private var shizukuInitialized = false

    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable.asStateFlow()

    private val _hasPrivilege = MutableStateFlow(false)
    /** True if either Root or Shizuku is available for privileged execution. */
    val hasPrivilege: StateFlow<Boolean> = _hasPrivilege.asStateFlow()

    private fun updateCombined() {
        _hasPrivilege.value = _rootAvailable.value || _shizukuAvailable.value
    }

    /**
     * Initializes privilege detection. Root-first strategy.
     */
    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val hasRoot = probeSu()
            _rootAvailable.value = hasRoot
            updateCombined()
            Log.d(TAG, "Root probe result: $hasRoot")

            if (!hasRoot) {
                // No Root → try Shizuku on main thread (Shizuku API requires it)
                withContext(Dispatchers.Main) {
                    initShizuku(context)
                }
            } else {
                Log.d(TAG, "Root detected, skipping Shizuku")
            }
        }
    }

    // ── Shizuku ──────────────────────────────────────────────────────────────

    private fun initShizuku(context: Context) {
        if (shizukuInitialized) return
        shizukuInitialized = true

        val hasShizuku = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }

        if (!hasShizuku) {
            Log.d(TAG, "Shizuku is not installed")
            _shizukuAvailable.value = false
            updateCombined()
            return
        }

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        checkShizukuPermission()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        checkShizukuPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        _shizukuAvailable.value = false
        updateCombined()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Shizuku permission result: granted=$granted")
            _shizukuAvailable.value = granted
            updateCombined()
        }

    private fun checkShizukuPermission() {
        if (_rootAvailable.value) return
        try {
            if (!Shizuku.pingBinder()) {
                _shizukuAvailable.value = false
                updateCombined()
                return
            }
            if (Shizuku.getUid() == 0 || Shizuku.getUid() == 2000) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    _shizukuAvailable.value = true
                } else {
                    Shizuku.requestPermission(0)
                }
            } else {
                _shizukuAvailable.value = false
            }
            updateCombined()
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku check failed", e)
            _shizukuAvailable.value = false
            updateCombined()
        }
    }

    fun requestShizukuPermission() {
        if (_rootAvailable.value) return
        try {
            Shizuku.requestPermission(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    // ── Root probe ───────────────────────────────────────────────────────────

    private fun probeSu(): Boolean {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/adb/ksu/bin/su", "/data/adb/magisk/su"
        )
        val hasBinary = suPaths.any { path ->
            try {
                val file = java.io.File(path)
                file.exists() && file.canExecute()
            } catch (_: Exception) { false }
        }
        if (hasBinary) return true

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            output.contains("uid=0")
        } catch (_: Exception) { false }
    }

    // ── Shell execution ──────────────────────────────────────────────────────

    /** Priority: Root (su) → Shizuku → failure. */
    suspend fun executeShellCommand(command: String): Boolean {
        return withContext(Dispatchers.IO) {
            when {
                _rootAvailable.value -> executeViaSu(command)
                _shizukuAvailable.value -> executeViaShizuku(command)
                else -> {
                    Log.e(TAG, "No privilege for: $command")
                    false
                }
            }
        }
    }

    private fun executeViaSu(command: String): Boolean {
        return try {
            Log.d(TAG, "su: $command")
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) { Log.e(TAG, "su failed", e); false }
    }

    private fun executeViaShizuku(command: String): Boolean {
        return try {
            Log.d(TAG, "shizuku: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) { Log.e(TAG, "shizuku failed", e); false }
    }

    fun release() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) {}
    }
}