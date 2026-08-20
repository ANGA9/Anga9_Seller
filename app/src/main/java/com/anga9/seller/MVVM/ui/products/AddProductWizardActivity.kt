package com.anga9.seller.MVVM.ui.products

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.anga9.seller.R
import com.anga9.seller.MVVM.ui.products.wizard.*
import com.anga9.seller.utils.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddProductWizardActivity : AppCompatActivity() {

    private val viewModel: AddProductWizardViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: Button
    private lateinit var btnNext: Button
    private lateinit var tvWizardSubtitle: TextView
    private lateinit var ivBackHeader: ImageView

    private val fragments: List<Fragment> = listOf(
        Step1MediaFragment(),
        Step2BasicDetailsFragment(),
        Step3PricingFragment(),
        Step4LogisticsFragment(),
        Step5PoliciesFragment()
    )

    private val stepTitles = listOf(
        "Media & Imagery",
        "Basic Details",
        "Pricing & Inventory",
        "Logistics & Compliance",
        "Policies & Search"
    )

    private val progressViews by lazy {
        listOf(
            findViewById<View>(R.id.progressStep1),
            findViewById<View>(R.id.progressStep2),
            findViewById<View>(R.id.progressStep3),
            findViewById<View>(R.id.progressStep4),
            findViewById<View>(R.id.progressStep5)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product_wizard)

        val root = findViewById<View>(R.id.wizardRoot)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnFooterBack)
        btnNext = findViewById(R.id.btnFooterNext)
        tvWizardSubtitle = findViewById(R.id.tvWizardSubtitle)
        ivBackHeader = findViewById(R.id.btnWizardBack)
        
        findViewById<TextView>(R.id.tvSaveDraft).setOnClickListener {
            Toast.makeText(this, "Draft saved locally", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        ivBackHeader.setOnClickListener {
            if (viewPager.currentItem > 0) {
                navigate(-1)
            } else {
                finish()
            }
        }

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
        viewPager.isUserInputEnabled = false // Disable swipe

        btnBack.setOnClickListener { navigate(-1) }
        btnNext.setOnClickListener {
            val currentFragment = fragments[viewPager.currentItem]
            if (currentFragment is WizardStep) {
                if (currentFragment.validate()) {
                    currentFragment.saveDataToViewModel(viewModel)
                    if (viewPager.currentItem < fragments.size - 1) {
                        navigate(1)
                    } else {
                        viewModel.submitProduct()
                    }
                }
            }
        }

        observeViewModel()
        updateUIForStep(0)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.createState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        btnNext.isEnabled = false
                        btnNext.text = "Submitting..."
                    }
                    is UiState.Success -> {
                        btnNext.isEnabled = true
                        Toast.makeText(this@AddProductWizardActivity, "Product submitted for review!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is UiState.Error -> {
                        btnNext.isEnabled = true
                        btnNext.text = "Submit for Review"
                        Toast.makeText(this@AddProductWizardActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigate(direction: Int) {
        val newPos = viewPager.currentItem + direction
        if (newPos in fragments.indices) {
            viewPager.setCurrentItem(newPos, true)
            updateUIForStep(newPos)
        }
    }

    private fun updateUIForStep(position: Int) {
        tvWizardSubtitle.text = "Step ${position + 1} of 5 · ${stepTitles[position]}"
        
        if (position == 0) {
            btnBack.isEnabled = false
        } else {
            btnBack.isEnabled = true
        }

        if (position == fragments.size - 1) {
            btnNext.text = "Submit for Review"
            btnNext.setBackgroundColor(resources.getColor(R.color.wizard_success_text, theme))
        } else {
            btnNext.text = "Next: ${stepTitles[position + 1]}"
            btnNext.setBackgroundColor(resources.getColor(R.color.primary, theme))
        }

        progressViews.forEachIndexed { index, view ->
            if (index <= position) {
                view.alpha = 1.0f
            } else {
                view.alpha = 0.3f
            }
        }
    }
}

interface WizardStep {
    fun validate(): Boolean
    fun saveDataToViewModel(viewModel: AddProductWizardViewModel)
}
