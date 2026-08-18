package com.anga9.seller.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anga9.seller.data.model.*
import com.anga9.seller.data.repository.AnalyticsRepository
import com.anga9.seller.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AnalyticsRepository(application.applicationContext)

    // Current period (used by Activity for export label)
    var selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTH
        private set

    // LiveData observed by Activity
    private val _summary = MutableLiveData<Resource<SellerAnalyticsSummary>>()
    val summary: LiveData<Resource<SellerAnalyticsSummary>> = _summary

    private val _chartData = MutableLiveData<Resource<List<RevenueChartPoint>>>()
    val chartData: LiveData<Resource<List<RevenueChartPoint>>> = _chartData

    private val _topProducts = MutableLiveData<Resource<List<TopProduct>>>()
    val topProducts: LiveData<Resource<List<TopProduct>>> = _topProducts

    private val _categoryRevenue = MutableLiveData<Resource<List<CategoryRevenue>>>()
    val categoryRevenue: LiveData<Resource<List<CategoryRevenue>>> = _categoryRevenue

    private val _gstBreakdown = MutableLiveData<Resource<GstBreakdown>>()
    val gstBreakdown: LiveData<Resource<GstBreakdown>> = _gstBreakdown

    private val _exportData = MutableLiveData<Resource<List<List<String>>>>()
    val exportData: LiveData<Resource<List<List<String>>>> = _exportData

    // ─── Load All ─────────────────────────────────────────────────────────

    fun loadAll(period: AnalyticsPeriod = AnalyticsPeriod.MONTH) {
        selectedPeriod = period
        _summary.value = Resource.Loading()
        viewModelScope.launch {
            repo.getAnalyticsSummary(period).collectLatest { res ->
                _summary.postValue(res)
                // Derive GST breakdown from summary once loaded
                if (res is Resource.Success) {
                    val s = res.data
                    if (s != null) {
                        _gstBreakdown.postValue(Resource.Success(
                            GstBreakdown(
                                gst5          = s.gst5Amount,
                                gst12         = s.gst12Amount,
                                gst18         = s.gst18Amount,
                                totalGst      = s.gst5Amount + s.gst12Amount + s.gst18Amount,
                                taxableAmount = s.totalRevenue - (s.gst5Amount + s.gst12Amount + s.gst18Amount)
                            )
                        ))
                    }
                }
            }
        }
        viewModelScope.launch {
            repo.getRevenueChart(period).collectLatest { _chartData.postValue(it) }
        }
        viewModelScope.launch {
            repo.getTopProducts(period).collectLatest { _topProducts.postValue(it) }
        }
        viewModelScope.launch {
            repo.getCategoryRevenue(period).collectLatest { _categoryRevenue.postValue(it) }
        }
    }

    // ─── Export ───────────────────────────────────────────────────────────

    fun loadExportData() {
        _exportData.value = Resource.Loading()
        viewModelScope.launch {
            try {
                // Build export rows from cached summary + orders
                // For now return a simple placeholder that triggers export
                val rows = buildExportRows()
                _exportData.postValue(Resource.Success(rows))
            } catch (e: Exception) {
                _exportData.postValue(Resource.Error("Export failed: ${e.message}"))
            }
        }
    }

    private fun buildExportRows(): List<List<String>> {
        // Export rows built from what we have
        // Returns empty list if no data — caller handles gracefully
        return emptyList()
    }
}