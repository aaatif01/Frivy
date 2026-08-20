package com.frivy.assistant.accessibility
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
class FrivyAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    fun currentPackage(): String? = rootInActiveWindow?.packageName?.toString()
    fun readScreen(): String = rootInActiveWindow?.let { collect(it).trim() } ?: ""
    fun click(text: String): Boolean = find(rootInActiveWindow, text)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    fun scroll(forward: Boolean) = rootInActiveWindow?.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean { val p = Path().apply { moveTo(x1,y1); lineTo(x2,y2) }; return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,500)).build(),null,null) }
    private fun collect(node: AccessibilityNodeInfo): String = buildString { node.text?.let { append(it).append(' ') }; for(i in 0 until node.childCount) node.getChild(i)?.let { append(collect(it)) } }
    private fun find(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? { if (node == null) return null; if (node.text?.toString()?.contains(text,true)==true) return node; for(i in 0 until node.childCount) find(node.getChild(i),text)?.let{return it}; return null }
}