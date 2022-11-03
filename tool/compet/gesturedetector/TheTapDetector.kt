package tool.compet.gesturedetector

import android.view.MotionEvent

// TAP detection
// Definition: tap is considered as click.
class TheTapDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onTap(detector: TheTapDetector?): Boolean
	}

	var tapSlopSquare = 0f
	var pivotX = 0f
	var pivotY = 0f
	var lastPivotX = 0f
	var lastPivotY = 0f

	override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false
		if (eventInfo.actionDown) {
			pivotX = eventInfo.pivotX
			lastPivotX = pivotX
			pivotY = eventInfo.pivotY
			lastPivotY = pivotY
		}
		else if (eventInfo.actionUp) {
			pivotX = eventInfo.pivotX
			pivotY = eventInfo.pivotY
			val dx = pivotX - lastPivotX
			val dy = pivotY - lastPivotY
			if (dx * dx + dy * dy <= tapSlopSquare) {
				// DkLogcats.debug(this, "----> TAP: %s", this.toString());
				if (listener.onTap(this)) {
					handled = true
					accept()
				}
			}
		}
		return handled
	}

	fun x(): Float {
		return (lastPivotX + pivotX) / 2f
	}

	fun y(): Float {
		return (lastPivotY + pivotY) / 2f
	}

	fun accept() {
		lastPivotX = pivotX
		lastPivotY = pivotY
	}

	override fun toString(): String {
		return "x: " + x() + ", y: " + y()
	}
}