package com.anga9.seller.ui.support

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.repository.SupportRepository
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

/**
 * Article detail screen with Markwon rendering.
 * "Was this helpful?" feedback → POST /articles/:slug/feedback
 */
class HelpArticleDetailActivity : BaseActivity() {

    companion object {
        const val EXTRA_SLUG  = "extra_slug"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var repository: SupportRepository
    private lateinit var markwon: Markwon

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvBody: TextView
    private lateinit var layoutFeedback: View
    private lateinit var btnHelpful: Button
    private lateinit var btnNotHelpful: Button
    private lateinit var tvFeedbackThanks: TextView

    private var slug: String = ""
    private var feedbackSubmitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_article_detail)

        repository = SupportRepository(this)
        markwon    = Markwon.create(this)
        slug       = intent.getStringExtra(EXTRA_SLUG) ?: ""
        val title  = intent.getStringExtra(EXTRA_TITLE) ?: "Article"

        initViews()
        tvTitle.text = title

        if (slug.isEmpty()) {
            showError("Invalid article")
            return
        }
        loadArticle()
    }

    private fun initViews() {
        btnBack         = findViewById(R.id.btnBack)
        tvTitle         = findViewById(R.id.tvTitle)
        progressBar     = findViewById(R.id.progressBar)
        tvError         = findViewById(R.id.tvError)
        tvBody          = findViewById(R.id.tvBody)
        layoutFeedback  = findViewById(R.id.layoutFeedback)
        btnHelpful      = findViewById(R.id.btnHelpful)
        btnNotHelpful   = findViewById(R.id.btnNotHelpful)
        tvFeedbackThanks = findViewById(R.id.tvFeedbackThanks)

        btnBack.setOnClickListener { finish() }

        btnHelpful.setOnClickListener    { submitFeedback(true) }
        btnNotHelpful.setOnClickListener { submitFeedback(false) }
    }

    private fun loadArticle() {
        progressBar.visibility = View.VISIBLE
        tvBody.visibility = View.GONE
        lifecycleScope.launch {
            val result = repository.getArticleBySlug(slug)
            progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { article ->
                    tvTitle.text = article.title
                    markwon.setMarkdown(tvBody, article.bodyMd ?: "")
                    tvBody.visibility = View.VISIBLE
                    layoutFeedback.visibility = View.VISIBLE
                },
                onFailure = { showError("Could not load article") }
            )
        }
    }

    private fun submitFeedback(helpful: Boolean) {
        if (feedbackSubmitted) return
        feedbackSubmitted = true
        btnHelpful.isEnabled    = false
        btnNotHelpful.isEnabled = false

        lifecycleScope.launch {
            repository.rateArticle(slug, helpful)
            btnHelpful.visibility     = View.GONE
            btnNotHelpful.visibility  = View.GONE
            tvFeedbackThanks.visibility = View.VISIBLE
        }
    }

    private fun showError(msg: String) {
        progressBar.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = msg
    }
}
