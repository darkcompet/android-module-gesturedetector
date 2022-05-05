package tool.compet.gesturedetector;

import static java.lang.Math.PI;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

import tool.compet.core.DkFloatArrayList;
import tool.compet.core.DkIntFloatArrayMap;

// ROTATE detection
// By default, we require at least 2 pointers perform rotate
public class TheRotateDetector extends MyDetector {
	public interface Listener {
		boolean onRotate(TheRotateDetector detector);
	}

	TheRotateDetector(@NonNull Listener listener) {
		this.listener = listener;
	}

	@NonNull Listener listener;

	private static final double PIPI = PI * 2;

	boolean inProgress;

	// Can start rotate after over this slop
	float rotationSlop;

	float rotation;

	// The focal point which be rotated around
	float lastPivotX;
	float lastPivotY;
	float pivotX;
	float pivotY;

	DkIntFloatArrayMap pointer2bearing = new DkIntFloatArrayMap();

	@Override
	boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo) {
		boolean handled = false;

		if (eventInfo.pointerCount >= 2) {
			// Reset to start new detection when config changed
			if (eventInfo.configChanged) {
				// Update bearing for pointers
				for (int pointerIndex = 0; pointerIndex < eventInfo.pointerCount; ++pointerIndex) {
					if (pointerIndex != eventInfo.skipPointerIndex) {
						int pointerId = event.getPointerId(pointerIndex);
						// Reverse dy to convert to Oxy coordinate-system
						float pointerBearing = (float) Math.atan2(-eventInfo.pyList.get(pointerIndex) + eventInfo.pivotY, eventInfo.pxList.get(pointerIndex) - eventInfo.pivotX);

						this.pointer2bearing.put(pointerId, pointerBearing);
					}
				}

				this.inProgress = false;
				this.lastPivotX = this.pivotX = eventInfo.pivotX;
				this.lastPivotY = this.pivotY = eventInfo.pivotY;
			}
			// Detect at action-move !
			else if (eventInfo.actionMove) {
				// Calculate and Update current detection info
				float sumBearing = 0;
				for (int pointerIndex = 0; pointerIndex < eventInfo.pointerCount; ++pointerIndex) {
					// Always true: pointerIndex != skipPointerIndex
					int pointerId = event.getPointerId(pointerIndex);
					float lastPointerBearing = this.pointer2bearing.get(pointerId, Integer.MAX_VALUE);

					if (lastPointerBearing != Integer.MAX_VALUE) {
						float pointerBearing = (float) Math.atan2(-eventInfo.pyList.get(pointerIndex) + eventInfo.pivotY, eventInfo.pxList.get(pointerIndex) - eventInfo.pivotX);
						sumBearing += calcRotation(lastPointerBearing, pointerBearing);
					}
				}
				this.pivotX = eventInfo.pivotX;
				this.pivotY = eventInfo.pivotY;
				this.rotation = sumBearing / eventInfo.divPointerCount;

				if (! this.inProgress && Math.abs(this.rotation) >= this.rotationSlop) {
					this.inProgress = true;
					this.accept(event, eventInfo.pointerCount, eventInfo.pxList, eventInfo.pyList);
				}
				else if (this.inProgress) {
					// DkLogcats.debug(this, "----> ROTATE: %s", this.toString());

					if (listener.onRotate(this)) {
						handled = true;
						this.accept(event, eventInfo.pointerCount, eventInfo.pxList, eventInfo.pyList);
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
	 * Call this when detection has finished.
	 */
	void accept(MotionEvent event, int pointerCount, DkFloatArrayList pxList, DkFloatArrayList pyList) {
		lastPivotX = pivotX;
		lastPivotY = pivotY;

		for (int pointerIndex = 0; pointerIndex < pointerCount; ++pointerIndex) {
			pointer2bearing.put(event.getPointerId(pointerIndex), (float) Math.atan2(-pyList.get(pointerIndex) + pivotY, pxList.get(pointerIndex) - pivotX));
		}
	}

	/**
	 * @return Rotated angle in radian.
	 */
	public float rotation() {
		return rotation;
	}

	/**
	 * @return Pivot X coordinate (other name: focus, center, gravity...).
	 */
	public float pivotX() {
		return (lastPivotX + pivotX) / 2f;
	}

	/**
	 * @return Pivot Y coordinate (other name: focus, center, gravity...).
	 */
	public float pivotY() {
		return (lastPivotY + pivotY) / 2f;
	}

	@Override
	public String toString() {
		return "rotation: " + rotation() + ", pivotX: " + pivotX() + ", pivotY: " + pivotY();
	}

	/**
	 * The problem when we calculate rotation angle from 2 bearings (last_bearing, current_bearing)
	 * is when rotation go around angle PI, sign of angle will be changed.
	 * For eg,. 178° -> -179°, or -178° -> 179°.
	 *
	 * This function does not just compute rotation (`current_bearing - last_bearing`),
	 * it also check/fix rotation by compare with PI or -PI.
	 */
	private float calcRotation(float lastBearing, float bearing) {
		float rotation = bearing - lastBearing;
		if (rotation >= PI) {
			return (float) (rotation - PIPI);
		}
		if (rotation <= -PI) {
			return (float) (rotation + PIPI);
		}
		return rotation;
	}

	private static double make2PiRange(double angle) {
		if (angle >= PIPI || angle <= -PIPI) {
			angle %= PIPI;
		}
		if (angle < 0) {
			angle += PIPI;
		}
		return angle;
	}
}
