package tool.compet.gesturedetector

import tool.compet.core.DkFloatArrayList

class MyEventInfo {
	var actionPointerUp = false
	var actionPointerDown = false
	var actionCancel = false
	var actionUp = false
	var actionMove = false
	var actionDown = false

	// Stream status
	var configChanged = false
	var streamCompleted = false

	// Pointer
	// Raw pointer count
	var pointerCount = 0
	var skipPointerIndex = 0

	// For divide (pointer count after skip up-pointer)
	var divPointerCount = 0f

	// Coordinate of pointers
	val pxList = DkFloatArrayList()
	val pyList = DkFloatArrayList()
	var pivotX = 0f
	var pivotY = 0f
}