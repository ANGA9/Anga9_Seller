package com.anga9.seller.MVVM.ui.products

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.MVVM.data.repository.ProductRepository
import com.anga9.seller.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class BulkUploadActivity : BaseActivity() {
    private lateinit var repository: ProductRepository

    // Header
    private lateinit var btnBackStacked: ImageView
    
    // States layout
    private lateinit var layoutIdleState: View
    private lateinit var layoutSelectedState: View
    private lateinit var layoutSuccessState: View
    private lateinit var layoutErrorState: View

    // Idle views
    private lateinit var btnBrowseFiles: MaterialButton

    // Selected/Uploading views
    private lateinit var tvSelectedFileName: TextView
    private lateinit var tvSelectedFileSize: TextView
    private lateinit var btnCancelSelection: ImageView
    private lateinit var progressUpload: ProgressBar
    private lateinit var btnStartUpload: MaterialButton

    // Success views
    private lateinit var tvSuccessHeading: TextView
    private lateinit var tvSuccessSubtext: TextView
    private lateinit var btnUploadAnotherSuccess: TextView

    // Error views
    private lateinit var tvErrorReason: TextView
    private lateinit var btnUploadAnotherError: TextView
    private lateinit var btnTryAgain: MaterialButton

    // Instructions
    private lateinit var llInstructionsList: LinearLayout

    // Bottom action
    private lateinit var btnDownloadTemplate: MaterialButton

    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                val (name, size) = getFileNameAndSize(uri)
                
                // Show selected state
                transitionToState(State.SELECTED)
                tvSelectedFileName.text = name
                tvSelectedFileSize.text = size
                
                // Reset progress and enable buttons
                progressUpload.visibility = View.GONE
                progressUpload.progress = 0
                btnCancelSelection.visibility = View.VISIBLE
                btnStartUpload.isEnabled = true
                btnStartUpload.text = "Start Upload"
            }
        }
    }

    enum class State {
        IDLE, SELECTED, UPLOADING, SUCCESS, ERROR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bulk_upload)

        repository = ProductRepository(this)
        
        initViews()
        setupListeners()
        populateInstructions()
        
        // Initial state
        transitionToState(State.IDLE)
    }

    private fun initViews() {
        btnBackStacked = findViewById(R.id.btnBackStacked)

        layoutIdleState = findViewById(R.id.layoutIdleState)
        layoutSelectedState = findViewById(R.id.layoutSelectedState)
        layoutSuccessState = findViewById(R.id.layoutSuccessState)
        layoutErrorState = findViewById(R.id.layoutErrorState)

        btnBrowseFiles = findViewById(R.id.btnBrowseFiles)
        
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName)
        tvSelectedFileSize = findViewById(R.id.tvSelectedFileSize)
        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        progressUpload = findViewById(R.id.progressUpload)
        btnStartUpload = findViewById(R.id.btnStartUpload)

        tvSuccessHeading = findViewById(R.id.tvSuccessHeading)
        tvSuccessSubtext = findViewById(R.id.tvSuccessSubtext)
        btnUploadAnotherSuccess = findViewById(R.id.btnUploadAnotherSuccess)

        tvErrorReason = findViewById(R.id.tvErrorReason)
        btnUploadAnotherError = findViewById(R.id.btnUploadAnotherError)
        btnTryAgain = findViewById(R.id.btnTryAgain)

        llInstructionsList = findViewById(R.id.llInstructionsList)
        
        btnDownloadTemplate = findViewById(R.id.btnDownloadTemplate)
    }

    private fun setupListeners() {
        btnBackStacked.setOnClickListener { finish() }

        btnBrowseFiles.setOnClickListener { openFilePicker() }
        
        btnCancelSelection.setOnClickListener {
            selectedFileUri = null
            transitionToState(State.IDLE)
        }

        btnStartUpload.setOnClickListener {
            startUpload()
        }

        val uploadAnotherAction = View.OnClickListener {
            selectedFileUri = null
            transitionToState(State.IDLE)
        }
        
        btnUploadAnotherSuccess.setOnClickListener(uploadAnotherAction)
        btnUploadAnotherError.setOnClickListener(uploadAnotherAction)
        
        btnTryAgain.setOnClickListener {
            startUpload()
        }

        btnDownloadTemplate.setOnClickListener {
            downloadDummyTemplate()
        }
    }

    private fun populateInstructions() {
        val rules = listOf(
            "Download the template to ensure your columns match exactly.",
            "Maximum 500 rows per upload.",
            "Category IDs must be valid UUIDs from the category list. Multiple IDs can be separated by commas inside quotes.",
            "Empty or invalid rows will be skipped, and successful rows will still be imported."
        )

        llInstructionsList.removeAllViews()
        for ((index, rule) in rules.withIndex()) {
            val ruleLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index > 0) (8 * resources.displayMetrics.density).toInt() else 0
                }
            }

            val tvBullet = TextView(this).apply {
                text = "•"
                setTextColor(android.graphics.Color.parseColor("#9AA1AC"))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
            }

            val tvRule = TextView(this).apply {
                text = rule
                setTextColor(android.graphics.Color.parseColor("#5B6472"))
                textSize = 12f
                setLineSpacing(0f, 1.5f)
            }

            ruleLayout.addView(tvBullet)
            ruleLayout.addView(tvRule)
            llInstructionsList.addView(ruleLayout)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
            // Fallback for some devices where "text/csv" doesn't list anything
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "application/csv"))
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select CSV"))
    }

    private fun transitionToState(state: State) {
        layoutIdleState.visibility = if (state == State.IDLE) View.VISIBLE else View.GONE
        layoutSelectedState.visibility = if (state == State.SELECTED || state == State.UPLOADING) View.VISIBLE else View.GONE
        layoutSuccessState.visibility = if (state == State.SUCCESS) View.VISIBLE else View.GONE
        layoutErrorState.visibility = if (state == State.ERROR) View.VISIBLE else View.GONE

        if (state == State.UPLOADING) {
            btnCancelSelection.visibility = View.GONE
            btnStartUpload.isEnabled = false
            btnStartUpload.text = "Uploading..."
            progressUpload.visibility = View.VISIBLE
        }
    }

    private fun startUpload() {
        val uri = selectedFileUri ?: return
        transitionToState(State.UPLOADING)
        progressUpload.isIndeterminate = true
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val buffer = ByteArrayOutputStream()
                inputStream?.use { it.copyTo(buffer) }
                val bytes = buffer.toByteArray()
                
                val requestBody = bytes.toRequestBody("text/csv".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", "upload.csv", requestBody)
                
                val result = repository.bulkUploadProducts(filePart)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    showSuccessState(data?.successCount ?: 0, data?.failedCount ?: 0)
                } else {
                    showErrorState(result.exceptionOrNull()?.message ?: "Upload failed")
                }
            } catch (e: Exception) {
                showErrorState(e.message ?: "File reading failed")
            }
        }
    }

    private fun showSuccessState(successCount: Int, failedCount: Int) {
        transitionToState(State.SUCCESS)
        
        if (failedCount > 0) {
            tvSuccessHeading.text = "$successCount products imported successfully"
            tvSuccessSubtext.visibility = View.VISIBLE
            tvSuccessSubtext.text = "$failedCount rows skipped — view details"
        } else {
            tvSuccessHeading.text = "All products imported successfully"
            tvSuccessSubtext.visibility = View.GONE
        }
    }

    private fun showErrorState(reason: String) {
        transitionToState(State.ERROR)
        tvErrorReason.text = reason
    }

    private fun getFileNameAndSize(uri: Uri): Pair<String, String> {
        var name = "Unknown file"
        var sizeStr = ""
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    sizeStr = formatFileSize(sizeBytes)
                }
            }
        }
        return Pair(name, sizeStr)
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun downloadDummyTemplate() {
        // Just opens a dummy URL in browser/intent for now
        val url = "https://seller.anga9.com/templates/anga9_bulk_product_template.csv"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        try {
            startActivity(intent)
            Toast.makeText(this, "Downloading template...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to handle download.", Toast.LENGTH_SHORT).show()
        }
    }
}
