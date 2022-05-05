package tool.compet.gesturedetector;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

// SCALE detection
// By default, we require at least 2 pointers perform scale
public class TheScaleDetector extends MyDetector {
	public interface Listener {
		boolean onScale(TheScaleDetector detector);
	}

	TheScaleDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;
	
	boolean inProgress;

	float spanSlop;

	float pivotX;
	float pivotY;
	float lastPivotX;
	float lastPivotY;

	// Average radius (span) from the pivot point to pointers
	float R;
	float Rx;
	float Ry;
	float lastR;
	float lastRx;
	float lastRy;

	@Override
	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		if (eventInfo.pointerCount >= 2) {
			// Calculate average Radius from the Pivot to pointers
			float sumRx = 0;
			float sumRy = 0;
			for (int pointerIndex = 0; pointerIndex < eventInfo.pointerCount; ++pointerIndex) {
				if (pointerIndex != eventInfo.skipPointerIndex) {
					sumRx += Math.abs(eventInfo.pxList.get(pointerIndex) - eventInfo.pivotX);
					sumRy += Math.abs(eventInfo.pyList.get(pointerIndex) - eventInfo.pivotY);
				}
			}
			final float Rx = sumRx / eventInfo.divPointerCount;
			final float Ry = sumRy / eventInfo.divPointerCount;
			final float R = (float) Math.hypot(Rx, Ry);

			if (eventInfo.configChanged) {
				this.inProgress = false;
				this.lastPivotX = this.pivotX = eventInfo.pivotX;
				this.lastPivotY = this.pivotY = eventInfo.pivotY;
				this.lastR = this.R = R;
				this.lastRx = this.Rx = Rx;
				this.lastRy = this.Ry = Ry;
			}
			// Detect at action-move !
			else if (eventInfo.actionMove) {
				this.pivotX = eventInfo.pivotX;
				this.pivotY = eventInfo.pivotY;
				this.Rx = Rx;
				this.Ry = Ry;
				this.R = R;

				if (! this.inProgress && Math.abs(R - this.lastR) >= this.spanSlop) {
					this.inProgress = true;
					this.accept();
				}
				else if (this.inProgress) {
					// DkLogcats.debug(this, "----> SCALE: %s", this.toString());

					if (listener.onScale(this)) {
						handled = true;
						this.accept();
					}
				}
			}
			else if (eventInfo.streamCompleted) {
				this.inProgress = false;
			}
		}
		return handled;
	}

	/**
	 * Call this when finish detection.
	 */
	void accept() {
		lastPivotX = pivotX;
		lastPivotY = pivotY;
		lastR = R;
		lastRx = Rx;
		lastRy = Ry;
	}

	/**
	 * @return Scaled-ratio between current span and last span.
	 */
	public float scaleFactor() {
		return lastR > 0 ? R / lastR : 1;
	}

	/**
	 * @return X coordinate of pivot point which be rotated around.
	 */
	public float pivotX() {
		return (lastPivotX + pivotX) / 2f;
	}

	/**
	 * @return Y coordinate of pivot point which be rotated around.
	 */
	public float pivotY() {
		return (lastPivotY + pivotY) / 2f;
	}

	@Override
	public String toString() {
		return "scaleFactor: " + scaleFactor();
	}
}
