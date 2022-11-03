package tool.compet.gesturedetector

import android.view.MotionEvent

class TheDoubleDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onDoubleTap(detector: TheDoubleDetector?): Boolean
	}

	// Slops
	var tapTimeOut = 0
	var tapSlopSquare = 0

	// Tap time in millis.
	var firstTime: Long = 0
	var secondTime: Long = 0

	// Tap coordinate
	var firstPivotX = 0f
	var firstPivotY = 0f
	var secondPivotX = 0f
	var secondPivotY = 0f
	public override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false

		// DOUBLE-TAP detection
		// Definition: w-tap is considered as double-click.
		// Detect at action-up !
		// Main reason to detect at action-up since
		// we allow user cancel by moving fingers to make detection-condition failed.
		if (eventInfo.actionUp) {
			// First time
			if (firstTime == 0L) {
				firstTime = System.currentTimeMillis()
				firstPivotX = eventInfo.pivotX
				firstPivotY = eventInfo.pivotY
			}
			else if (secondTime == 0L) {
				secondTime = System.currentTimeMillis()
				secondPivotX = eventInfo.pivotX
				secondPivotY = eventInfo.pivotY
			}
			else if (firstTime > 0 && secondTime > 0) {
				// Detect from first-down and second-down.
				// Note: Handler timeout is another option to check double-tap.
				val dx = secondPivotX - firstPivotX
				val dy = secondPivotY - firstPivotY
				val isDoubleTap = secondTime - firstTime <= tapTimeOut &&
					dx * dx + dy * dy <= tapSlopSquare
				if (isDoubleTap) {
					// DkLogcats.debug(this, "----> DOUBLE-TAP: %s", this.toString());
					if (listener.onDoubleTap(this)) {
						handled = true
						accept()
					}
				}

				// Complete detection
				firstTime = 0
				secondTime = 0
			}
		}
		return handled
	}

	/**
	 * Call when detection completed.
	 */
	fun accept() {
		firstTime = secondTime
		firstPivotX = secondPivotX
		firstPivotY = secondPivotY
	}

	fun dx(): Float {
		return secondPivotX - firstPivotX
	}

	fun dy(): Float {
		return secondPivotY - firstPivotY
	}

	/**
	 * @return Time in millis between first-up time and second-up time.
	 */
	fun elapsed(): Long {
		return secondTime - firstTime
	}

	override fun toString(): String {
		return "dx: " + dx() + ", dy: " + dy() + ", elapsed: " + elapsed()
	}
}