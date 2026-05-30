package btm.m.edgeflow.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Manages the lifecycle and positioning of WindowManager overlay views.
 *
 * Handles:
 *  - Edge trigger strips (invisible gesture zones) with system gesture exclusion
 *  - Full-screen sidebar panel overlays (ComposeView) with immersive flags
 *  - Dynamic focus management (FLAG_NOT_FOCUSABLE toggling)
 *  - Proper lifecycle ownership for ComposeView hosted in WindowManager
 *  - Back key interception via KeyInterceptingFrameLayout
 */
class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var edgeStripView: EdgeTriggerView? = null
    private var sidebarComposeView: ComposeView? = null
    private var sidebarContainer: KeyInterceptingFrameLayout? = null
    private var sidebarParams: WindowManager.LayoutParams? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null

    /**
     * Adds the invisible edge trigger strip to the screen.
     */
    fun addEdgeTriggerStrip(
        side: Int = Gravity.LEFT,
        widthPx: Int = (24 * context.resources.displayMetrics.density).toInt(),
        heightPx: Int = WindowManager.LayoutParams.MATCH_PARENT,
        yOffsetPx: Int = 0,
        sensitivityPx: Int = 40,
        highlightDebug: Boolean = false,
        onGestureDetected: () -> Unit
    ) {
        if (edgeStripView != null) return

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = side or Gravity.CENTER_VERTICAL
            y = yOffsetPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val view = EdgeTriggerView(context).apply {
            setOnGestureDetectedListener(onGestureDetected)
            if (highlightDebug) {
                setBackgroundColor(0x55FF0000.toInt()) // Semi-transparent red for debug
            }
        }

        try {
            windowManager.addView(view, params)
            edgeStripView = view
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to add edge trigger strip", e)
        }
    }

    /**
     * Adds a full-screen immersive sidebar ComposeView overlay.
     *
     * Uses a [KeyInterceptingFrameLayout] as root container to intercept BACK key events.
     * The ComposeView is wrapped inside this container.
     *
     * Uses FLAG_LAYOUT_NO_LIMITS + FLAG_LAYOUT_IN_SCREEN to render beneath
     * the system status bar and navigation bar for a true edge-to-edge experience.
     */
    fun addSidebarOverlay(
        viewModelStoreOwner: ViewModelStoreOwner? = null,
        onBackPressIntercepted: (() -> Unit)? = null,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        if (sidebarContainer != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Android 12+: system-level background blur
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = 60
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        val composeView = ComposeView(context).apply {
            setContent(content)
        }

        // Wrap ComposeView in KeyInterceptingFrameLayout for back key interception
        val container = KeyInterceptingFrameLayout(context).apply {
            addView(composeView, android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ))
            onBackPressed = onBackPressIntercepted
        }

        // ViewTree owners MUST be set on the container (not ComposeView),
        // because Compose resolution walks UP the view tree from ComposeView
        // to find the nearest LifecycleOwner on any ancestor view.
        container.setViewTreeLifecycleOwner(lifecycleOwner)
        container.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        if (viewModelStoreOwner != null) {
            container.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        }

        try {
            windowManager.addView(container, params)
            sidebarContainer = container
            sidebarComposeView = composeView
            sidebarParams = params
            overlayLifecycleOwner = lifecycleOwner
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to add sidebar overlay", e)
            lifecycleOwner.onDestroy()
        }
    }

    /**
     * Removes FLAG_NOT_FOCUSABLE from the sidebar overlay so it can intercept
     * back press events. Call this when the sidebar is fully expanded.
     */
    fun setSidebarFocusable(focusable: Boolean) {
        val container = sidebarContainer ?: return
        val params = sidebarParams ?: return

        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try {
            windowManager.updateViewLayout(container, params)
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to update focus flags", e)
        }
    }

    /** Removes the sidebar overlay if it is currently attached. Crash-safe. */
    fun removeSidebarOverlay() {
        sidebarContainer?.let { container ->
            try {
                if (container.isAttachedToWindow) {
                    windowManager.removeView(container)
                } else {
                    android.util.Log.w("OverlayManager", "Sidebar container already detached, skipping removeView")
                }
            } catch (e: Exception) {
                android.util.Log.e("OverlayManager", "Failed to remove sidebar overlay", e)
            }
        }
        overlayLifecycleOwner?.onDestroy()
        sidebarContainer = null
        sidebarComposeView = null
        sidebarParams = null
        overlayLifecycleOwner = null
    }

    /** Sets the back press interceptor on the KeyInterceptingFrameLayout. */
    fun setBackPressInterceptor(interceptor: (() -> Unit)?) {
        sidebarContainer?.onBackPressed = interceptor
    }

    /** Removes the edge trigger strip if it is currently attached. */
    fun removeEdgeTriggerStrip() {
        edgeStripView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                android.util.Log.e("OverlayManager", "Failed to remove edge trigger strip", e)
            }
        }
        edgeStripView = null
    }

    /** Returns the current edge trigger view, or null. */
    fun getEdgeTriggerView(): EdgeTriggerView? = edgeStripView

    /** Returns the current edge trigger LayoutParams, or null. */
    fun getEdgeTriggerParams(): WindowManager.LayoutParams? {
        val view = edgeStripView ?: return null
        return view.layoutParams as? WindowManager.LayoutParams
    }

    /** Dynamically updates the edge trigger strip LayoutParams (hot-reload). MUST be called on Main thread. */
    fun updateEdgeTriggerLayoutParams(params: WindowManager.LayoutParams) {
        val view = edgeStripView ?: return
        if (!view.isAttachedToWindow) {
            android.util.Log.w("OverlayManager", "Edge trigger view not attached, skipping updateViewLayout")
            return
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to update edge trigger params", e)
        }
    }

    /** Returns the current LayoutParams of the sidebar overlay, or null if not attached. */
    fun getSidebarLayoutParams(): WindowManager.LayoutParams? = sidebarParams

    /** Updates the sidebar overlay's LayoutParams (e.g. blurBehindRadius). MUST be called on Main thread. */
    fun updateSidebarLayoutParams(params: WindowManager.LayoutParams) {
        val container = sidebarContainer ?: return
        if (!container.isAttachedToWindow) {
            android.util.Log.w("OverlayManager", "Sidebar container not attached, skipping updateViewLayout")
            return
        }
        try {
            windowManager.updateViewLayout(container, params)
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Failed to update sidebar params", e)
        }
    }

    /** Returns true if the sidebar overlay is currently attached. */
    fun isSidebarShowing(): Boolean = sidebarContainer != null

    /** Cleans up all overlay views. */
    fun removeAll() {
        removeSidebarOverlay()
        removeEdgeTriggerStrip()
    }

    /**
     * Synthetic [LifecycleOwner] + [SavedStateRegistryOwner] for ComposeView
     * hosted inside a WindowManager overlay.
     */
    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun onResume() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun onDestroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}