package com.aiham.virtualtest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Guest MainActivity onCreate")

        val tv = TextView(this).apply {
            text = "Aiham Private Space Test"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        setContentView(tv)
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Guest MainActivity onResume")
    }

    private companion object {
        const val TAG = "AIHAM_GUEST_TEST"
    }
}
