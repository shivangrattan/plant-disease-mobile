package com.example.pdseg

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.BitmapFactory

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val imageView = findViewById<ImageView>(R.id.resultImageView)
        val severityText = findViewById<TextView>(R.id.severityText)
        val treatmentText = findViewById<TextView>(R.id.treatmentText)
        val backBtn = findViewById<Button>(R.id.backBtn)

        // Receive data
        val imageBytes = intent.getByteArrayExtra("resultImage")
        val severity = intent.getFloatExtra("severity", 0f)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)

        imageView.setImageBitmap(bitmap)
        severityText.text = "Severity: %.2f%%".format(severity * 100)
        treatmentText.text = getTreatment(severity)

        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun getTreatment(ratio: Float): String {
        val index = when {
            ratio == 0f -> 0
            ratio <= 0.03f -> 1
            ratio <= 0.06f -> 2
            ratio <= 0.12f -> 3
            ratio <= 0.25f -> 4
            ratio <= 0.50f -> 5
            ratio <= 0.75f -> 6
            ratio <= 0.87f -> 7
            ratio <= 0.94f -> 8
            ratio <= 0.97f -> 9
            ratio < 1.0f -> 10
            else -> 11
        }

        return when (index) {
            in 1..3 -> "• Prune affected leaves\n• Apply copper-based fungicide\n• Improve air circulation"
            in 4..6 -> "• Regular fungicide use\n• Bio-fungicides\n• Continue pruning"
            in 7..10 -> "• Rotate fungicides\n• Remove severely infected plants\n• Post-harvest sanitation"
            else -> "• No treatment needed"
        }
    }
}
