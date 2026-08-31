package com.aiham.virtualtest

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Guest MainActivity onCreate")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val splitBadge = ImageView(this).apply {
            setImageResource(R.drawable.split_badge)
            layoutParams = LinearLayout.LayoutParams(160, 160)
            contentDescription = "Split resource badge"
        }

        val label = TextView(this).apply {
            text = "Aiham Private Space Test\nSplit resource loaded"
            textSize = 24f
            gravity = Gravity.CENTER
        }

        container.addView(splitBadge)
        container.addView(label)
        setContentView(container)
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Guest MainActivity onResume")
    }

    private companion object {
        const val TAG = "AIHAM_GUEST_TEST"
    }
}
