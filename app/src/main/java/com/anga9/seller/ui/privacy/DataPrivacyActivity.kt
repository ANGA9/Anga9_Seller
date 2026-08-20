package com.anga9.seller.ui.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.auth.SellerPhoneLoginActivity
import com.anga9.seller.data.model.privacy.DataPrivacyConfig
import com.anga9.seller.ui.legal.LegalActivity
import com.anga9.seller.utils.Constants
import com.anga9.seller.utils.FcmTokenManager
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DataPrivacyActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DataPrivacyActivity::class.java))
        }
    }

    private val viewModel: DataPrivacyViewModel by viewModels()

    private lateinit var btnBack: ImageView
    private lateinit var btnReadFullPolicy: TextView
    private lateinit var layoutPermissionsContainer: LinearLayout
    private lateinit var btnDeleteSellerAccount: MaterialButton

    private var activeBottomSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_privacy)

        initViews()
        setupListeners()
        populatePermissions()
        observeViewModel()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnReadFullPolicy = findViewById(R.id.btnReadFullPolicy)
        layoutPermissionsContainer = findViewById(R.id.layoutPermissionsContainer)
        btnDeleteSellerAccount = findViewById(R.id.btnDeleteSellerAccount)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnReadFullPolicy.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.anga9.com/privacy?audience=seller"))
                startActivity(intent)
            } catch (e: Exception) {
                LegalActivity.startPrivacy(this)
            }
        }

        btnDeleteSellerAccount.setOnClickListener {
            showDeleteAccountBottomSheet()
        }
    }

    /**
     * Dynamically populates the single-column permissions list from config
     */
    private fun populatePermissions() {
        layoutPermissionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        DataPrivacyConfig.permissions.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_app_permission, layoutPermissionsContainer, false)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivPermissionIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvPermissionTitle)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvPermissionDesc)

            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            tvDesc.text = item.description

            layoutPermissionsContainer.addView(itemView)
        }
    }

    /**
     * Explicit high-friction confirmation bottom sheet for account deletion
     */
    private fun showDeleteAccountBottomSheet() {
        viewModel.resetDeleteState()

        val bottomSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete_seller_account, null)
        bottomSheet.setContentView(sheetView)
        activeBottomSheet = bottomSheet

        val etConfirm = sheetView.findViewById<EditText>(R.id.etConfirmDelete)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelDelete)
        val btnConfirm = sheetView.findViewById<MaterialButton>(R.id.btnConfirmDelete)
        val tvError = sheetView.findViewById<TextView>(R.id.tvDeleteError)
        val pbDeleting = sheetView.findViewById<ProgressBar>(R.id.pbDeleting)

        btnConfirm.isEnabled = false
        btnConfirm.alpha = 0.5f

        etConfirm.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString()?.trim() ?: ""
                val isValid = input == "DELETE"
                btnConfirm.isEnabled = isValid
                btnConfirm.alpha = if (isValid) 1.0f else 0.5f
                tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancel.setOnClickListener {
            bottomSheet.dismiss()
        }

        btnConfirm.setOnClickListener {
            tvError.visibility = View.GONE
            viewModel.deleteAccount()
        }

        bottomSheet.setOnDismissListener {
            activeBottomSheet = null
            viewModel.resetDeleteState()
        }

        bottomSheet.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.deleteState.collect { state ->
                val sheet = activeBottomSheet ?: return@collect
                val pbDeleting = sheet.findViewById<ProgressBar>(R.id.pbDeleting)
                val btnCancel = sheet.findViewById<MaterialButton>(R.id.btnCancelDelete)
                val btnConfirm = sheet.findViewById<MaterialButton>(R.id.btnConfirmDelete)
                val tvError = sheet.findViewById<TextView>(R.id.tvDeleteError)
                val etConfirm = sheet.findViewById<EditText>(R.id.etConfirmDelete)

                when (state) {
                    is UiState.Idle -> {
                        pbDeleting?.visibility = View.GONE
                        btnCancel?.isEnabled = true
                        val isDeleteTyped = etConfirm?.text?.toString()?.trim() == "DELETE"
                        btnConfirm?.isEnabled = isDeleteTyped
                        btnConfirm?.alpha = if (isDeleteTyped) 1.0f else 0.5f
                    }
                    is UiState.Loading -> {
                        pbDeleting?.visibility = View.VISIBLE
                        btnCancel?.isEnabled = false
                        btnConfirm?.isEnabled = false
                        btnConfirm?.alpha = 0.5f
                        tvError?.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        pbDeleting?.visibility = View.GONE
                        sheet.dismiss()
                        showToast("Seller account successfully closed and deleted")
                        performCompleteLogout()
                    }
                    is UiState.Error -> {
                        pbDeleting?.visibility = View.GONE
                        btnCancel?.isEnabled = true
                        val isDeleteTyped = etConfirm?.text?.toString()?.trim() == "DELETE"
                        btnConfirm?.isEnabled = isDeleteTyped
                        btnConfirm?.alpha = if (isDeleteTyped) 1.0f else 0.5f
                        tvError?.text = state.message
                        tvError?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun performCompleteLogout() {
        FcmTokenManager.clearToken(this)
        TokenManager.clearAll(this)
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, SellerPhoneLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
