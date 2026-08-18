package com.anga9.seller.ui.analytics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.utils.TokenManager
import com.anga9.seller.data.model.AnalyticsPeriod
import com.anga9.seller.utils.Resource

class AnalyticsActivity : BaseActivity() {

    private val vm: AnalyticsViewModel by viewModels()

    private lateinit var chipWeek: TextView
    private lateinit var chipMonth: TextView
    private lateinit var chipQuarter: TextView
    private lateinit var chipYear: TextView

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvAvgOrderValue: TextView
    private lateinit var tvNetEarnings: TextView
    private lateinit var tvFulfillmentRate: TextView
    private lateinit var tvReturnRate: TextView
    private lateinit var tvActiveProducts: TextView
    private lateinit var tvPendingEarnings: TextView

    private lateinit var chartContainer: LinearLayout
    private lateinit var tvChartEmpty: TextView

    private lateinit var rvTopProducts: RecyclerView
    private lateinit var topProductAdapter: TopProductAdapter

    private lateinit var rvCategoryRevenue: RecyclerView
    private lateinit var categoryAdapter: CategoryRevenueAdapter

    private lateinit var tvGst5: TextView
    private lateinit var tvGst12: TextView
    private lateinit var tvGst18: TextView
    private lateinit var tvGstTotal: TextView
    private lateinit var tvTaxableAmount: TextView
    private lateinit var tvPlatformFees: TextView

    private lateinit var btnExportCsv: Button
    private lateinit var btnExportPdf: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    private var exportAsPdf = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        initViews()
        setupAdapters()
        setupPeriodChips()
        setupExportButtons()
        observeViewModel()
        vm.loadAll(AnalyticsPeriod.MONTH)
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        chipWeek    = findViewById(R.id.chipWeek)
        chipMonth   = findViewById(R.id.chipMonth)
        chipQuarter = findViewById(R.id.chipQuarter)
        chipYear    = findViewById(R.id.chipYear)

        tvTotalRevenue    = findViewById(R.id.tvTotalRevenue)
        tvTotalOrders     = findViewById(R.id.tvTotalOrders)
        tvAvgOrderValue   = findViewById(R.id.tvAvgOrderValue)
        tvNetEarnings     = findViewById(R.id.tvNetEarnings)
        tvFulfillmentRate = findViewById(R.id.tvFulfillmentRate)
        tvReturnRate      = findViewById(R.id.tvReturnRate)
        tvActiveProducts  = findViewById(R.id.tvActiveProducts)
        tvPendingEarnings = findViewById(R.id.tvPendingEarnings)

        chartContainer = findViewById(R.id.chartContainer)
        tvChartEmpty   = findViewById(R.id.tvChartEmpty)

        tvGst5          = findViewById(R.id.tvGst5)
        tvGst12         = findViewById(R.id.tvGst12)
        tvGst18         = findViewById(R.id.tvGst18)
        tvGstTotal      = findViewById(R.id.tvGstTotal)
        tvTaxableAmount = findViewById(R.id.tvTaxableAmount)
        tvPlatformFees  = findViewById(R.id.tvPlatformFees)

        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        progressBar  = findViewById(R.id.progressBar)
        scrollView   = findViewById(R.id.scrollView)
    }

    private fun setupAdapters() {
        topProductAdapter = TopProductAdapter()
        rvTopProducts = findViewById(R.id.rvTopProducts)
        rvTopProducts.layoutManager = LinearLayoutManager(this)
        rvTopProducts.isNestedScrollingEnabled = false
        rvTopProducts.adapter = topProductAdapter

        categoryAdapter = CategoryRevenueAdapter()
        rvCategoryRevenue = findViewById(R.id.rvCategoryRevenue)
        rvCategoryRevenue.layoutManager = LinearLayoutManager(this)
        rvCategoryRevenue.isNestedScrollingEnabled = false
        rvCategoryRevenue.adapter = categoryAdapter
    }

    private fun setupPeriodChips() {
        val chips = listOf(chipWeek, chipMonth, chipQuarter, chipYear)
        val periods = listOf(
            AnalyticsPeriod.WEEK, AnalyticsPeriod.MONTH,
            AnalyticsPeriod.QUARTER, AnalyticsPeriod.YEAR
        )
        chips.forEachIndexed { i, chip ->
            chip.setOnClickListener {
                chips.forEach { c -> setChipSelected(c, false) }
                setChipSelected(chip, true)
                vm.loadAll(periods[i])
            }
        }
        setChipSelected(chipMonth, true)
    }

    private fun setChipSelected(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.btn_primary_rounded)
            chip.setTextColor(Color.WHITE)
        } else {
            chip.setBackgroundResource(R.drawable.chip_unselected)
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun setupExportButtons() {
        btnExportCsv.setOnClickListener {
            exportAsPdf = false
            vm.loadExportData()
            Toast.makeText(this, "Preparing CSV export...", Toast.LENGTH_SHORT).show()
        }
        btnExportPdf.setOnClickListener {
            exportAsPdf = true
            vm.loadExportData()
            Toast.makeText(this, "Preparing PDF export...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        vm.summary.observe(this) { res ->
            when (res) {
                is Resource.Loading<*> -> showLoading(true)
                is Resource.Error      -> { showLoading(false); showToast(res.message ?: "Error") }
                is Resource.Success    -> {
                    showLoading(false)
                    val s = res.data ?: return@observe
                    tvTotalRevenue.text    = "\u20b9${String.format("%,.0f", s.totalRevenue)}"
                    tvTotalOrders.text     = s.totalOrders.toString()
                    tvAvgOrderValue.text   = "\u20b9${String.format("%,.0f", s.avgOrderValue)}"
                    tvNetEarnings.text     = "\u20b9${String.format("%,.0f", s.netEarnings)}"
                    tvFulfillmentRate.text = "${String.format("%.1f", s.fulfillmentRate)}%"
                    tvReturnRate.text      = "${String.format("%.1f", s.returnRate)}%"
                    tvActiveProducts.text  = s.activeProducts.toString()
                    tvPendingEarnings.text = "\u20b9${String.format("%,.0f", s.pendingEarnings)}"
                    tvPlatformFees.text    = "\u20b9${String.format("%,.0f", s.platformFeesTotal)}"
                }
            }
        }

        vm.chartData.observe(this) { res ->
            if (res is Resource.Success) {
                val data = res.data ?: return@observe
                if (data.isEmpty()) {
                    tvChartEmpty.visibility   = View.VISIBLE
                    chartContainer.visibility = View.GONE
                } else {
                    tvChartEmpty.visibility   = View.GONE
                    chartContainer.visibility = View.VISIBLE
                    renderBarChart(data.map { it.label to it.amount })
                }
            }
        }

        vm.topProducts.observe(this) { res ->
            if (res is Resource.Success) topProductAdapter.submitList(res.data)
        }

        vm.categoryRevenue.observe(this) { res ->
            if (res is Resource.Success) categoryAdapter.submitList(res.data)
        }

        vm.gstBreakdown.observe(this) { res ->
            if (res is Resource.Success) {
                val g = res.data ?: return@observe
                tvGst5.text          = "\u20b9${String.format("%,.2f", g.gst5)}"
                tvGst12.text         = "\u20b9${String.format("%,.2f", g.gst12)}"
                tvGst18.text         = "\u20b9${String.format("%,.2f", g.gst18)}"
                tvGstTotal.text      = "\u20b9${String.format("%,.2f", g.totalGst)}"
                tvTaxableAmount.text = "\u20b9${String.format("%,.2f", g.taxableAmount)}"
            }
        }

        vm.exportData.observe(this) { res ->
            when (res) {
                is Resource.Success    -> {
                    val data = res.data ?: return@observe
                    if (exportAsPdf) exportToPdf(data) else exportToCsv(data)
                }
                is Resource.Error      -> showToast("Export failed: ${res.message}")
                else -> {}
            }
        }
    }

    // ─── Bar Chart ────────────────────────────────────────────────────────

    private fun renderBarChart(data: List<Pair<String, Double>>) {
        chartContainer.removeAllViews()
        if (data.isEmpty()) return
        val maxVal = data.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val chartHeight = resources.getDimensionPixelSize(R.dimen.chart_bar_height)

        data.forEach { (label, amount) ->
            val barWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setPadding(4, 0, 4, 0)
            }

            val amtLabel = TextView(this).apply {
                text = if (amount >= 1000) "\u20b9${String.format("%.0fK", amount / 1000)}"
                       else "\u20b9${String.format("%.0f", amount)}"
                textSize = 8f
                setTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
            }

            val barHeight = ((amount / maxVal) * chartHeight).toInt().coerceAtLeast(4)
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.chart_bar_width), barHeight
                ).also { it.topMargin = 4 }
                background = ContextCompat.getDrawable(this@AnalyticsActivity, R.drawable.bar_chart_item)
            }

            val xLabel = TextView(this).apply {
                text = label
                textSize = 9f
                setTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }

            barWrapper.addView(amtLabel)
            barWrapper.addView(bar)
            barWrapper.addView(xLabel)
            chartContainer.addView(barWrapper)
        }
    }

    // ─── Export ───────────────────────────────────────────────────────────

    private fun exportToCsv(rows: List<List<String>>) {
        try {
            val headers = listOf("Date", "Order ID", "Customer", "Total Amount",
                "GST", "Platform Fee", "Net Earnings", "Status")
            val sb = StringBuilder()
            sb.appendLine(headers.joinToString(","))
            rows.forEach { row -> sb.appendLine(row.joinToString(",")) }

            val fileName = "ANGA9_Report_${System.currentTimeMillis()}.csv"
            val dir = getExternalFilesDir(null) ?: cacheDir
            val file = java.io.File(dir, fileName)
            file.writeText(sb.toString())

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ANGA9 Sales Report - ${vm.selectedPeriod.label}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export Report"))
        } catch (e: Exception) {
            showToast("Export failed: ${e.message}")
        }
    }

    private fun exportToPdf(rows: List<List<String>>) {
        try {
            val headers = listOf("Date", "Order ID", "Customer", "Total",
                "GST", "Platform Fee", "Net Earnings", "Status")
            val pdfDoc    = android.graphics.pdf.PdfDocument()
            val pageWidth = 842
            val pageHeight = 595
            val margin    = 30f
            val rowHeight = 22f
            val colWidth  = (pageWidth - margin * 2) / headers.size

            val titlePaint = android.graphics.Paint().apply {
                textSize = 16f; isFakeBoldText = true
                color = android.graphics.Color.parseColor("#1A365D")
            }
            val headerPaint = android.graphics.Paint().apply {
                textSize = 9f; isFakeBoldText = true
                color = android.graphics.Color.WHITE
            }
            val cellPaint = android.graphics.Paint().apply {
                textSize = 8f
                color = android.graphics.Color.parseColor("#1A2332")
            }
            val bgHeaderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1A365D")
                style = android.graphics.Paint.Style.FILL
            }
            val bgAltPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F5F7FA")
                style = android.graphics.Paint.Style.FILL
            }
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E5E7EB")
                strokeWidth = 0.5f
            }

            val rowsPerPage  = ((pageHeight - margin * 2 - rowHeight * 2 - 30f) / rowHeight).toInt()
            val totalPages   = maxOf(1, Math.ceil(rows.size.toDouble() / rowsPerPage).toInt())

            for (pageNum in 0 until totalPages) {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNum + 1).create()
                val page   = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                var y      = margin

                canvas.drawText(
                    "ANGA9 Sales Report - ${vm.selectedPeriod.label}",
                    margin, y + 16f, titlePaint)
                y += 30f

                val subPaint = android.graphics.Paint().apply { textSize = 8f; color = android.graphics.Color.GRAY }
                val dateStr  = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                canvas.drawText("Generated: $dateStr  |  Page ${pageNum + 1} of $totalPages", margin, y, subPaint)
                y += 14f

                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, bgHeaderPaint)
                headers.forEachIndexed { i, h ->
                    canvas.drawText(h, margin + i * colWidth + 3f, y + 14f, headerPaint)
                }
                y += rowHeight

                val startIdx = pageNum * rowsPerPage
                val endIdx   = minOf(startIdx + rowsPerPage, rows.size)
                for (rowIdx in startIdx until endIdx) {
                    val row = rows[rowIdx]
                    if ((rowIdx - startIdx) % 2 == 1)
                        canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, bgAltPaint)
                    row.forEachIndexed { i, cell ->
                        val maxChars    = (colWidth / 5).toInt()
                        val displayText = if (cell.length > maxChars) cell.take(maxChars - 1) + "..." else cell
                        canvas.drawText(displayText, margin + i * colWidth + 3f, y + 14f, cellPaint)
                    }
                    canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, linePaint)
                    y += rowHeight
                }
                pdfDoc.finishPage(page)
            }

            val fileName = "ANGA9_Report_${System.currentTimeMillis()}.pdf"
            val dir      = getExternalFilesDir(null) ?: cacheDir
            val file     = java.io.File(dir, fileName)
            pdfDoc.writeTo(java.io.FileOutputStream(file))
            pdfDoc.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ANGA9 Sales Report - ${vm.selectedPeriod.label}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
        } catch (e: Exception) {
            showToast("PDF export failed: ${e.message}")
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ── Phase 5 (Multi-Brand): Show active brand name in subtitle ──────────
    // When a child brand is active, the action bar subtitle shows which brand
    // context is currently active — prevents confusion between brands.
    private fun showBrandIndicator() {
        val activeBrandId = TokenManager.getActiveBrandId(this)
        if (activeBrandId != null) {
            // Child brand context: show stored brand name or fallback
            val brandName = getSharedPreferences("anga9_seller_prefs", MODE_PRIVATE)
                .getString("brand_name_", null)
            supportActionBar?.subtitle = brandName ?: "Brand Context Active"
        } else {
            supportActionBar?.subtitle = null
        }
    }

    override fun onResume() {
        super.onResume()
        showBrandIndicator()
    }

}