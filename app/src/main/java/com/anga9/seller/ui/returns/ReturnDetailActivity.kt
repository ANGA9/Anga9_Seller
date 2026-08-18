package com.anga9.seller.ui.returns

import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anga9.seller.R

/**
 * Return Detail Activity
 *
 * STATUS: Coming Soon — backend endpoints not yet available.
 * Required endpoints (NOT in backend yet):
 *   GET    /api/seller/returns/:id
 *   PATCH  /api/seller/returns/:id/accept
 *   PATCH  /api/seller/returns/:id/reject   body: { "reason": "..." }
 *
 * Showing placeholder UI until backend implements return management.
 */
class ReturnDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_return_detail_seller)

        // Back button
        try { findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() } } catch (_: Exception) {}

        // Hide action buttons, show coming soon
        try {
            val layoutActions = findViewById<android.widget.LinearLayout?>(R.id.layoutActions)
            val progressBarAction = findViewById<android.widget.ProgressBar?>(R.id.progressBarAction)
            val scrollContent = findViewById<ScrollView?>(R.id.scrollContent)

            layoutActions?.visibility = android.view.View.GONE
            progressBarAction?.visibility = android.view.View.GONE
            scrollContent?.visibility = android.view.View.GONE
        } catch (_: Exception) {}

        // Show coming soon message
        try {
            val tvTitle = findViewById<TextView?>(R.id.tvTitle)
            tvTitle?.text = "Return Detail"
        } catch (_: Exception) {}

        Toast.makeText(this, "Return management coming soon", Toast.LENGTH_SHORT).show()
    }
}