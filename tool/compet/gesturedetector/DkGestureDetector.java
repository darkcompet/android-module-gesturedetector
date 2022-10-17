/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */

package tool.compet.gesturedetector;

import static java.lang.Math.PI;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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
public class DkGestureDetector {
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
	public interface Listener extends
		TheTapDetector.Listener,
		TheDoubleDetector.Listener,
		TheDragDetector.Listener,
		TheFlyDetector.Listener,
		TheScaleDetector.Listener,
		TheRotateDetector.Listener {}

	// Callback when some gesture was detected
	@NonNull private Listener listener;

	// All supported gestures
	public static final int FLAG_TAP = 1 << 1; // disabled by default
	public static final int FLAG_DOUBLE_TAP = 1 << 2;
	public static final int FLAG_DRAG = 1 << 3;
	public static final int FLAG_FLY = 1 << 4; // disabled by default
	public static final int FLAG_SCALE = 1 << 5;
	public static final int FLAG_ROTATE = 1 << 6;
	private static final int ALL_GESTURES_ON_FLAG = (1 << 7) - 1; // 2^7 - 1 = 0111111B

	// This is gestures which be enabled when setting.
	// ONLY enabled gestures can be considered as target of detection.
	private int enabledGesturesFlag = FLAG_DOUBLE_TAP | FLAG_DRAG | FLAG_SCALE | FLAG_ROTATE;

	// This is gestures which detection will be skipped while event-streaming.
	// Note that: all gestures are turned ON after event-stream completed.
	private int skipGesturesFlag = ALL_GESTURES_ON_FLAG ^ enabledGesturesFlag;

	// Event info to share between detectors
	private final MyEventInfo eventInfo = new MyEventInfo();

	// Now, we have 6 detectors.
	private final List<MyDetector> detectors = new ArrayList<>(6);

	public DkGestureDetector(Context context, @NonNull Listener listener) {
		final ViewConfiguration viewConfig = ViewConfiguration.get(context);
		final int touchSlop = viewConfig.getScaledTouchSlop();
		final int doubleTapSlop = viewConfig.getScaledDoubleTapSlop();

		this.listener = listener;

		// Gesture info (contains detect-info about process and result)
		final TheTapDetector tapInfo = new TheTapDetector(listener);
		tapInfo.gestureFlag = FLAG_TAP;
		tapInfo.tapSlopSquare = touchSlop * touchSlop;

		final TheDoubleDetector doubleTapInfo = new TheDoubleDetector(listener);
		doubleTapInfo.gestureFlag = FLAG_DOUBLE_TAP;
		doubleTapInfo.tapTimeOut = ViewConfiguration.getDoubleTapTimeout();
		doubleTapInfo.tapSlopSquare = doubleTapSlop * doubleTapSlop;

		final TheDragDetector dragInfo = new TheDragDetector(listener);
		dragInfo.gestureFlag = FLAG_DRAG;
		dragInfo.dragSlopSquare = touchSlop * touchSlop;

		final TheFlyDetector flyInfo = new TheFlyDetector(listener);
		flyInfo.gestureFlag = FLAG_FLY;
		flyInfo.minVelocity = viewConfig.getScaledMinimumFlingVelocity(); // px/sec
		flyInfo.maxVelocity = viewConfig.getScaledMaximumFlingVelocity(); // px/sec

		final TheScaleDetector scaleInfo = new TheScaleDetector(listener);
		scaleInfo.gestureFlag = FLAG_SCALE;
		scaleInfo.spanSlop = touchSlop * 2;

		final TheRotateDetector rotateInfo = new TheRotateDetector(listener);
		rotateInfo.gestureFlag = FLAG_ROTATE;
		rotateInfo.rotationSlop = (float) (PI / 18);

		// For now, maybe we don't need priority for detectors.
		// Just for draft.
		tapInfo.priority = 1;
		doubleTapInfo.priority = 2;
		dragInfo.priority = 3;
		flyInfo.priority = 4;
		scaleInfo.priority = 5;
		rotateInfo.priority = 6;

		List<MyDetector> detectors = this.detectors;
		detectors.add(tapInfo);
		detectors.add(doubleTapInfo);
		detectors.add(rotateInfo);
		detectors.add(scaleInfo);
		detectors.add(dragInfo);
		detectors.add(flyInfo);
	}

	public void setListener(@NonNull Listener listener) {
		this.listener = listener;
	}

	/**
	 * Call this when you receive motion-event, let us know to detect the user's gesture.
	 */
	public boolean onTouchEvent(MotionEvent event) {
		final MyEventInfo eventInfo = this.eventInfo;
		final int action = event.getActionMasked();
		eventInfo.actionDown = action == MotionEvent.ACTION_DOWN;
		eventInfo.actionMove = action == MotionEvent.ACTION_MOVE;
		eventInfo.actionUp = action == MotionEvent.ACTION_UP;
		eventInfo.actionCancel = action == MotionEvent.ACTION_CANCEL;
		eventInfo.actionPointerDown = action == MotionEvent.ACTION_POINTER_DOWN;
		eventInfo.actionPointerUp = action == MotionEvent.ACTION_POINTER_UP;

		// Check with this flag to re-init detection info.
		// Note: while streaming, config-changed maybe cause big-gap between current event and next event.
		// For eg,. move (frame: 1, pointers: 3) -> pointer-up (frame: 2, pointers: 3) -> move (frame: 3, pointers: 2).
		// To remove gap between frame 1 and frame 3, we need ignore event at pointer-up,
		// and init/reset detection info for new detection at next action move.
		eventInfo.configChanged = eventInfo.actionDown || eventInfo.actionPointerDown || eventInfo.actionPointerUp;

		// When stream has ended, we should reset detection info
		eventInfo.streamCompleted = eventInfo.actionUp || eventInfo.actionCancel;

		// For action down, we need set as TRUE to indicate we handle next event.
		// If not set to TRUE at action down, we will get unintentional behavior later
		boolean handled = eventInfo.actionDown || eventInfo.actionPointerDown;

		// It is important to note that: when some pointer has up, to avoid a gap mentioned above,
		// we should skip calculation for index of pointer-up.
		eventInfo.pointerCount = event.getPointerCount();
		eventInfo.skipPointerIndex = eventInfo.actionPointerUp ? event.getActionIndex() : -1;
		eventInfo.divPointerCount = eventInfo.actionPointerUp ? eventInfo.pointerCount - 1 : eventInfo.pointerCount;

		// Calculate Pivot (center, focal) point of pointers
		float sumX = 0f;
		float sumY = 0f;

		eventInfo.pxList.ensureCapacity(eventInfo.pointerCount);
		eventInfo.pyList.ensureCapacity(eventInfo.pointerCount);

		for (int pointerIndex = 0; pointerIndex < eventInfo.pointerCount; ++pointerIndex) {
			if (pointerIndex != eventInfo.skipPointerIndex) {
				float pointerX = event.getX(pointerIndex);
				float pointerY = event.getY(pointerIndex);
				eventInfo.pxList.set(pointerIndex, pointerX);
				eventInfo.pyList.set(pointerIndex, pointerY);
				sumX += pointerX;
				sumY += pointerY;
			}
		}
		eventInfo.pivotX = sumX / eventInfo.divPointerCount;
		eventInfo.pivotY = sumY / eventInfo.divPointerCount;

		// Check and Run all detectors
		for (MyDetector detector : this.detectors) {
			if (shouldDetectGesture(detector.gestureFlag)) {
				handled |= detector.onTouchEvent(event, eventInfo);
			}
		}

		if (eventInfo.streamCompleted) {
			turnAllGesturesOn();
		}

		return handled;
	}

	private boolean shouldDetectGesture(int gestureFlag) {
		return (enabledGesturesFlag & gestureFlag) != 0 && (skipGesturesFlag & gestureFlag) == 0;
	}

	/**
	 * Enable all gestures.
	 */
	public DkGestureDetector enableAllGestures() {
		this.enabledGesturesFlag = ALL_GESTURES_ON_FLAG;
		return this;
	}

	/**
	 * Disable all gestures.
	 */
	public DkGestureDetector disableAllGestures() {
		this.enabledGesturesFlag = 0;
		return this;
	}

	/**
	 * Enable given gestures via bit-masked flags.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	public void enableGestures(int... gestureFlags) {
		if (gestureFlags != null) {
			int enabledGesturesFlag = this.enabledGesturesFlag;
			for (int flag : gestureFlags) {
				enabledGesturesFlag |= flag;
			}
			this.enabledGesturesFlag = enabledGesturesFlag;
		}
	}

	/**
	 * Disable given gestures (other gestures will not be afftected).
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	public void disableGestures(int... gestureFlags) {
		if (gestureFlags != null) {
			int enabledGesturesFlag = this.enabledGesturesFlag;
			for (int flag : gestureFlags) {
				// Set bit at the `gestureFlag` to 0, other bits will be not changed
				enabledGesturesFlag &= (~flag);
			}
			this.enabledGesturesFlag = enabledGesturesFlag;
		}
	}

	/**
	 * Check whether given gesture is enabled or not.
	 *
	 * @param gestureFlag For eg,. FLAG_DOUBLE_TAP
	 */
	public boolean isGestureEnabled(int gestureFlag) {
		return (enabledGesturesFlag & gestureFlag) != 0;
	}

	/**
	 * Turn ON detection for all gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 */
	public DkGestureDetector turnAllGesturesOn() {
		this.skipGesturesFlag = 0;
		return this;
	}

	/**
	 * Turn OFF detection for all gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 */
	public DkGestureDetector turnAllGesturesOff() {
		this.skipGesturesFlag = ALL_GESTURES_ON_FLAG;
		return this;
	}

	/**
	 * Turn ON detection for given gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	public void turnGesturesOn(int... gestureFlags) {
		if (gestureFlags != null) {
			int skipGesturesFlag = this.skipGesturesFlag;
			for (int flag : gestureFlags) {
				skipGesturesFlag &= (~flag); // set as 0 to keep the flag
			}
			this.skipGesturesFlag = skipGesturesFlag;
		}
	}

	/**
	 * Turn OFF detection for given gestures `while streaming`.
	 *
	 * Note: after stream completed, all gestures will NOT be skipped unless caller skip them again.
	 *
	 * @param gestureFlags For eg,. FLAG_DOUBLE_TAP, FLAG_DRAG, FLAG_SCALE, FLAG_ROTATE
	 */
	public void turnGesturesOff(int... gestureFlags) {
		if (gestureFlags != null) {
			int skipGesturesFlag = this.skipGesturesFlag;
			for (int flag : gestureFlags) {
				skipGesturesFlag |= flag; // set as 1 to skip the flag
			}
			this.skipGesturesFlag = skipGesturesFlag;
		}
	}

	/**
	 * Check whether detection of given gesture-flag is skipped or not `while streaming`.
	 *
	 * @param gestureFlag For eg,. FLAG_DOUBLE_TAP
	 */
	public boolean isGestureSkipped(int gestureFlag) {
		return (skipGesturesFlag & gestureFlag) != 0;
	}

	private String actionName(int action) {
		if (action == MotionEvent.ACTION_DOWN)
			return "down";
		if (action == MotionEvent.ACTION_UP)
			return "up";
		if (action == MotionEvent.ACTION_MOVE)
			return "move";
		if (action == MotionEvent.ACTION_CANCEL)
			return "cancel";
		if (action == MotionEvent.ACTION_OUTSIDE)
			return "outside";
		if (action == MotionEvent.ACTION_POINTER_DOWN)
			return "pointer-down";
		if (action == MotionEvent.ACTION_POINTER_UP)
			return "pointer-up";

		throw new RuntimeException("Unexpected action");
	}
}
