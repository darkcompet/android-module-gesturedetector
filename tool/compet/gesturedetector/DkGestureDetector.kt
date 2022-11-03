/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.gesturedetector

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * It provides various gesture detections on pointers (single or multiple).
 * Supported getures:
 * - Tap (disabled by default): support multiple pointers.
 * - Double-tap: support multiple pointers.
 * - Fly (disabled by default): apply for primary pointer (first poitner).
 * - Drag: support multiple pointers.
 * - Scale: multiple pointers (normally require at least 2 pointers).
 * - Rotate: multiple pointers (normally require at least 2 pointers).
 */
class DkGestureDetector(context: Context) {
	companion object {
		// All supported gestures
		const val FLAG_TAP = 1 shl 1 // disabled by default
		const val FLAG_DOUBLE_TAP = 1 shl 2
		const val FLAG_DRAG = 1 shl 3
		const val FLAG_FLY = 1 shl 4 // disabled by default
		const val FLAG_SCALE = 1 shl 5
		const val FLAG_ROTATE = 1 shl 6
		private const val ALL_GESTURES_ON_FLAG = (1 shl 7) - 1 // 2^7 - 1 = 0111111B
	}

	/**
	 * For all listeners.
	 *
	 * - Return TRUE: indicates that listener has handled this detection,
	 * so last detection-result will be updated to current detection-result.
	 * - Return FALSE: last detection-result will be remained, and be used
	 * for next detection.
	 *
	 * For eg,. if listener does not accept (handle) detection since detected result
	 * is smalling to take into account. Then return FALSE to wait new result to continue.
	 */
	interface Listener : TheTapDetector.Listener,
		TheDoubleDetector.Listener,
		TheDragDetector.Listener,
		TheFlyDetector.Listener,
		TheScaleDetector.Listener,
		TheRotationDetector.Listener

	// Callback when some gesture was detected
	//private var listener: Listener? = null

	// This is gestures which be enabled when setting.
	// ONLY enabled gestures can be considered as target of detection.
	private var enabledGesturesFlag = FLAG_DOUBLE_TAP or FLAG_DRAG or FLAG_SCALE or FLAG_ROTATE

	// This is gestures which detection will be skipped while event-streaming.
	// Note that: all gestures are turned ON after event-stream completed.
	private var skipGesturesFlag = ALL_GESTURES_ON_FLAG xor enabledGesturesFlag

	// Event info to share between detectors
	private val eventInfo = MyEventInfo()

	// Current registered detectors
	private val detectors: MutableList<MyDetector> = ArrayList()

	init {
	}

	fun initWithDefaultDetectors(context: Context, listener: Listener) {
		val viewConfig = ViewConfiguration.get(context)
		val touchSlop = viewConfig.scaledTouchSlop
		val doubleTapSlop = viewConfig.scaledDoubleTapSlop

		// Gesture info (contains detect-info about process and result)
		val tapInfo = TheTapDetector(listener)
		tapInfo.gestureFlag = FLAG_TAP
		tapInfo.tapSlopSquare = (touchSlop * touchSlop).toFloat()

		val doubleTapInfo = TheDoubleDetector(listener)
		doubleTapInfo.gestureFlag = FLAG_DOUBLE_TAP
		doubleTapInfo.tapTimeOut = ViewConfiguration.getDoubleTapTimeout()
		doubleTapInfo.tapSlopSquare = doubleTapSlop * doubleTapSlop

		val dragInfo = TheDragDetector(listener)
		dragInfo.gestureFlag = FLAG_DRAG
		dragInfo.dragSlopSquare = (touchSlop * touchSlop).toFloat()

		val flyInfo = TheFlyDetector(listener)
		flyInfo.gestureFlag = FLAG_FLY
		flyInfo.minVelocity = viewConfig.scaledMinimumFlingVelocity // px/sec
		flyInfo.maxVelocity = viewConfig.scaledMaximumFlingVelocity // px/sec

		val scaleInfo = TheScaleDetector(listener)
		scaleInfo.gestureFlag = FLAG_SCALE
		scaleInfo.spanSlop = (touchSlop * 2).toFloat()

		val rotateInfo = TheRotationDetector(listener)
		rotateInfo.gestureFlag = FLAG_ROTATE
		rotateInfo.rotationSlop = (Math.PI / 18).toFloat()

		// For now, maybe we don't need priority for detectors.
		// Just for draft.
		tapInfo.priority = 1
		doubleTapInfo.priority = 2
		dragInfo.priority = 3
		flyInfo.priority = 4
		scaleInfo.priority = 5
		rotateInfo.priority = 6

		val detectors = detectors
		detectors.add(tapInfo)
		detectors.add(doubleTapInfo)
		detectors.add(rotateInfo)
		detectors.add(scaleInfo)
		detectors.add(dragInfo)
		detectors.add(flyInfo)
	}

	fun registerDetector(detector: MyDetector) {
		if (!this.detectors.contains(detector)) {
			this.detectors.add(detector)
		}
	}

	fun unregisterDetector(detector: MyDetector) {
		this.detectors.remove(detector)
	}

	/**
	 * Call this when you receive motion-event, let us know to detect the user's gesture.
	 */
	fun onTouchEvent(event: MotionEvent): Boolean {
		val eventInfo = eventInfo
		val action = event.actionMasked
		eventInfo.actionDown = action == MotionEvent.ACTION_DOWN
		eventInfo.actionMove = action == MotionEvent.ACTION_MOVE
		eventInfo.actionUp = action == MotionEvent.ACTION_UP
		eventInfo.actionCancel = action == MotionEvent.ACTION_CANCEL
		eventInfo.actionPointerDown = action == MotionEvent.ACTION_POINTER_DOWN
		eventInfo.actionPointerUp = action == MotionEvent.ACTION_POINTER_UP

		// Check with this flag to re-init detection info.
		// Note: while streaming, config-changed maybe cause big-gap between current event and next event.
		// For eg,. move (frame: 1, pointers: 3) -> pointer-up (frame: 2, pointers: 3) -> move (frame: 3, pointers: 2).
		// To remove gap between frame 1 and frame 3, we need ignore event at pointer-up,
		// and init/reset detection info for new detection at next action move.
		eventInfo.configChanged = eventInfo.actionDown || eventInfo.actionPointerDown || eventInfo.actionPointerUp

		// When stream has ended, we should reset detection info
		eventInfo.streamCompleted = eventInfo.actionUp || eventInfo.actionCancel

		// For action down, we need set as TRUE to indicate we handle next event.
		// If not set to TRUE at action down, we will get unintentional behavior later
		var handled = eventInfo.actionDown || eventInfo.actionPointerDown

		// It is important to note that: when some pointer has up, to avoid a gap mentioned above,
		// we should skip calculation for index of pointer-up.
		eventInfo.pointerCount = event.pointerCount
		eventInfo.skipPointerIndex = if (eventInfo.actionPointerUp) event.actionIndex else -1
		eventInfo.divPointerCount =
			if (eventInfo.actionPointerUp) (eventInfo.pointerCount - 1).toFloat() else eventInfo.pointerCount.toFloat()

		// Calculate Pivot (center, focal) point of pointers
		var sumX = 0f
		var sumY = 0f
		eventInfo.pxList.ensureCapacity(eventInfo.pointerCount)
		eventInfo.pyList.ensureCapacity(eventInfo.pointerCount)
		for (pointerIndex in 0 until eventInfo.pointerCount) {
			if (pointerIndex != eventInfo.skipPointerIndex) {
				val pointerX = event.getX(pointerIndex)
				val pointerY = event.getY(pointerIndex)
				eventInfo.pxList[pointerIndex] = pointerX
				eventInfo.pyList[pointerIndex] = pointerY
				sumX += pointerX
				sumY += pointerY
			}
		}
		eventInfo.pivotX = sumX / eventInfo.divPointerCount
		eventInfo.pivotY = sumY / eventInfo.divPointerCount

		// Check and Run all detectors
		for (detector in detectors) {
			if (shouldDetectGesture(detector.gestureFlag)) {
				handled = handled or detector.onTouchEvent(event, eventInfo)
			}
		}

		if (eventInfo.streamCompleted) {
			turnAllGesturesOn()
		}

		return handled
	}

	private fun shouldDetectGesture(gestureFlag: Int): Boolean {
		return enabledGesturesFlag and gestureFlag != 0 && skipGesturesFlag and gestureFlag == 0
	}

	/**
	 * Enable all gestures.
	 */
	fun enableAllGestures(): DkGestureDetector {
		enabledGesturesFlag = ALL_GESTURES_ON_FLAG
		return this
	}

	/**
	 * Disable all gestures.
	 */
	fun disableAllGestures(): DkGestureDetector {
		enabledGesturesFlag = 0
		return this
	}

	/**
	 * Enable given gestures via bit-masked flags.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	fun enableGestures(vararg gestureFlags: Int) {
		var enabledGesturesFlag = enabledGesturesFlag
		for (flag in gestureFlags) {
			enabledGesturesFlag = enabledGesturesFlag or flag
		}
		this.enabledGesturesFlag = enabledGesturesFlag
	}

	/**
	 * Disable given gestures (other gestures will not be afftected).
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	fun disableGestures(vararg gestureFlags: Int) {
		var enabledGesturesFlag = enabledGesturesFlag
		for (flag in gestureFlags) {
			// Set bit at the `gestureFlag` to 0, other bits will be not changed
			enabledGesturesFlag = enabledGesturesFlag and flag.inv()
		}
		this.enabledGesturesFlag = enabledGesturesFlag
	}

	/**
	 * Check whether given gesture is enabled or not.
	 *
	 * @param gestureFlag For eg,. FLAG_DOUBLE_TAP
	 */
	fun isGestureEnabled(gestureFlag: Int): Boolean {
		return enabledGesturesFlag and gestureFlag != 0
	}

	/**
	 * Turn ON detection for all gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 */
	fun turnAllGesturesOn(): DkGestureDetector {
		skipGesturesFlag = 0
		return this
	}

	/**
	 * Turn OFF detection for all gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 */
	fun turnAllGesturesOff(): DkGestureDetector {
		skipGesturesFlag = ALL_GESTURES_ON_FLAG
		return this
	}

	/**
	 * Turn ON detection for given gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	fun turnGesturesOn(vararg gestureFlags: Int) {
		var skipGesturesFlag = skipGesturesFlag
		for (flag in gestureFlags) {
			skipGesturesFlag = skipGesturesFlag and flag.inv() // set as 0 to keep the flag
		}
		this.skipGesturesFlag = skipGesturesFlag
	}

	/**
	 * Turn OFF detection for given gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	fun turnGesturesOff(vararg gestureFlags: Int) {
		var skipGesturesFlag = skipGesturesFlag
		for (flag in gestureFlags) {
			skipGesturesFlag = skipGesturesFlag or flag // set as 1 to skip the flag
		}
		this.skipGesturesFlag = skipGesturesFlag
	}

	/**
	 * Check whether detection of given gesture-flag is skipped or not `while streaming`.
	 *
	 * @param gestureFlag For eg,. FLAG_DOUBLE_TAP
	 */
	fun isGestureSkipped(gestureFlag: Int): Boolean {
		return skipGesturesFlag and gestureFlag != 0
	}

	private fun actionName(action: Int): String {
		if (action == MotionEvent.ACTION_DOWN) return "down"
		if (action == MotionEvent.ACTION_UP) return "up"
		if (action == MotionEvent.ACTION_MOVE) return "move"
		if (action == MotionEvent.ACTION_CANCEL) return "cancel"
		if (action == MotionEvent.ACTION_OUTSIDE) return "outside"
		if (action == MotionEvent.ACTION_POINTER_DOWN) return "pointer-down"
		if (action == MotionEvent.ACTION_POINTER_UP) return "pointer-up"
		throw RuntimeException("Unexpected action")
	}
}