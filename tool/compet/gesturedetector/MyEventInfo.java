package tool.compet.gesturedetector;

import tool.compet.core.DkFloatArrayList;

class MyEventInfo {
	boolean actionPointerUp;
	boolean actionPointerDown;
	boolean actionCancel;
	boolean actionUp;
	boolean actionMove;
	boolean actionDown;
	// Stream status
	boolean configChanged;
	boolean streamCompleted;

	// Pointer
	int pointerCount; // Raw pointer count
	int skipPointerIndex;
	float divPointerCount; // For divide (pointer count after skip up-pointer)

	// Coordinate of pointers
	final DkFloatArrayList pxList = new DkFloatArrayList();
	final DkFloatArrayList pyList = new DkFloatArrayList();

	float pivotX;
	float pivotY;
}
