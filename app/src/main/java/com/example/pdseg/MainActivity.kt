package com.example.pdseg

import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.github.chrisbanes.photoview.PhotoView

class MainActivity : AppCompatActivity(), LeafDiseaseSegmentationHelper.SegmentationListener {

    private lateinit var imageView: ImageView
    private lateinit var selectImageBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var segmentationHelper: LeafDiseaseSegmentationHelper
    private lateinit var infoText: TextView
    private lateinit var severityText: TextView
    private lateinit var cycleBtn: Button
    private lateinit var titleText: TextView

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
        severityText = findViewById(R.id.severityText)

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

    override fun onResults(resizedBitmap: Bitmap, resultImage: Bitmap, leafMask: Bitmap, inferenceTime: Long, ratio: Float) {
        runOnUiThread {
            progressBar.hide()
            imageView = findViewById(R.id.leafImageView)
            selectImageBtn = findViewById(R.id.selectImageBtn)
            progressBar = findViewById(R.id.progressBar)
            infoText = findViewById(R.id.infoText)
            severityText = findViewById(R.id.severityText)
            cycleBtn = findViewById(R.id.cycleBtn)
            titleText = findViewById(R.id.titleText)

            titleText.text = "Diseased Spots"
            imageView.setImageBitmap(resultImage)
            severityText.text = "Severity: %.2f%%".format(ratio * 100)
            infoText.text = getTreatment(ratio)

            cycleBtn.alpha = 0f
            cycleBtn.visibility = View.VISIBLE
            cycleBtn.animate().alpha(1f).setDuration(300).start()

            imageView.setOnClickListener {
                val dialog = FullscreenImageDialog(resultImage)
                dialog.show(supportFragmentManager, "fullscreenImage")
            }

            var current_img = 0
            cycleBtn.setOnClickListener {
                when (current_img) {
                    0 -> {
                        current_img++
                        titleText.text = "Extracted Leaf"
                        imageView.setImageBitmap(leafMask)
                        imageView.setOnClickListener {
                            val dialog = FullscreenImageDialog(leafMask)
                            dialog.show(supportFragmentManager, "fullscreenImage")
                        }
                    }
                    1 -> {
                        current_img++
                        titleText.text = "Original Image"
                        imageView.setImageBitmap(resizedBitmap)
                        imageView.setOnClickListener {
                            val dialog = FullscreenImageDialog(resizedBitmap)
                            dialog.show(supportFragmentManager, "fullscreenImage")
                        }
                    }
                    else -> {
                        current_img = 0
                        titleText.text = "Diseased Spots"
                        imageView.setImageBitmap(resultImage)
                        imageView.setOnClickListener {
                            val dialog = FullscreenImageDialog(resultImage)
                            dialog.show(supportFragmentManager, "fullscreenImage")
                        }
                    }
                }
            }
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            progressBar.hide()
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ProgressBar.show() {
        this.visibility = View.VISIBLE
    }

    private fun ProgressBar.hide() {
        this.visibility = View.GONE
    }

    private fun getTreatment(ratio: Float): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val percentage = ratio * 100
        val index = when {
            percentage == 0f -> 0
            percentage in 0f..5f -> 1
            percentage in 6f..20f -> 2
            percentage in 21f..40f -> 3
            percentage in 41f..70f -> 4
            else -> 5
        }

        fun appendLarge(label: String, value: String? = null, scale: Float = 1.15f) {
            val start = builder.length
            builder.append(label)
            builder.setSpan(
                RelativeSizeSpan(scale),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (value != null) {
                builder.append(" $value")
            }
            builder.append("\n\n")
        }

        fun appendBullet(text: String) {
            val start = builder.length
            builder.append(text + "\n\n")
            builder.setSpan(
                BulletSpan(20),  // 20px gap between bullet and text, adjust as needed
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        fun appendBulletEnd(text: String) {
            val start = builder.length
            builder.append(text + "\n")
            builder.setSpan(
                BulletSpan(20),  // 20px gap between bullet and text, adjust as needed
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Add treatment based on index
        when (index) {
            in 1..1 -> {
                appendLarge("Severity Index: $index (Mild Infection)")
                appendBullet("Remove and destroy infected lower leaves to prevent disease spread.")
                appendBullet("Stake or cage plants to improve airflow and reduce humidity.")
                appendBulletEnd("Use drip irrigation or water at the base to keep foliage dry.")
            }

            in 2..3 -> {
                appendLarge("Severity Index: $index (Moderate Infection)")
                appendBullet("Use fungicides containing chlorothalonil or copper-based products every 7–10 days.")
                appendBullet("Apply bio-fungicides like Bacillus subtilis (Serenade) or Bacillus amyloliquefaciens (Double Nickel).")
                appendBulletEnd("Continue removing infected leaves and maintain proper spacing to reduce humidity.")
            }

            in 4..5 -> {
                appendLarge("Severity Index: $index (Severe Infection)")
                appendBullet("Rotate fungicides with different modes of action, like strobilurins and chlorothalonil.")
                appendBullet("Uproot and destroy heavily infected plants.")
                appendBulletEnd("Remove all plant debris and practice crop rotation to prevent overwintering pathogens.")
            }

            else -> {
                appendLarge("Healthy Leaf")
                appendBulletEnd("No treatment necessary.")
            }
        }

        return builder
    }
    class FullscreenImageDialog(private val bitmap: Bitmap) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_fullscreen_image)
            dialog.setCancelable(true)

            val photoView = dialog.findViewById<PhotoView>(R.id.fullscreenImage)
            photoView.setImageBitmap(bitmap)

            // Optional: Dismiss the dialog when the image is tapped
            photoView.setOnClickListener { dismiss() }

            return dialog
        }
    }

}

