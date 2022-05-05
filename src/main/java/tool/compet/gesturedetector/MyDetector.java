package tool.compet.gesturedetector;

import android.view.MotionEvent;

abstract class MyDetector {
	int gestureFlag;
	int priority;

	abstract boolean onTouchEvent(MotionEvent event, MyEventInfo eventInfo);
}
