package com.storyteller_f.rui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val findViewById = findViewById<com.storyteller_f.rui_library.Rui>(R.id.rui)
        findViewById.listener = com.storyteller_f.rui_library.Rui.RatingChangedListener { _: Float, _: Int, _: Boolean ->
            true
        }
    }
}