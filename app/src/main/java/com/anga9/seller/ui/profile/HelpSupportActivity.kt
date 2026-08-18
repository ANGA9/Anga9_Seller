package com.anga9.seller.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.data.repository.SupportRepository
import com.anga9.seller.ui.support.SupportArticleAdapter
import com.anga9.seller.ui.support.MyTicketsActivity
import com.anga9.seller.ui.support.CreateTicketActivity
import com.anga9.seller.ui.support.HelpArticlesActivity
import com.anga9.seller.ui.support.HelpArticleDetailActivity
import com.anga9.seller.ui.chatbot.ChatbotActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Help Center Landing Screen — Phase 5 (Seller App)
 *
 * Features:
 * - Search bar → GET /api/support/articles?q=... (debounced 400ms)
 * - Category tiles → HelpArticlesActivity
 * - "My Tickets" → MyTicketsActivity
 * - "Raise a Ticket" → CreateTicketActivity
 * - Popular articles list (top 5)
 * - Contact options (email/phone)
 *
 * Security: isInternal == true messages filtered in TicketDetailActivity
 */
class HelpSupportActivity : BaseActivity() {

    private lateinit var repository: SupportRepository
    private lateinit var articleAdapter: SupportArticleAdapter

    private lateinit var btnBack: ImageView
    private lateinit var etSearch: EditText
    private lateinit var searchProgress: ProgressBar
    private lateinit var btnMyTickets: CardView
    private lateinit var btnRaiseTicket: CardView
    private lateinit var catOrders: CardView
    private lateinit var catReturns: CardView
    private lateinit var catPayments: CardView
    private lateinit var catAccount: CardView
    private lateinit var catOther: CardView
    private lateinit var tvArticlesSectionTitle: TextView
    private lateinit var tvViewAllArticles: TextView
    private lateinit var articlesProgress: ProgressBar
    private lateinit var tvArticlesEmpty: TextView
    private lateinit var rvArticles: RecyclerView
    private lateinit var btnEmailSupport: View
    private lateinit var btnPhoneSupport: View

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support_new)

        repository = SupportRepository(this)
        initViews()
        setupAdapter()
        setupClickListeners()
        loadPopularArticles()
    }

    private fun initViews() {
        btnBack              = findViewById(R.id.btnBack)
        etSearch             = findViewById(R.id.etSearch)
        searchProgress       = findViewById(R.id.searchProgress)
        btnMyTickets         = findViewById(R.id.btnMyTickets)
        btnRaiseTicket       = findViewById(R.id.btnRaiseTicket)
        catOrders            = findViewById(R.id.catOrders)
        catReturns           = findViewById(R.id.catReturns)
        catPayments          = findViewById(R.id.catPayments)
        catAccount           = findViewById(R.id.catAccount)
        catOther             = findViewById(R.id.catOther)
        tvArticlesSectionTitle = findViewById(R.id.tvArticlesSectionTitle)
        tvViewAllArticles    = findViewById(R.id.tvViewAllArticles)
        articlesProgress     = findViewById(R.id.articlesProgress)
        tvArticlesEmpty      = findViewById(R.id.tvArticlesEmpty)
        rvArticles           = findViewById(R.id.rvArticles)
        btnEmailSupport      = findViewById(R.id.btnEmailSupport)
        btnPhoneSupport      = findViewById(R.id.btnPhoneSupport)
    }

    private fun setupAdapter() {
        articleAdapter = SupportArticleAdapter { article ->
            openArticle(article.slug, article.title)
        }
        rvArticles.layoutManager = LinearLayoutManager(this)
        rvArticles.adapter = articleAdapter
        rvArticles.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnMyTickets.setOnClickListener {
            startActivity(Intent(this, MyTicketsActivity::class.java))
        }

        btnRaiseTicket.setOnClickListener {
            startActivity(Intent(this, CreateTicketActivity::class.java))
        }

        // Chat with Anga — Phase 2 Chatbot
        val btnChatWithAnga = findViewById<View?>(R.id.btnChatWithAnga)
        btnChatWithAnga?.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        catOrders.setOnClickListener   { openArticlesByCategory("orders",   "Orders") }
        catReturns.setOnClickListener  { openArticlesByCategory("returns",  "Returns") }
        catPayments.setOnClickListener { openArticlesByCategory("payments", "Payments") }
        catAccount.setOnClickListener  { openArticlesByCategory("account",  "Account") }
        catOther.setOnClickListener    { openArticlesByCategory("other",    "Other") }

        tvViewAllArticles.setOnClickListener {
            startActivity(Intent(this, HelpArticlesActivity::class.java))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.isEmpty()) {
                    tvArticlesSectionTitle.text = "Popular Articles"
                    loadPopularArticles()
                } else {
                    searchJob = lifecycleScope.launch {
                        delay(400)
                        searchArticles(query)
                    }
                }
            }
        })

        btnEmailSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:seller-support@anga9.com")
                putExtra(Intent.EXTRA_SUBJECT, "Seller Support Request")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        }

        btnPhoneSupport.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917003724388")))
        }
    }

    private fun loadPopularArticles() {
        tvArticlesSectionTitle.text = "Popular Articles"
        showArticlesLoading(true)
        lifecycleScope.launch {
            val result = repository.getArticles(page = 1, limit = 5)
            showArticlesLoading(false)
            result.fold(
                onSuccess = { response ->
                    if (response.articles.isEmpty()) {
                        showArticlesEmpty(true)
                    } else {
                        showArticlesEmpty(false)
                        articleAdapter.submitList(response.articles)
                    }
                },
                onFailure = {
                    showArticlesEmpty(true)
                    tvArticlesEmpty.text = "Could not load articles"
                }
            )
        }
    }

    private fun searchArticles(query: String) {
        tvArticlesSectionTitle.text = "Search Results"
        showArticlesLoading(true)
        lifecycleScope.launch {
            val result = repository.getArticles(query = query, page = 1, limit = 10)
            showArticlesLoading(false)
            result.fold(
                onSuccess = { response ->
                    if (response.articles.isEmpty()) {
                        showArticlesEmpty(true)
                        tvArticlesEmpty.text = "No articles found for \"$query\""
                    } else {
                        showArticlesEmpty(false)
                        articleAdapter.submitList(response.articles)
                    }
                },
                onFailure = {
                    showArticlesEmpty(true)
                    tvArticlesEmpty.text = "Search failed. Please try again."
                }
            )
        }
    }

    private fun openArticlesByCategory(category: String, displayName: String) {
        val intent = Intent(this, HelpArticlesActivity::class.java).apply {
            putExtra(HelpArticlesActivity.EXTRA_CATEGORY, category)
            putExtra(HelpArticlesActivity.EXTRA_TITLE, displayName)
        }
        startActivity(intent)
    }

    private fun openArticle(slug: String, title: String) {
        val intent = Intent(this, HelpArticleDetailActivity::class.java).apply {
            putExtra(HelpArticleDetailActivity.EXTRA_SLUG, slug)
            putExtra(HelpArticleDetailActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
    }

    private fun showArticlesLoading(loading: Boolean) {
        articlesProgress.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            rvArticles.visibility = View.GONE
            tvArticlesEmpty.visibility = View.GONE
        }
    }

    private fun showArticlesEmpty(empty: Boolean) {
        tvArticlesEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        rvArticles.visibility = if (empty) View.GONE else View.VISIBLE
    }
}

