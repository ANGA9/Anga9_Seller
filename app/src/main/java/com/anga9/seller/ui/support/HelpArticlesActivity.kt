package com.anga9.seller.ui.support

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.repository.SupportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Article list screen filtered by category.
 * Launched from HelpSupportActivity category tiles or "View All".
 */
class HelpArticlesActivity : BaseActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TITLE    = "extra_title"
    }

    private lateinit var repository: SupportRepository
    private lateinit var adapter: SupportArticleAdapter

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvArticles: RecyclerView

    private var category: String? = null
    private var currentPage = 1
    private var isLoading = false
    private var hasMore = true
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_articles)

        repository = SupportRepository(this)
        category   = intent.getStringExtra(EXTRA_CATEGORY)
        val title  = intent.getStringExtra(EXTRA_TITLE) ?: "Articles"

        initViews()
        tvTitle.text = title
        setupAdapter()
        setupSearch()
        loadArticles(reset = true)
    }

    private fun initViews() {
        btnBack     = findViewById(R.id.btnBack)
        tvTitle     = findViewById(R.id.tvTitle)
        etSearch    = findViewById(R.id.etSearch)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty     = findViewById(R.id.tvEmpty)
        rvArticles  = findViewById(R.id.rvArticles)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupAdapter() {
        adapter = SupportArticleAdapter { article ->
            val intent = Intent(this, HelpArticleDetailActivity::class.java).apply {
                putExtra(HelpArticleDetailActivity.EXTRA_SLUG,  article.slug)
                putExtra(HelpArticleDetailActivity.EXTRA_TITLE, article.title)
            }
            startActivity(intent)
        }
        rvArticles.layoutManager = LinearLayoutManager(this)
        rvArticles.adapter = adapter

        // Infinite scroll
        rvArticles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                if (!isLoading && hasMore &&
                    lm.findLastVisibleItemPosition() >= lm.itemCount - 3) {
                    loadArticles(reset = false)
                }
            }
        })
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    loadArticles(reset = true, query = s?.toString()?.trim())
                }
            }
        })
    }

    private fun loadArticles(reset: Boolean, query: String? = null) {
        if (isLoading) return
        if (reset) {
            currentPage = 1
            hasMore = true
            adapter.submitList(emptyList())
        }
        isLoading = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.getArticles(
                query    = query,
                category = category,
                page     = currentPage,
                limit    = 15
            )
            isLoading = false
            progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { response ->
                    val current = adapter.currentList.toMutableList()
                    current.addAll(response.articles)
                    adapter.submitList(current)
                    hasMore = response.articles.size >= 15
                    currentPage++
                    tvEmpty.visibility = if (current.isEmpty()) View.VISIBLE else View.GONE
                    rvArticles.visibility = if (current.isEmpty()) View.GONE else View.VISIBLE
                },
                onFailure = {
                    if (adapter.currentList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Could not load articles"
                    }
                }
            )
        }
    }
}
