package com.anga9.seller.ui.deals

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.utils.Resource
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateDealActivity : BaseActivity() {

    private lateinit var viewModel: CreateDealViewModel

    private lateinit var btnClose: View
    private lateinit var btnSelectProduct: View
    private lateinit var tvSelectedProduct: TextView
    private lateinit var tvProductMrp: TextView
    
    private lateinit var btnSelectDealType: View
    private lateinit var ivDealTypeIcon: ImageView
    private lateinit var tvDealType: TextView
    
    private lateinit var etDealPrice: EditText
    private lateinit var tvPriceError: TextView
    
    private lateinit var btnStartsAt: View
    private lateinit var tvStartsAt: TextView
    
    private lateinit var btnEndsAt: View
    private lateinit var tvEndsAt: TextView
    private lateinit var tvDateError: TextView
    private lateinit var tvDuration: TextView
    
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnPublish: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_deal)
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay)

        viewModel = ViewModelProvider(this)[CreateDealViewModel::class.java]

        initViews()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        btnSelectProduct = findViewById(R.id.btnSelectProduct)
        tvSelectedProduct = findViewById(R.id.tvSelectedProduct)
        tvProductMrp = findViewById(R.id.tvProductMrp)
        
        btnSelectDealType = findViewById(R.id.btnSelectDealType)
        ivDealTypeIcon = findViewById(R.id.ivDealTypeIcon)
        tvDealType = findViewById(R.id.tvDealType)
        
        etDealPrice = findViewById(R.id.etDealPrice)
        tvPriceError = findViewById(R.id.tvPriceError)
        
        btnStartsAt = findViewById(R.id.btnStartsAt)
        tvStartsAt = findViewById(R.id.tvStartsAt)
        
        btnEndsAt = findViewById(R.id.btnEndsAt)
        tvEndsAt = findViewById(R.id.tvEndsAt)
        tvDateError = findViewById(R.id.tvDateError)
        tvDuration = findViewById(R.id.tvDuration)
        
        btnCancel = findViewById(R.id.btnCancel)
        btnPublish = findViewById(R.id.btnPublish)
        progressBar = findViewById(R.id.progressBar)
        
        updateButtonState()
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { handleClose() }
        btnCancel.setOnClickListener { handleClose() }

        btnSelectProduct.setOnClickListener {
            val sheet = ProductPickerBottomSheet { product ->
                viewModel.selectedProduct.value = product
            }
            sheet.show(supportFragmentManager, "ProductPicker")
        }

        btnSelectDealType.setOnClickListener {
            val sheet = DealTypeBottomSheet { dealType ->
                viewModel.dealType.value = dealType
            }
            sheet.show(supportFragmentManager, "DealTypePicker")
        }

        etDealPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.dealPrice.value = s?.toString() ?: ""
                validateForm()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnStartsAt.setOnClickListener { showDateTimePicker { date -> viewModel.startsAt.value = date } }
        btnEndsAt.setOnClickListener { showDateTimePicker { date -> viewModel.endsAt.value = date } }

        btnPublish.setOnClickListener {
            if (viewModel.isFormValid()) {
                viewModel.submitDeal()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.selectedProduct.observe(this) { product ->
            if (product != null) {
                tvSelectedProduct.text = product.name
                tvSelectedProduct.setTextColor(Color.parseColor("#1A1D23"))
                tvProductMrp.visibility = View.VISIBLE
                tvProductMrp.text = "MRP: ₹${product.price}"
            }
            validateForm()
        }

        viewModel.dealType.observe(this) { type ->
            if (type != null) {
                tvDealType.text = type.title
                tvDealType.setTextColor(Color.parseColor("#1A1D23"))
                ivDealTypeIcon.visibility = View.VISIBLE
                try {
                    ivDealTypeIcon.setImageResource(type.iconRes)
                    ivDealTypeIcon.imageTintList = ColorStateList.valueOf(type.iconTint)
                } catch (e: Exception) {}
            }
            validateForm()
        }

        viewModel.startsAt.observe(this) { date ->
            if (date != null) {
                tvStartsAt.text = displayFormat.format(date)
                tvStartsAt.setTextColor(Color.parseColor("#1A1D23"))
            }
            validateForm()
        }

        viewModel.endsAt.observe(this) { date ->
            if (date != null) {
                tvEndsAt.text = displayFormat.format(date)
                tvEndsAt.setTextColor(Color.parseColor("#1A1D23"))
            }
            validateForm()
        }

        viewModel.submitState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnPublish.isEnabled = false
                    btnCancel.isEnabled = false
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Deal Created Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    btnPublish.isEnabled = true
                    btnCancel.isEnabled = true
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateForm() {
        val product = viewModel.selectedProduct.value
        val priceStr = viewModel.dealPrice.value ?: ""
        val price = priceStr.toDoubleOrNull()
        
        var isPriceValid = true
        if (product != null && price != null) {
            if (price >= product.price) {
                isPriceValid = false
                tvPriceError.visibility = View.VISIBLE
                etDealPrice.setBackgroundResource(R.drawable.shape_form_input_error)
            } else {
                tvPriceError.visibility = View.GONE
                etDealPrice.setBackgroundResource(R.drawable.shape_form_input)
            }
        } else {
            tvPriceError.visibility = View.GONE
            etDealPrice.setBackgroundResource(R.drawable.shape_form_input)
        }

        val start = viewModel.startsAt.value
        val end = viewModel.endsAt.value
        var isDateValid = true

        if (start != null && end != null) {
            if (!end.after(start)) {
                isDateValid = false
                tvDateError.visibility = View.VISIBLE
                tvDuration.visibility = View.GONE
                findViewById<LinearLayout>(R.id.btnEndsAt).setBackgroundResource(R.drawable.shape_form_input_error)
            } else {
                tvDateError.visibility = View.GONE
                findViewById<LinearLayout>(R.id.btnEndsAt).setBackgroundResource(R.drawable.shape_form_input)
                
                val diff = end.time - start.time
                val days = diff / (1000 * 60 * 60 * 24)
                tvDuration.visibility = View.VISIBLE
                tvDuration.text = "Runs for ${if(days > 0) "$days days" else "less than a day"}"
            }
        } else {
            tvDateError.visibility = View.GONE
            tvDuration.visibility = View.GONE
            findViewById<LinearLayout>(R.id.btnEndsAt).setBackgroundResource(R.drawable.shape_form_input)
        }

        val isValid = viewModel.isFormValid() && isPriceValid && isDateValid
        updateButtonState(isValid)
    }

    private fun updateButtonState(isEnabled: Boolean = false) {
        if (isEnabled) {
            btnPublish.isEnabled = true
            btnPublish.setBackgroundColor(Color.parseColor("#2851C4"))
            btnPublish.setTextColor(Color.WHITE)
        } else {
            btnPublish.isEnabled = false
            btnPublish.setBackgroundColor(Color.parseColor("#EFF1F5"))
            btnPublish.setTextColor(Color.parseColor("#9AA1AC"))
        }
    }

    private fun showDateTimePicker(onDateSet: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateSet(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun handleClose() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Discard this deal?")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard") { _, _ -> finishWithAnim() }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            finishWithAnim()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        return viewModel.selectedProduct.value != null ||
                viewModel.dealType.value != null ||
                !viewModel.dealPrice.value.isNullOrEmpty() ||
                viewModel.startsAt.value != null ||
                viewModel.endsAt.value != null
    }

    private fun finishWithAnim() {
        finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
    }

    override fun onBackPressed() {
        handleClose()
    }
}
