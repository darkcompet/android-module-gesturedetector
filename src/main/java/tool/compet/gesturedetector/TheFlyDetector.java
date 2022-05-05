package tool.compet.gesturedetector;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.annotation.NonNull;

public class TheFlyDetector extends MyDetector {
	public interface Listener {
		boolean onFly(TheFlyDetector detector);
	}

	TheFlyDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;
	
	// Config
	int minVelocity; // px/pec
	int maxVelocity; // px/pec

	// After obtain and use it, must recyle for other place re-use
	VelocityTracker velocityTracker;

	// Detection info
	float velocityX; // x-velocity
	float velocityY; // y-velocity

	@Override
	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		if (eventInfo.configChanged) {
			if (this.velocityTracker == null) {
				this.velocityTracker = VelocityTracker.obtain();
			}
		}

		this.velocityTracker.addMovement(event);

		// Detect at action-up !
		// We detect Fly at action-up since an action is
		// considered as Fly iff it performs action-up.
		if (eventInfo.actionUp) {
			final int mainPointerId = event.getPointerId(0);
			this.velocityTracker.computeCurrentVelocity(1000, this.maxVelocity);

			final float velocityX = this.velocityX = this.velocityTracker.getXVelocity(mainPointerId);
			final float velocityY = this.velocityY = this.velocityTracker.getXVelocity(mainPointerId);

			if (velocityX >= this.minVelocity || velocityY >= this.minVelocity) {
				// DkLogcats.debug(this, "----> FLY: %s", this.toString());

				if (listener.onFly(this)) {
					handled = true;
					this.accept();
				}
			}
		}
		// Don't else here
		if (eventInfo.streamCompleted) {
			this.complete();
		}
		return handled;
	}

	void accept() {
	}

	void complete() {
		if (this.velocityTracker != null) {
			this.velocityTracker.recycle();
			this.velocityTracker = null;
		}
	}

	/**
	 * @return X-velocity
	 */
	public float velocityX() {
		return this.velocityX;
	}

	/**
	 * @return Y-velocity
	 */
	public float velocityY() {
		return this.velocityY;
	}

	@Override
	public String toString() {
		return "vx: " + velocityX() + ", vy: " + velocityY();
	}
}
