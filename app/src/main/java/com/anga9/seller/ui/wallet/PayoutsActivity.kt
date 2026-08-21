package com.anga9.seller.ui.wallet

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.databinding.ActivityPayoutsBinding
import com.anga9.seller.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PayoutsActivity : BaseActivity() {

    private lateinit var binding: ActivityPayoutsBinding
    private val viewModel: PayoutsViewModel by viewModels()
    private val historyAdapter = PayoutHistoryAdapter()
    
    private val formatINR = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    
    private var availableBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayoutsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        viewModel.fetchData()
    }

    private fun setupUI() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchData()
        }

        binding.layoutHeader.llBack.setOnClickListener {
            finish()
        }

        // Setup History Recycler
        binding.layoutBankTransfers.rvPayoutHistory.apply {
            layoutManager = LinearLayoutManager(this@PayoutsActivity)
            adapter = historyAdapter
        }

        // Setup Processing Grid Card
        binding.layoutProcessing.tvLabel.text = "PAYOUTS PROCESSING"
        binding.layoutProcessing.flIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF7E8"))
        binding.layoutProcessing.ivIcon.setImageResource(R.drawable.ic_warning_triangle) // Use an alert-circle like icon
        binding.layoutProcessing.ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D98E04"))

        // Setup Transferred Grid Card
        binding.layoutTransferred.tvLabel.text = "TOTAL TRANSFERRED"
        binding.layoutTransferred.flIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0FBF4"))
        binding.layoutTransferred.ivIcon.setImageResource(R.drawable.ic_check_circle)
        binding.layoutTransferred.ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#1E7A45"))

        binding.layoutHeroWithdraw.btnWithdraw.setOnClickListener {
            if (availableBalance > 0) {
                showWithdrawConfirmationDialog()
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.earningsState.collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        // Keep current or show loading state
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.layoutHeroWithdraw.tvAvailableAmount.text = "₹..."
                            binding.layoutProcessing.tvAmount.text = "₹..."
                            binding.layoutTransferred.tvAmount.text = "₹..."
                        }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        availableBalance = data?.available ?: 0.0
                        
                        binding.layoutHeroWithdraw.tvAvailableAmount.text = formatINR.format(availableBalance)
                        binding.layoutProcessing.tvAmount.text = formatINR.format(data?.pending ?: 0.0)
                        binding.layoutTransferred.tvAmount.text = formatINR.format(data?.paid ?: 0.0)
                        
                        updateWithdrawButtonState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@PayoutsActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.payoutsState.collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            // Show shimmer or loading indicator for list
                        }
                    }
                    is Resource.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val list = result.data?.payouts ?: emptyList()
                        historyAdapter.submitList(list)
                        
                        if (list.isEmpty()) {
                            binding.layoutBankTransfers.rvPayoutHistory.visibility = View.GONE
                            binding.layoutBankTransfers.layoutEmptyState.root.visibility = View.VISIBLE
                        } else {
                            binding.layoutBankTransfers.rvPayoutHistory.visibility = View.VISIBLE
                            binding.layoutBankTransfers.layoutEmptyState.root.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@PayoutsActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.requestState.collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        // show a loading spinner overlay or similar if needed
                    }
                    is Resource.Success -> {
                        Toast.makeText(this@PayoutsActivity, "Payout requested successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetRequestState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@PayoutsActivity, result.message, Toast.LENGTH_LONG).show()
                        viewModel.resetRequestState()
                    }
                    null -> {}
                }
            }
        }
    }

    private fun updateWithdrawButtonState() {
        val btn = binding.layoutHeroWithdraw.btnWithdraw
        if (availableBalance > 0) {
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2851C4"))
            btn.setTextColor(Color.WHITE)
            btn.isClickable = true
            btn.isFocusable = true
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EFF1F5"))
            btn.setTextColor(Color.parseColor("#9AA1AC"))
            // We set it clickable but catch it in listener if we want to show a toast,
            // or we can just disable it completely. Let's show a toast when clicked.
        }
    }

    private fun showWithdrawConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_withdraw_confirm, null)
        val dialog = BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val tvMaxAmount = dialogView.findViewById<TextView>(R.id.tvMaxAmount)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmitPayout)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvMaxAmount.text = "Max: ${formatINR.format(availableBalance)}"
        
        tvMaxAmount.setOnClickListener {
            etAmount.setText(availableBalance.toString())
            etAmount.setSelection(etAmount.text.length)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount > availableBalance) {
                Toast.makeText(this, "Amount cannot exceed available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.requestPayout(amount)
            dialog.dismiss()
        }

        dialog.show()
    }
}
