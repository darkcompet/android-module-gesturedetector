package tool.compet.gesturedetector

import android.view.MotionEvent
import android.view.VelocityTracker

class TheFlyDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onFly(detector: TheFlyDetector?): Boolean
	}

	// Config
	var minVelocity // px/pec
		= 0
	var maxVelocity // px/pec
		= 0

	// After obtain and use it, must recyle for other place re-use
	var velocityTracker: VelocityTracker? = null

	// Detection info
	var velocityX // x-velocity
		= 0f
	var velocityY // y-velocity
		= 0f

	public override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false
		if (eventInfo.configChanged) {
			if (velocityTracker == null) {
				velocityTracker = VelocityTracker.obtain()
			}
		}
		velocityTracker!!.addMovement(event)

		// Detect at action-up !
		// We detect Fly at action-up since an action is
		// considered as Fly iff it performs action-up.
		if (eventInfo.actionUp) {
			val mainPointerId = event.getPointerId(0)
			velocityTracker!!.computeCurrentVelocity(1000, maxVelocity.toFloat())
			velocityX = velocityTracker!!.getXVelocity(mainPointerId)
			val velocityX = velocityX
			velocityY = velocityTracker!!.getXVelocity(mainPointerId)
			val velocityY = velocityY
			if (velocityX >= minVelocity || velocityY >= minVelocity) {
				// DkLogcats.debug(this, "----> FLY: %s", this.toString());
				if (listener.onFly(this)) {
					handled = true
					accept()
				}
			}
		}
		// Don't else here
		if (eventInfo.streamCompleted) {
			complete()
		}
		return handled
	}

	fun accept() {}
	fun complete() {
		if (velocityTracker != null) {
			velocityTracker!!.recycle()
			velocityTracker = null
		}
	}

	/**
	 * @return X-velocity
	 */
	fun velocityX(): Float {
		return velocityX
	}

	/**
	 * @return Y-velocity
	 */
	fun velocityY(): Float {
		return velocityY
	}

	override fun toString(): String {
		return "vx: " + velocityX() + ", vy: " + velocityY()
	}
}