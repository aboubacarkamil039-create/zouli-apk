package com.kamil.zouli

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "Zouli v0.1\nCréée par Kamil"
        t.textSize = 24f
        t.gravity = Gravity.CENTER
        setContentView(t)
    }
}
