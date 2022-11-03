package tool.compet.gesturedetector

import android.view.MotionEvent
import tool.compet.core.DkFloatArrayList
import tool.compet.core.DkIntFloatArrayMap
import kotlin.math.abs
import kotlin.math.atan2

// ROTATE detection
// By default, we require at least 2 pointers perform rotate
class TheRotationDetector internal constructor(var listener: Listener) : MyDetector() {
	interface Listener {
		fun onRotate(detector: TheRotationDetector): Boolean
	}

	companion object {
		private const val PIPI = Math.PI * 2
	}

	var inProgress = false

	// Can start rotate after over this slop
	var rotationSlop = 0f
	var rotation = 0f

	// The focal point which be rotated around
	var lastPivotX = 0f
	var lastPivotY = 0f
	var pivotX = 0f
	var pivotY = 0f
	var pointer2bearing = DkIntFloatArrayMap()

	override fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean {
		var handled = false

		if (eventInfo.pointerCount >= 2) {
			// Reset to start new detection when config changed
			if (eventInfo.configChanged) {
				// Update bearing of pointers
				for (pointerIndex in 0 until eventInfo.pointerCount) {
					if (pointerIndex != eventInfo.skipPointerIndex) {
						val pointerId = event.getPointerId(pointerIndex)

						// Reverse dy to convert to Oxy coordinate-system
						val pointerBearing = atan2(
							(-eventInfo.pyList[pointerIndex] + eventInfo.pivotY).toDouble(),
							(eventInfo.pxList[pointerIndex] - eventInfo.pivotX).toDouble()
						).toFloat()

						pointer2bearing.put(pointerId, pointerBearing)
					}
				}

				inProgress = false
				pivotX = eventInfo.pivotX
				lastPivotX = pivotX
				pivotY = eventInfo.pivotY
				lastPivotY = pivotY
			}
			else if (eventInfo.actionMove) {
				// Calculate and Update current detection info
				var sumBearing = 0f

				for (pointerIndex in 0 until eventInfo.pointerCount) {
					// Always true: pointerIndex != skipPointerIndex
					val pointerId = event.getPointerId(pointerIndex)
					val lastPointerBearing = pointer2bearing[pointerId, Int.MAX_VALUE.toFloat()]

					if (lastPointerBearing.toInt() != Int.MAX_VALUE) {
						val pointerBearing = atan2(
							(-eventInfo.pyList[pointerIndex] + eventInfo.pivotY).toDouble(),
							(eventInfo.pxList[pointerIndex] - eventInfo.pivotX).toDouble()
						).toFloat()

						sumBearing += calcRotation(lastPointerBearing, pointerBearing)
					}
				}

				pivotX = eventInfo.pivotX
				pivotY = eventInfo.pivotY
				rotation = sumBearing / eventInfo.divPointerCount

				if (!inProgress && abs(rotation) >= rotationSlop) {
					inProgress = true
					accept(event, eventInfo.pointerCount, eventInfo.pxList, eventInfo.pyList)
				}
				else if (inProgress) {
					// DkLogcats.debug(this, "----> ROTATE: %s", this.toString());
					if (listener.onRotate(this)) {
						handled = true
						accept(event, eventInfo.pointerCount, eventInfo.pxList, eventInfo.pyList)
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
	 * Call this when detection has finished.
	 */
	private fun accept(event: MotionEvent, pointerCount: Int, pxList: DkFloatArrayList, pyList: DkFloatArrayList) {
		lastPivotX = pivotX
		lastPivotY = pivotY

		for (pointerIndex in 0 until pointerCount) {
			pointer2bearing.put(
				event.getPointerId(pointerIndex),
				atan2((-pyList[pointerIndex] + pivotY).toDouble(), (pxList[pointerIndex] - pivotX).toDouble()).toFloat()
			)
		}
	}

	/**
	 * @return Rotated angle in radian.
	 */
	fun rotation(): Float {
		return rotation
	}

	/**
	 * @return Pivot X coordinate (other name: focus, center, gravity...).
	 */
	fun pivotX(): Float {
		return (lastPivotX + pivotX) / 2f
	}

	/**
	 * @return Pivot Y coordinate (other name: focus, center, gravity...).
	 */
	fun pivotY(): Float {
		return (lastPivotY + pivotY) / 2f
	}

	override fun toString(): String {
		return "rotation: " + rotation() + ", pivotX: " + pivotX() + ", pivotY: " + pivotY()
	}

	/**
	 * The problem when we calculate rotation angle from 2 bearings (last_bearing, current_bearing)
	 * is when rotation go around angle PI, sign of angle will be changed.
	 * For eg,. 178° -> -179°, or -178° -> 179°.
	 *
	 * This function does not just compute rotation (`current_bearing - last_bearing`),
	 * it also check/fix rotation by compare with PI or -PI.
	 */
	private fun calcRotation(lastBearing: Float, bearing: Float): Float {
		val rotation = bearing - lastBearing
		if (rotation >= Math.PI) {
			return (rotation - PIPI).toFloat()
		}
		return if (rotation <= -Math.PI) {
			(rotation + PIPI).toFloat()
		}
		else rotation
	}
}