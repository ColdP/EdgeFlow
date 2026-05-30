package btm.m.edgeflow.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * Edge trigger strip with bidirectional swipe detection.
 * When [isRightSide] is true, swipes are reversed (left instead of right).
 */
class EdgeTriggerView(context: Context) : View(context) {

    companion object {
        private const val Y_TOLERANCE_PX = 120
    }

    var swipeThresholdPx: Int = 40
    var isRightSide: Boolean = false

    private var startX = 0f
    private var startY = 0f
    private var isTracking = false
    private var hasTriggered = false

    private var onGestureDetectedListener: (() -> Unit)? = null

    fun setOnGestureDetectedListener(listener: () -> Unit) {
        onGestureDetectedListener = listener
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) updateGestureExclusionRects()
    }

    private fun updateGestureExclusionRects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val h = height
            val exclH = (h * 0.5f).toInt()
            val top = (h - exclH) / 2
            ViewCompat.setSystemGestureExclusionRects(this, listOf(Rect(0, top, width, top + exclH)))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                isTracking = true
                hasTriggered = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTracking || hasTriggered) return true
                val dx = event.rawX - startX
                val dy = abs(event.rawY - startY)
                // Left edge: swipe right (dx > threshold)
                // Right edge: swipe left (dx < -threshold)
                val triggered = if (isRightSide) dx < -swipeThresholdPx else dx > swipeThresholdPx
                if (triggered && dy < Y_TOLERANCE_PX) {
                    hasTriggered = true
                    onGestureDetectedListener?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTracking = false
                hasTriggered = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}