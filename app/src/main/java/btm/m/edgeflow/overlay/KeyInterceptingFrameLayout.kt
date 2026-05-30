package btm.m.edgeflow.overlay

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * A FrameLayout wrapper that intercepts the BACK key event.
 *
 * Used to wrap the ComposeView hosted in WindowManager so that
 * pressing the system back button triggers the sidebar dismiss animation
 * instead of being ignored (ComposeView in WindowManager has no Lifecycle
 * owner to dispatch BackHandler events).
 *
 * Usage:
 * 1. Create this FrameLayout as the root container for the ComposeView.
 * 2. Set [onBackPressed] to trigger sidebar dismiss animation (progress.animateTo(0f)).
 * 3. Toggle FLAG_NOT_FOCUSABLE in WindowManager: remove when panel opens,
 *    add back when panel closes — so this container can receive key events.
 */
class KeyInterceptingFrameLayout(context: Context) : FrameLayout(context) {

    /**
     * Callback invoked when the system BACK key is pressed.
     * Set this to trigger the sidebar dismiss animation.
     */
    var onBackPressed: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackPressed?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}