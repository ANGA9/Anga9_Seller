package com.anga9.seller.ui.support

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.R
import com.anga9.seller.databinding.ActivityRaiseTicketBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RaiseTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRaiseTicketBinding
    private val viewModel: SupportViewModel by viewModels()

    private val categories = listOf(
        "Payouts", "Listing rejected", "KYC", "Inventory issue", "Commission dispute", "Account", "Other"
    )

    private val attachments = mutableListOf<Uri>()
    private lateinit var attachmentAdapter: AttachmentPreviewAdapter

    private var selectedPriority = "medium"

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    if (attachments.size < 3) {
                        attachments.add(data.clipData!!.getItemAt(i).uri)
                    }
                }
            } else if (data?.data != null) {
                if (attachments.size < 3) {
                    attachments.add(data.data!!)
                }
            }
            updateAttachmentsUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRaiseTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            // Confirm discard? Just finish for now or show dialog if fields are filled.
            finish()
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Category
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actCategory.setAdapter(adapter)
        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            checkSubmitEnable()
        }

        // Character counter and validation
        binding.etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvCharCount.text = "$length / 500"
                if (length >= 500) {
                    binding.tvCharCount.setTextColor(ContextCompat.getColor(this@RaiseTicketActivity, R.color.error_red))
                } else {
                    binding.tvCharCount.setTextColor(ContextCompat.getColor(this@RaiseTicketActivity, R.color.text_muted))
                }
                checkSubmitEnable()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSubject.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkSubmitEnable()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Priorities
        binding.btnPriorityLow.setOnClickListener { selectPriority("low") }
        binding.btnPriorityMedium.setOnClickListener { selectPriority("medium") }
        binding.btnPriorityHigh.setOnClickListener { selectPriority("high") }
        binding.btnPriorityUrgent.setOnClickListener { selectPriority("urgent") }

        // Attachments
        attachmentAdapter = AttachmentPreviewAdapter(attachments) { index ->
            if (index in attachments.indices) {
                attachments.removeAt(index)
                updateAttachmentsUI()
            }
        }
        binding.rvAttachments.adapter = attachmentAdapter

        binding.clDropzone.setOnClickListener {
            if (attachments.size < 3) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpeg", "application/pdf"))
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                filePickerLauncher.launch(Intent.createChooser(intent, "Select Files"))
            }
        }

        // Submit
        binding.btnSubmit.setOnClickListener {
            if (checkSubmitEnable()) {
                val cat = binding.actCategory.text.toString()
                val sub = binding.etSubject.text.toString().trim()
                val desc = binding.etDescription.text.toString().trim()

                viewModel.createTicket(
                    subject = sub,
                    category = cat,
                    description = desc,
                    priority = selectedPriority,
                    attachments = attachments
                )
            }
        }

        checkSubmitEnable()
    }

    private fun selectPriority(priority: String) {
        selectedPriority = priority
        val buttons = listOf(binding.btnPriorityLow, binding.btnPriorityMedium, binding.btnPriorityHigh, binding.btnPriorityUrgent)
        val selectedBtn = when(priority) {
            "low" -> binding.btnPriorityLow
            "high" -> binding.btnPriorityHigh
            "urgent" -> binding.btnPriorityUrgent
            else -> binding.btnPriorityMedium
        }

        for (btn in buttons) {
            if (btn == selectedBtn) {
                btn.setBackgroundResource(R.drawable.bg_priority_button_selected)
                btn.setTextColor(ContextCompat.getColor(this, R.color.primary))
                btn.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                btn.setBackgroundResource(R.drawable.bg_priority_button_unselected)
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                btn.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun updateAttachmentsUI() {
        if (attachments.size >= 3) {
            binding.clDropzone.visibility = View.GONE
        } else {
            binding.clDropzone.visibility = View.VISIBLE
        }
        attachmentAdapter.notifyDataSetChanged()
    }

    private fun checkSubmitEnable(): Boolean {
        val cat = binding.actCategory.text.toString()
        val sub = binding.etSubject.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()

        val isValid = cat.isNotEmpty() && sub.isNotEmpty() && desc.isNotEmpty()
        binding.btnSubmit.isEnabled = isValid
        binding.btnSubmit.alpha = if (isValid) 1.0f else 0.5f
        return isValid
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.createState.collectLatest { state ->
                when (state) {
                    is CreateTicketState.Loading -> {
                        binding.btnSubmit.isEnabled = false
                        binding.btnSubmit.text = "Submitting..."
                    }
                    is CreateTicketState.Success -> {
                        Toast.makeText(this@RaiseTicketActivity, "Ticket created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is CreateTicketState.Error -> {
                        binding.btnSubmit.isEnabled = true
                        binding.btnSubmit.text = "Submit ticket"
                        Toast.makeText(this@RaiseTicketActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
