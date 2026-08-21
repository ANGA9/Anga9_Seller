package com.anga9.seller.ui.support

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.databinding.ActivitySupportHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupportHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportHomeBinding
    private val viewModel: SupportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHomeData()
    }

    private fun setupClickListeners() {
        binding.cvRaiseTicket.setOnClickListener {
            startActivity(Intent(this, RaiseTicketActivity::class.java))
        }

        binding.cvMyTickets.setOnClickListener {
            startActivity(Intent(this, MyTicketsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.homeState.collectLatest { state ->
                when (state) {
                    is SupportUiState.Loading -> {
                        // Keep current UI or show placeholder
                    }
                    is SupportUiState.Success -> {
                        val count = state.openTickets
                        if (count > 0) {
                            binding.tvTicketCountBadge.visibility = View.VISIBLE
                            binding.tvTicketCountBadge.text = count.toString()
                            binding.tvOpenConversations.text = "$count open conversation${if (count > 1) "s" else ""}"
                        } else {
                            binding.tvTicketCountBadge.visibility = View.GONE
                            binding.tvOpenConversations.text = "No open conversations"
                        }
                    }
                    is SupportUiState.Error -> {
                        binding.tvTicketCountBadge.visibility = View.GONE
                    }
                    else -> {}
                }
            }
        }
    }
}
