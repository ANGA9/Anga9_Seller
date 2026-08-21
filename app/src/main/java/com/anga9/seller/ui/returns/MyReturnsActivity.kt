package com.anga9.seller.ui.returns

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anga9.seller.BaseActivity
import com.anga9.seller.R

/**
 * My Returns Activity
 *
 * STATUS: Coming Soon — backend endpoints not yet available.
 * Required endpoints (NOT in backend yet):
 *   GET    /api/seller/returns
 *   GET    /api/seller/returns/:id
 *   PATCH  /api/seller/returns/:id/accept
 *   PATCH  /api/seller/returns/:id/reject
 *
 * Showing placeholder UI until backend implements return management.
 */
class MyReturnsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_returns_seller)

        // Back button
        try { findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() } } catch (_: Exception) {}

        // Show coming soon empty state
        try {
            val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)
            layoutEmpty?.visibility = View.VISIBLE
        } catch (_: Exception) {}

        Toast.makeText(this, "Returns Management coming soon", Toast.LENGTH_SHORT).show()
    }
}
