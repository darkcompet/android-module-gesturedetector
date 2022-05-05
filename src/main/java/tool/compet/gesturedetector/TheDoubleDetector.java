package tool.compet.gesturedetector;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class TheDoubleDetector extends MyDetector {
	public interface Listener {
		boolean onDoubleTap(TheDoubleDetector detector);
	}

	TheDoubleDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;
	
	// Slops
	int tapTimeOut;
	int tapSlopSquare;

	// Tap time in millis.
	long firstTime;
	long secondTime;

	// Tap coordinate
	float firstPivotX;
	float firstPivotY;
	float secondPivotX;
	float secondPivotY;

	@Override
	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		// DOUBLE-TAP detection
		// Definition: w-tap is considered as double-click.
		// Detect at action-up !
		// Main reason to detect at action-up since
		// we allow user cancel by moving fingers to make detection-condition failed.
		if (eventInfo.actionUp) {
			// First time
			if (this.firstTime == 0) {
				this.firstTime = System.currentTimeMillis();
				this.firstPivotX = eventInfo.pivotX;
				this.firstPivotY = eventInfo.pivotY;
			}
			// Second time
			else if (this.secondTime == 0) {
				this.secondTime = System.currentTimeMillis();
				this.secondPivotX = eventInfo.pivotX;
				this.secondPivotY = eventInfo.pivotY;
			}
			// Detect time
			else if (this.firstTime > 0 && this.secondTime > 0) {
				// Detect from first-down and second-down.
				// Note: Handler timeout is another option to check double-tap.
				final float dx = this.secondPivotX - this.firstPivotX;
				final float dy = this.secondPivotY - this.firstPivotY;
				final boolean isDoubleTap =
					(this.secondTime - this.firstTime <= this.tapTimeOut) &&
						(dx * dx + dy * dy) <= this.tapSlopSquare;

				if (isDoubleTap) {
					// DkLogcats.debug(this, "----> DOUBLE-TAP: %s", this.toString());

					if (listener.onDoubleTap(this)) {
						handled = true;
						this.accept();
					}
				}

				// Complete detection
				this.firstTime = 0;
				this.secondTime = 0;
			}
		}
		return handled;
	}

	/**
	 * Call when detection completed.
	 */
	void accept() {
		firstTime = secondTime;
		firstPivotX = secondPivotX;
		firstPivotY = secondPivotY;
	}

	public float dx() {
		return secondPivotX - firstPivotX;
	}

	public float dy() {
		return secondPivotY - firstPivotY;
	}

	/**
	 * @return Time in millis between first-up time and second-up time.
	 */
	public long elapsed() {
		return secondTime - firstTime;
	}

	@Override
	public String toString() {
		return "dx: " + dx() + ", dy: " + dy() + ", elapsed: " + elapsed();
	}
}
