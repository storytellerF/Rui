package com.storyteller_f.rui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val findViewById = findViewById<com.storyteller_f.rui_library.Rui>(R.id.rui)
        findViewById.listener = com.storyteller_f.rui_library.Rui.RatingChangedListener { current: Float, _: Int, _: Boolean ->
            val b = current > 0f
            Log.i(TAG, "onCreate: $b")
            b
        }
    }
    companion object {
        private const val TAG = "Rui-Main"
    }
}