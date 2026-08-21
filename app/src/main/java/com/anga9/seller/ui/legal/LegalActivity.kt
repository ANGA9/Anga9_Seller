package com.anga9.seller.ui.legal

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.model.LegalDataSource
import com.anga9.seller.data.model.PrivacyBlock
import com.anga9.seller.data.model.PrivacyLangContent
import com.anga9.seller.data.model.TermsLangContent
import com.anga9.seller.data.repository.LegalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LegalActivity - Phase 2 (Seller App)
 * Single activity for Privacy Policy + Terms of Service.
 * Usage: LegalActivity.startPrivacy(context) / startTerms(context)
 */
class LegalActivity : BaseActivity() {

    companion object {
        const val EXTRA_TYPE = "legal_type"
        const val TYPE_PRIVACY = "privacy"
        const val TYPE_TERMS   = "terms"

        fun startPrivacy(context: Context) {
            context.startActivity(
                Intent(context, LegalActivity::class.java).putExtra(EXTRA_TYPE, TYPE_PRIVACY)
            )
        }

        fun startTerms(context: Context) {
            context.startActivity(
                Intent(context, LegalActivity::class.java).putExtra(EXTRA_TYPE, TYPE_TERMS)
            )
        }
    }

    private lateinit var tvTitle: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var containerContent: LinearLayout
    private lateinit var spinnerLang: Spinner
    private lateinit var layoutError: View
    private lateinit var tvError: TextView
    private lateinit var btnRetry: View

    private lateinit var repository: LegalRepository
    private var currentType: String = TYPE_PRIVACY
    private var currentLang: String = "en"
    private var isSpinnerInitialized = false

    private val langCodes get() = LegalRepository.SUPPORTED_LANGS
    private val langNames  get() = LegalRepository.LANG_DISPLAY_NAMES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)

        repository  = LegalRepository(this)
        currentType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_PRIVACY
        currentLang = repository.getDeviceLang()

        bindViews()
        setupBackButton()
        setupLangSpinner()
        loadContent(currentType, currentLang)
    }

    private fun bindViews() {
        tvTitle          = findViewById(R.id.tvTitle)
        tvLastUpdated    = findViewById(R.id.tvLastUpdated)
        progressBar      = findViewById(R.id.progressBar)
        containerContent = findViewById(R.id.containerContent)
        spinnerLang      = findViewById(R.id.spinnerLang)
        layoutError      = findViewById(R.id.layoutError)
        tvError          = findViewById(R.id.tvError)
        btnRetry         = findViewById(R.id.btnRetry)
        btnRetry.setOnClickListener { loadContent(currentType, currentLang) }
    }

    private fun setupBackButton() {
        findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupLangSpinner() {
        val adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, langNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.textSize = 13f
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLang.adapter = adapter

        val defaultIdx = langCodes.indexOf(currentLang).coerceAtLeast(0)
        spinnerLang.setSelection(defaultIdx, false)

        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) { isSpinnerInitialized = true; return }
                val selected = langCodes[position]
                if (selected != currentLang) {
                    currentLang = selected
                    loadContent(currentType, currentLang)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        isSpinnerInitialized = false
    }

    private fun loadContent(type: String, lang: String) {
        showLoading()
        lifecycleScope.launch {
            if (type == TYPE_PRIVACY) {
                val result = withContext(Dispatchers.IO) { repository.getPrivacyContent(lang) }
                if (result.data != null) {
                    renderPrivacy(result.data)
                    showContent()
                    if (result.source == LegalDataSource.ASSETS) {
                        Toast.makeText(this@LegalActivity, "Showing offline content", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(result.error ?: "Could not load Privacy Policy")
                }
            } else {
                val result = withContext(Dispatchers.IO) { repository.getTermsContent(lang) }
                if (result.data != null) {
                    renderTerms(result.data)
                    showContent()
                    if (result.source == LegalDataSource.ASSETS) {
                        Toast.makeText(this@LegalActivity, "Showing offline content", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(result.error ?: "Could not load Terms of Service")
                }
            }
        }
    }

    private fun renderPrivacy(content: PrivacyLangContent) {
        containerContent.removeAllViews()
        tvTitle.text = content.meta.title.ifEmpty { "Privacy Policy" }
        tvLastUpdated.text = if (!content.meta.lastUpdated.isNullOrEmpty())
            "Last updated: ${content.meta.lastUpdated}" else ""
        content.content.forEach { (_, blocks) ->
            blocks.forEach { block -> renderBlock(block) }
            addSpacing(8)
        }
    }

    private fun renderTerms(content: TermsLangContent) {
        containerContent.removeAllViews()
        tvTitle.text = content.meta.title.ifEmpty { "Terms of Service" }
        tvLastUpdated.text = if (!content.meta.lastUpdated.isNullOrEmpty())
            "Last updated: ${content.meta.lastUpdated}" else ""
        content.sections.forEach { (_, section) ->
            if (section.heading.isNotEmpty()) addHeading2(section.heading)
            section.paragraphs.forEach { para -> if (para.isNotEmpty()) addParagraph(para) }
            section.listIntro?.let { if (it.isNotEmpty()) addParagraph(it) }
            section.list.forEach { item -> if (item.isNotEmpty()) addBulletItem(item) }
            section.blocks?.forEach { block -> renderBlock(block) }
            addSpacing(12)
        }
    }

    private fun renderBlock(block: PrivacyBlock) {
        when (block.kind) {
            "h2" -> block.text?.let { addHeading2(it) }
            "h3" -> block.text?.let { addHeading3(it) }
            "p"  -> block.text?.let { addParagraph(it) }
            "ul" -> block.items?.forEach { addBulletItem(it) }
            else -> block.text?.let { addParagraph(it) }
        }
    }

    private fun addHeading2(text: String) {
        containerContent.addView(TextView(this).apply {
            this.text = text; textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF1A2332.toInt())
            setPadding(0, dpToPx(18), 0, dpToPx(6))
        })
    }

    private fun addHeading3(text: String) {
        containerContent.addView(TextView(this).apply {
            this.text = text; textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF1A2332.toInt())
            setPadding(0, dpToPx(14), 0, dpToPx(4))
        })
    }

    private fun addParagraph(text: String) {
        containerContent.addView(TextView(this).apply {
            this.text = buildSpannableWithLinks(text)
            textSize = 14f
            setLineSpacing(0f, 1.4f)
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(3), 0, dpToPx(3))
            movementMethod = LinkMovementMethod.getInstance()
        })
    }

    private fun addBulletItem(text: String) {
        containerContent.addView(TextView(this).apply {
            this.text = buildSpannableWithLinks("\u2022  $text")
            textSize = 14f
            setLineSpacing(0f, 1.35f)
            setTextColor(0xFF6B7280.toInt())
            setPadding(dpToPx(12), dpToPx(2), 0, dpToPx(2))
            movementMethod = LinkMovementMethod.getInstance()
        })
    }

    private fun addSpacing(dp: Int) {
        containerContent.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dp))
        })
    }

    private fun buildSpannableWithLinks(text: String): SpannableString {
        val linkPatterns = listOf(
            "mailto:support@anga9.com",
            "/shipping-policy", "/returns", "/cancellation", "/contact"
        )
        val spannable = SpannableString(text)
        val linkColor = 0xFF2563EB.toInt()
        linkPatterns.forEach { pattern ->
            var startIndex = 0
            while (true) {
                val idx = text.indexOf(pattern, startIndex)
                if (idx == -1) break
                val end = idx + pattern.length
                val clickUrl = pattern
                val clickSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) { handleLegalLink(clickUrl) }
                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = linkColor
                        ds.isUnderlineText = true
                    }
                }
                spannable.setSpan(clickSpan, idx, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(linkColor), idx, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                startIndex = end
            }
        }
        return spannable
    }

    private fun handleLegalLink(url: String) {
        try {
            when {
                url.startsWith("mailto:") -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse(url) }
                    if (intent.resolveActivity(packageManager) != null) startActivity(intent)
                    else Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
                }
                url.startsWith("http://") || url.startsWith("https://") -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                url.startsWith("/") -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://anga9.com$url")))
                }
                else -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        progressBar.visibility      = View.VISIBLE
        containerContent.visibility = View.GONE
        layoutError.visibility      = View.GONE
    }

    private fun showContent() {
        progressBar.visibility      = View.GONE
        containerContent.visibility = View.VISIBLE
        layoutError.visibility      = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility      = View.GONE
        containerContent.visibility = View.GONE
        layoutError.visibility      = View.VISIBLE
        tvError.text                = message
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
