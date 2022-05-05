package tool.compet.gesturedetector;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class TheDragDetector extends MyDetector {
	public interface Listener {
		boolean onDrag(TheDragDetector detector);
	}

	TheDragDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;
	
	boolean inProgress;

	float dragSlopSquare;

	float pivotX;
	float pivotY;
	float lastPivotX;
	float lastPivotY;

	@Override
	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		if (eventInfo.configChanged) {
			this.inProgress = false;
			this.lastPivotX = this.pivotX = eventInfo.pivotX;
			this.lastPivotY = this.pivotY = eventInfo.pivotY;
		}
		// Detect at action-move !
		// At this frame, we perform detection
		else if (eventInfo.actionMove) {
			this.pivotX = eventInfo.pivotX;
			this.pivotY = eventInfo.pivotY;

			if (! this.inProgress) {
				final float dx = eventInfo.pivotX - this.lastPivotX;
				final float dy = eventInfo.pivotY - this.lastPivotY;

				if (dx * dx + dy * dy >= this.dragSlopSquare) {
					this.inProgress = true;
					this.accept();
				}
			}
			else {
				// DkLogcats.debug(this, "----> DRAG: %s", this.toString());

				if (listener.onDrag(this)) {
					handled = true;
					this.accept();
				}
			}
		}
		else if (eventInfo.streamCompleted) {
			this.inProgress = false;
		}
		return handled;
	}

	void accept() {
		lastPivotX = pivotX;
		lastPivotY = pivotY;
	}

	public float dx() {
		return this.pivotX - this.lastPivotX;
	}

	public float dy() {
		return this.pivotY - this.lastPivotY;
	}

	public float distance() {
		return (float) Math.hypot(dx(), dy());
	}

	@Override
	public String toString() {
		return "dx: " + dx() + ", dy: " + dy();
	}
}
