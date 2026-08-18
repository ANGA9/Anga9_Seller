package com.anga9.seller.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.BaseActivity
import com.google.android.material.textfield.TextInputEditText

/**
 * Delivery Zones Activity
 *
 * STATUS: Coming Soon — backend endpoints not yet available.
 * Required endpoints (NOT in backend yet):
 *   GET    /api/users/seller-profile/delivery-zones
 *   POST   /api/users/seller-profile/delivery-zones
 *   DELETE /api/users/seller-profile/delivery-zones/:pincode
 *
 * Showing placeholder UI until backend implements this feature.
 */
class DeliveryZonesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_zones)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Hide all functional UI — show coming soon state
        val inputLayout = (findViewById<View?>(R.id.etPincode)?.parent?.parent as? View)
        val btnAdd   = findViewById<View?>(R.id.btnAddPincode)
        val rvList   = findViewById<RecyclerView?>(R.id.rvPincodes)
        val btnSave  = findViewById<View?>(R.id.btnSaveZones)

        inputLayout?.visibility = View.GONE
        btnAdd?.visibility      = View.GONE
        rvList?.visibility      = View.GONE
        btnSave?.visibility     = View.GONE

        // Show empty/coming soon text
        val tvEmpty = findViewById<TextView?>(R.id.tvEmptyState)
        tvEmpty?.visibility = View.VISIBLE
        tvEmpty?.text = "Delivery Zones management is coming soon.\n\nThis feature is currently under development."
        showToast("Delivery Zones coming soon!")
    }
}

/**
 * PincodeAdapter stub — kept to avoid compilation errors from any remaining references.
 */
class PincodeAdapter(
    private val pincodes: List<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<PincodeAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvPincode: TextView = view.findViewById(R.id.tvPincode)
        val btnRemove: android.widget.ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pincode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pincode = pincodes[position]
        holder.tvPincode.text = pincode
        holder.btnRemove.setOnClickListener { onRemove(pincode) }
    }

    override fun getItemCount() = pincodes.size
}
