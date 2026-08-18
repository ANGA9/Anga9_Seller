package com.anga9.seller.ui.brand

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.utils.Resource
import com.anga9.seller.utils.TokenManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * Bottom sheet for brand switching and brand creation.
 * Plan ref: MULTI_BRAND_MANAGEMENT_IMPLEMENTATION_PLAN.md — Phase 2.2 & 2.4
 *
 * Shows:
 *  - List of all brands with active brand highlighted
 *  - "+ Add New Brand" button that opens the Add Brand dialog
 *
 * On brand selection:
 *  - Calls BrandViewModel.switchBrand() which persists + broadcasts
 *  - Dismisses the sheet
 */
class BrandSwitcherBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: BrandViewModel by activityViewModels()

    private lateinit var rvBrands: RecyclerView
    private lateinit var btnAddBrand: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.Theme_ANGA_SellerSide)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_brand_switcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvBrands = view.findViewById(R.id.rvBrands)
        btnAddBrand = view.findViewById(R.id.btnAddBrand)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        rvBrands.layoutManager = LinearLayoutManager(requireContext())

        // Observe brand list
        viewModel.brands.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvError.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    tvError.visibility = View.GONE
                    val brands = resource.data ?: emptyList()
                    // Phase 5.5: save child brand count so DashboardActivity chip can show badge
                    val childCount = brands.count { it.parentUserId != null }
                    requireContext().getSharedPreferences("anga9_seller_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putInt("total_child_brand_count", childCount)
                        .apply()
                    // Also cache brand names for chip label lookups
                    val prefsEditor = requireContext().getSharedPreferences("anga9_seller_prefs", android.content.Context.MODE_PRIVATE).edit()
                    brands.forEach { brand ->
                        brand.storeName?.let { name -> prefsEditor.putString("brand_name_${brand.id}", name) }
                    }
                    prefsEditor.apply()
                    val activeBrandId = TokenManager.getActiveBrandId(requireContext())
                    rvBrands.adapter = BrandAdapter(activeBrandId) { selectedBrand ->
                        viewModel.switchBrand(requireContext(), selectedBrand.id)
                        dismiss()
                    }
                    (rvBrands.adapter as BrandAdapter).submitList(brands)
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    tvError.visibility = View.VISIBLE
                    tvError.text = resource.message ?: "Failed to load brands"
                }
                else -> {}
            }
        }

        // Observe create result
        viewModel.createResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { newBrand ->
                        Toast.makeText(
                            requireContext(),
                            "Brand \"${newBrand.storeName}\" created",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Switch to the newly created brand immediately
                        viewModel.switchBrand(requireContext(), newBrand.id)
                        // Reload brands list to include the new one
                        viewModel.loadBrands()
                        dismiss()
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: "Failed to create brand",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }

        // Add Brand button -> show dialog
        btnAddBrand.setOnClickListener { showAddBrandDialog() }

        // Load brands if not already loaded
        viewModel.loadBrands()
    }

    /**
     * Shows the Add Brand dialog.
     * Plan ref: Phase 2.4 — dialog_add_brand.xml
     */
    private fun showAddBrandDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_brand, null)
        val etBrandName = dialogView.findViewById<EditText>(R.id.etBrandName)
        val etStoreSlug = dialogView.findViewById<EditText>(R.id.etStoreSlug)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            val name = etBrandName.text.toString().trim()
            val slug = etStoreSlug.text.toString().trim()

            if (name.isEmpty()) {
                etBrandName.error = "Brand name is required"
                return@setOnClickListener
            }
            if (slug.isEmpty()) {
                etStoreSlug.error = "Store slug is required"
                return@setOnClickListener
            }
            if (!slug.matches(Regex("^[a-z0-9-]+$"))) {
                etStoreSlug.error = "Slug: lowercase letters, numbers, hyphens only"
                return@setOnClickListener
            }

            dialog.dismiss()
            viewModel.createBrand(name, slug)
        }

        dialog.show()
    }

    companion object {
        const val TAG = "BrandSwitcherBottomSheet"

        fun newInstance(): BrandSwitcherBottomSheet = BrandSwitcherBottomSheet()
    }
}
