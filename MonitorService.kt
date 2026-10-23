package com.example.attentionmonitor

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.app.KeyguardManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MonitorService : AccessibilityService() {
    private lateinit var store: Store
    private var active = "NONE"
    private var unlocked = false
    private var lastScrollTs = 0L
    private var lastScrollApp = ""
    private var chromeInstagram = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> setUnlocked(false)
                Intent.ACTION_USER_PRESENT -> setUnlocked(true)
            }
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        store=Store(this)
        val f=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_USER_PRESENT) }
        registerReceiver(screenReceiver,f)
        val keyguard=getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val power=getSystemService(POWER_SERVICE) as PowerManager
        unlocked=power.isInteractive && !keyguard.isKeyguardLocked
        if(unlocked) store.add("UNLOCK","NONE")
    }
    private fun setUnlocked(value:Boolean) {
        if(unlocked==value) return
        unlocked=value
        if(value) { store.add("UNLOCK","NONE"); updateApp("NONE") }
        else { updateApp("NONE"); store.add("LOCK","NONE"); chromeInstagram=false }
    }
    private fun updateApp(target:String) {
        if(target==active) return
        val prev=active
        active=target
        lastScrollTs=0
        lastScrollApp=""
        store.add("SWITCH",target,"from=$prev")
    }
    private fun classify(packageName:String):String = when(packageName) {
        "com.facebook.katana" -> "Facebook"
        "com.google.android.youtube" -> "YouTube"
        "com.android.chrome" -> if(chromeInstagram) "Instagram Web" else "NONE"
        else -> "NONE"
    }
    // Only inspect Chrome's address field. Never store its content or the page contents.
    private fun instagramUrlVisible():Boolean? {
        val root=rootInActiveWindow ?: return null
        if(root.packageName?.toString()!="com.android.chrome") return null
        val nodes=root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
        val text=nodes.firstOrNull()?.text?.toString()?.lowercase()?.trim() ?: return null
        if(text.isEmpty()) return null
        // Chrome commonly renders an origin without scheme. Reject lookalike domains.
        val host=text.removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore('?').substringBefore(':')
        return host=="instagram.com" || host.endsWith(".instagram.com")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(!::store.isInitialized || event==null || !unlocked) return
        val pkg=event.packageName?.toString() ?: return
        val eventType=event.eventType
        val windowTransition=eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType==AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if(pkg=="com.android.chrome") {
            // A new Chrome window/navigation invalidates the previous URL decision.
            if(windowTransition) chromeInstagram=false
            val urlMatch=instagramUrlVisible()
            if(urlMatch!=null) chromeInstagram=urlMatch
        } else if(windowTransition) chromeInstagram=false
        if(windowTransition) {
            updateApp(classify(pkg))
        } else if(pkg=="com.android.chrome" && eventType==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            updateApp(classify(pkg))
        }
        if(eventType!=AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        val current=classify(pkg)
        if(current=="NONE" || current!=active) return
        // Positive scrollDeltaY indicates content moving forward, usually an upward swipe.
        // Accessibility reports scroll effects, not raw finger gestures.
        val dy=if(Build.VERSION.SDK_INT>=28) event.scrollDeltaY else 0
        if(dy<=0) return
        val now=System.currentTimeMillis()
        if(now-lastScrollTs<400 && lastScrollApp==current) return // coalesce animations/bursts
        val interval=if(lastScrollTs>0 && lastScrollApp==current) now-lastScrollTs else -1
        store.add("SCROLL",current,"deltaY=$dy;intervalMs=$interval",now)
        lastScrollTs=now
        lastScrollApp=current
    }
    override fun onInterrupt() {}
    override fun onDestroy() {
        if(::store.isInitialized) { updateApp("NONE"); store.close() }
        try { unregisterReceiver(screenReceiver) } catch (_:Exception) {}
        super.onDestroy()
    }
}
