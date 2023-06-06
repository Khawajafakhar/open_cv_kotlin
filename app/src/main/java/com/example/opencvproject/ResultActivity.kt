package com.example.opencvproject

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class ResultActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        imageView = findViewById(R.id.result_activity_image_view)
        val intent = intent
        val uriString = intent.getStringExtra("BITMAP_PATH")
        val uri = Uri.parse(uriString)
        imageView.setImageURI(uri)
    }
}