package tool.compet.gesturedetector

import android.view.MotionEvent

// SCALE detection
// By default, we require at least 2 pointers perform scale
class TheScaleDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onScale(detector: TheScaleDetector): Boolean
	}

	private var inProgress = false
	internal var spanSlop = 0f
	private var pivotX = 0f
	private var pivotY = 0f
	private var lastPivotX = 0f
	private var lastPivotY = 0f

	// Average radius (span) from the pivot point to pointers
	private var R = 0f
	private var Rx = 0f
	private var Ry = 0f
	private var lastR = 0f
	private var lastRx = 0f
	private var lastRy = 0f

	override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false
		if (eventInfo.pointerCount >= 2) {
			// Calculate average Radius from the Pivot to pointers
			var sumRx = 0f
			var sumRy = 0f
			for (pointerIndex in 0 until eventInfo.pointerCount) {
				if (pointerIndex != eventInfo.skipPointerIndex) {
					sumRx += Math.abs(eventInfo.pxList[pointerIndex] - eventInfo.pivotX)
					sumRy += Math.abs(eventInfo.pyList[pointerIndex] - eventInfo.pivotY)
				}
			}
			val Rx = sumRx / eventInfo.divPointerCount
			val Ry = sumRy / eventInfo.divPointerCount
			val R = Math.hypot(Rx.toDouble(), Ry.toDouble()).toFloat()
			if (eventInfo.configChanged) {
				inProgress = false
				pivotX = eventInfo.pivotX
				lastPivotX = pivotX
				pivotY = eventInfo.pivotY
				lastPivotY = pivotY
				this.R = R
				lastR = this.R
				this.Rx = Rx
				lastRx = this.Rx
				this.Ry = Ry
				lastRy = this.Ry
			}
			else if (eventInfo.actionMove) {
				pivotX = eventInfo.pivotX
				pivotY = eventInfo.pivotY
				this.Rx = Rx
				this.Ry = Ry
				this.R = R
				if (!inProgress && Math.abs(R - lastR) >= spanSlop) {
					inProgress = true
					accept()
				}
				else if (inProgress) {
					// DkLogcats.debug(this, "----> SCALE: %s", this.toString());
					if (listener.onScale(this)) {
						handled = true
						accept()
					}
				}
			}
			else if (eventInfo.streamCompleted) {
				inProgress = false
			}
		}
		return handled
	}

	/**
	 * Call this when finish detection.
	 */
	fun accept() {
		lastPivotX = pivotX
		lastPivotY = pivotY
		lastR = R
		lastRx = Rx
		lastRy = Ry
	}

	/**
	 * @return Scaled-ratio between current span and last span.
	 */
	fun scaleFactor(): Float {
		return if (lastR > 0) R / lastR else 1f
	}

	/**
	 * @return X coordinate of pivot point which be rotated around.
	 */
	fun pivotX(): Float {
		return (lastPivotX + pivotX) / 2f
	}

	/**
	 * @return Y coordinate of pivot point which be rotated around.
	 */
	fun pivotY(): Float {
		return (lastPivotY + pivotY) / 2f
	}

	override fun toString(): String {
		return "scaleFactor: " + scaleFactor()
	}
}