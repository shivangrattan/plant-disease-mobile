package com.example.pdseg

import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), LeafDiseaseSegmentationHelper.SegmentationListener {

    private lateinit var imageView: ImageView
    private lateinit var selectImageBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var segmentationHelper: LeafDiseaseSegmentationHelper
    private lateinit var infoText: TextView

    @RequiresApi(Build.VERSION_CODES.Q)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            bitmap?.let { bmp ->
                progressBar.show()
                segmentationHelper.processImage(bmp)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.leafImageView)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        progressBar = findViewById(R.id.progressBar)
        infoText = findViewById(R.id.infoText)

        progressBar.hide()
        segmentationHelper = LeafDiseaseSegmentationHelper(this, this)

        selectImageBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResults(resultImage: Bitmap, inferenceTime: Long, ratio: Float) {
        runOnUiThread {
            progressBar.hide()
            imageView.setImageBitmap(resultImage)
            infoText.text = getSpannableTreatment(ratio)
            Toast.makeText(this, "Processed in $inferenceTime ms", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            progressBar.hide()
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ProgressBar.show() {
        this.visibility = android.view.View.VISIBLE
    }

    private fun ProgressBar.hide() {
        this.visibility = android.view.View.GONE
    }

    fun getSpannableTreatment(ratio: Float): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val percentage = ratio * 100
        val index = when {
            percentage == 0f -> 0
            percentage in 0f..3f -> 1
            percentage in 3f..6f -> 2
            percentage in 6f..12f -> 3
            percentage in 12f..25f -> 4
            percentage in 25f..50f -> 5
            percentage in 50f..75f -> 6
            percentage in 75f..87f -> 7
            percentage in 87f..94f -> 8
            percentage in 94f..97f -> 9
            percentage in 97f..100f -> 10
            else -> 11
        }

        fun appendBold(label: String, value: String? = null) {
            val start = builder.length
            builder.append(label)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (value != null) {
                builder.append(" $value")
            }
            builder.append("\n")
        }

        fun appendBullet(text: String) {
            builder.append("• $text\n")
        }

        // Add percentage and index
        appendBold("Percentage of Diseased Area:", "%.2f".format(percentage) + "%")
        appendBold("Severity Index:", "$index")
        builder.append("\n")

        // Add treatment based on index
        when (index) {
            in 1..3 -> {
                appendBold("Mild Infection (Early Symptoms)")
                appendBold("Recommended Actions:")
                appendBullet("Prune Affected Leaves: Remove and destroy infected lower leaves to prevent disease spread.")
                appendBullet("Enhance Air Circulation: Stake or cage plants to improve airflow and reduce humidity.")
                appendBullet("Mulch Application: Apply organic mulch to prevent soil-borne spores from splashing onto foliage.")
                appendBullet("Watering Practices: Use drip irrigation or water at the base to keep foliage dry.")
                appendBullet("Preventative Fungicides: Apply copper-based fungicides or bio-fungicides like Bacillus subtilis.")
            }

            in 4..6 -> {
                appendBold("Moderate Infection")
                appendBold("Recommended Actions:")
                appendBullet("Regular Fungicide Applications: Use fungicides containing chlorothalonil or copper-based products every 7–10 days.")
                appendBullet("Biological Controls: Apply bio-fungicides like Bacillus subtilis (Serenade) or Bacillus amyloliquefaciens (Double Nickel).")
                appendBullet("Cultural Practices: Continue removing infected leaves and maintain proper spacing to reduce humidity.")
            }

            in 7..11 -> {
                appendBold("Severe Infection")
                appendBold("Recommended Actions:")
                appendBullet("Intensive Fungicide Regimen: Rotate fungicides with different modes of action, like strobilurins and chlorothalonil.")
                appendBullet("Remove Severely Affected Plants: Uproot and destroy heavily infected plants.")
                appendBullet("Post-Harvest Sanitation: Remove all plant debris and practice crop rotation to prevent overwintering pathogens.")
            }

            else -> {
                appendBold("Healthy Leaf")
                appendBullet("No treatment necessary.")
            }
        }

        return builder
    }
}
