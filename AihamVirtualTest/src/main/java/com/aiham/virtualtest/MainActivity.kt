package com.aiham.virtualtest

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Aiham Private Space Test"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        setContentView(tv)
    }
}
