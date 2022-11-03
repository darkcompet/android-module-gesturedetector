package tool.compet.gesturedetector

import android.view.MotionEvent

abstract class MyDetector {
	var gestureFlag = 0
	var priority = 0

	abstract fun onTouchEvent(event: MotionEvent, eventInfo: MyEventInfo): Boolean
}