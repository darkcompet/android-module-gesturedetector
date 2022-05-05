package tool.compet.gesturedetector;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

// TAP detection
// Definition: tap is considered as click.
public class TheTapDetector extends MyDetector {
	public interface Listener {
		boolean onTap(TheTapDetector detector);
	}

	TheTapDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;

	float tapSlopSquare;

	float pivotX;
	float pivotY;

	float lastPivotX;
	float lastPivotY;

	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		if (eventInfo.actionDown) {
			this.lastPivotX = this.pivotX = eventInfo.pivotX;
			this.lastPivotY = this.pivotY = eventInfo.pivotY;
		}
		// Detect at action-up !
		// Main reason to detect at eventInfo.action-up since
		// we allow user cancel by moving fingers to make detection-condition failed.
		else if (eventInfo.actionUp) {
			this.pivotX = eventInfo.pivotX;
			this.pivotY = eventInfo.pivotY;
			final float dx = this.pivotX - this.lastPivotX;
			final float dy = this.pivotY - this.lastPivotY;

			if (dx * dx + dy * dy <= this.tapSlopSquare) {
				// DkLogcats.debug(this, "----> TAP: %s", this.toString());

				if (listener.onTap(this)) {
					handled = true;
					this.accept();
				}
			}
		}

		return handled;
	}

	public float x() {
		return (lastPivotX + pivotX) / 2f;
	}

	public float y() {
		return (lastPivotY + pivotY) / 2f;
	}

	void accept() {
		lastPivotX = pivotX;
		lastPivotY = pivotY;
	}

	@Override
	public String toString() {
		return "x: " + x() + ", y: " + y();
	}
}
