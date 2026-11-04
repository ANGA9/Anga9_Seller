package com.anga9.seller.MVVM.ui.products

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anga9.seller.R
import com.anga9.seller.ui.dashboard.DashboardActivity

class MyProductsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                putExtra(DashboardActivity.EXTRA_NAV_TAB, R.id.nav_products)
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
