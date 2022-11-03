package tool.compet.gesturedetector

import android.view.MotionEvent

class TheDragDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onDrag(detector: TheDragDetector?): Boolean
	}

	var inProgress = false
	var dragSlopSquare = 0f
	var pivotX = 0f
	var pivotY = 0f
	var lastPivotX = 0f
	var lastPivotY = 0f
	public override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false
		if (eventInfo.configChanged) {
			inProgress = false
			pivotX = eventInfo.pivotX
			lastPivotX = pivotX
			pivotY = eventInfo.pivotY
			lastPivotY = pivotY
		}
		else if (eventInfo.actionMove) {
			pivotX = eventInfo.pivotX
			pivotY = eventInfo.pivotY
			if (!inProgress) {
				val dx = eventInfo.pivotX - lastPivotX
				val dy = eventInfo.pivotY - lastPivotY
				if (dx * dx + dy * dy >= dragSlopSquare) {
					inProgress = true
					accept()
				}
			}
			else {
				// DkLogcats.debug(this, "----> DRAG: %s", this.toString());
				if (listener.onDrag(this)) {
					handled = true
					accept()
				}
			}
		}
		else if (eventInfo.streamCompleted) {
			inProgress = false
		}
		return handled
	}

	fun accept() {
		lastPivotX = pivotX
		lastPivotY = pivotY
	}

	fun dx(): Float {
		return pivotX - lastPivotX
	}

	fun dy(): Float {
		return pivotY - lastPivotY
	}

	fun distance(): Float {
		return Math.hypot(dx().toDouble(), dy().toDouble()).toFloat()
	}

	override fun toString(): String {
		return "dx: " + dx() + ", dy: " + dy()
	}
}