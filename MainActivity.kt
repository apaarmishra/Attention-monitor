package com.example.attentionmonitor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.AlertDialog
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var panel: LinearLayout
    private val store by lazy { Store(this) }
    private fun label(s:String, size:Float=16f, bold:Boolean=false):TextView = TextView(this).apply {
        text=s; textSize=size;setTextColor(Color.rgb(33,42,55));setPadding(0,10,0,10)
        if(bold) setTypeface(null,Typeface.BOLD)
    }
    private fun section(title:String) { panel.addView(label(title,20f,true)) }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll=ScrollView(this)
        panel=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(40,40,40,40) }
        scroll.addView(panel)
        setContentView(scroll)
        render()
    }
    override fun onResume() { super.onResume(); if(::panel.isInitialized) render() }
    private fun render() {
        panel.removeAllViews()
        panel.addView(label("Attention Monitor",28f,true))
        panel.addView(label("Private, on-device tracking • V1 experimental"))
        val enabled=Settings.Secure.getString(contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.split(':')?.any { it.contains(packageName) && it.contains("MonitorService") }==true
        panel.addView(label(if(enabled) "Tracking service: ENABLED" else "Tracking service: DISABLED",16f,true))
        val startButton=Button(this).apply { text="Enable / inspect accessibility permission";setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        panel.addView(startButton)
        panel.addView(label("Grant access only if comfortable. Android may display a broad warning because accessibility can access on-screen information. This app only reads Chrome's address bar to identify Instagram and records timestamps, app names, and scroll deltas." ,13f))
        val refresh=Button(this).apply { text="Refresh report";setOnClickListener { render() } }
        panel.addView(refresh)
        val events=store.read(Store.todayStart())
        val scrolls=events.filter { it.kind=="SCROLL" }
        val known=listOf("Facebook","Instagram Web","YouTube")
        section("Today")
        panel.addView(label("Detected upward scroll events: ${scrolls.size}",18f,true))
        val intervals=scrolls.mapNotNull { Regex("intervalMs=(\\d+)").find(it.detail)?.groupValues?.get(1)?.toLongOrNull() }.filter{it in 1..300000}
        panel.addView(label("Average interval between detected scrolls (same app): ${if(intervals.isEmpty()) "—" else "%.1fs".format(Locale.US,intervals.average()/1000.0)}"))
        val switches=events.count { it.kind=="SWITCH" && it.app!="NONE" && it.detail!="from=NONE" }
        panel.addView(label("Direct monitored-app switches: $switches"))
        // Durations are based on observed foreground transitions. Gaps are not backfilled.
        val transitions=events.filter { it.kind=="SWITCH" || it.kind=="LOCK" }
        val duration=mutableMapOf<String,Long>()
        for(i in 0 until transitions.size-1) {
            val e=transitions[i];val next=transitions[i+1]
            if(e.kind=="SWITCH" && e.app in known)
                duration[e.app]=(duration[e.app]?:0L)+(next.ts-e.ts).coerceAtLeast(0L)
        }
        // Include current segment only if service appears active and latest event is a monitored app.
        // This is an estimate; the service may have been stopped by Android meanwhile.
        if(enabled && transitions.lastOrNull()?.kind=="SWITCH" && transitions.last().app in known) {
            val last=transitions.last()
            duration[last.app]=(duration[last.app]?:0L)+(System.currentTimeMillis()-last.ts).coerceAtLeast(0L)
        }
        for(app in known) {
            val count=scrolls.count { it.app==app }
            panel.addView(label("$app: $count scrolls · ${(duration[app]?:0L)/60000} min (estimated)"))
        }
        section("Recent event timeline")
        events.takeLast(35).asReversed().forEach { e ->
            val detail=if(e.kind=="SCROLL") e.detail.replace(";", " · ") else e.detail
            panel.addView(label("${Store.format(e.ts)}   ${e.kind}   ${e.app}   $detail",13f))
        }
        val clear=Button(this).apply { text="Delete all local history";setOnClickListener {
            AlertDialog.Builder(this@MainActivity).setTitle("Delete all stored events?")
                .setMessage("This permanently clears the on-device event database.")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_-> store.clear();render() }.show()
        } }
        panel.addView(clear)
        panel.addView(label("Caveats: not raw swipes; some apps do not report scroll deltas. Chrome URL detection requires a readable address bar and may miss Instagram when the bar is hidden. Foreground duration estimates can be incomplete if the service is interrupted.",13f))
    }
    override fun onDestroy() { store.close();super.onDestroy() }
}
