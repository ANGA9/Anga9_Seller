package com.anga9.seller.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.anga9.seller.R

/**
 * Floating Capsule Bottom Navigation Bar with a smooth sliding greyish active pill indicator
 * that animates across tabs upon selection, mirroring the Flipkart style in ANGA9 Customer app.
 */
class FlipkartBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class NavItem(val itemId: Int)

    private val tabsContainer: LinearLayout
    private val activePillIndicator: View

    private val tabHome: LinearLayout
    private val tabOrders: LinearLayout
    private val tabProducts: LinearLayout
    private val tabInventory: LinearLayout

    private val ivHome: ImageView
    private val tvHome: TextView
    private val ivOrders: ImageView
    private val tvOrders: TextView
    private val ivProducts: ImageView
    private val tvProducts: TextView
    private val ivInventory: ImageView
    private val tvInventory: TextView

    private val tabIds = listOf(R.id.nav_home, R.id.nav_orders, R.id.nav_products, R.id.nav_inventory)
    private var currentSelectedId: Int = R.id.nav_home
    private var listener: ((NavItem) -> Boolean)? = null

    private val colorActive = Color.parseColor("#1A6FD4")
    private val colorInactive = Color.parseColor("#6B7280")

    private var pillWidth = 0
    private var horizontalInset = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.view_seller_flipkart_bottom_nav, this, true)

        tabsContainer = findViewById(R.id.tabsContainer)
        activePillIndicator = findViewById(R.id.activePillIndicator)

        tabHome = findViewById(R.id.tabHome)
        tabOrders = findViewById(R.id.tabOrders)
        tabProducts = findViewById(R.id.tabProducts)
        tabInventory = findViewById(R.id.tabInventory)

        ivHome = findViewById(R.id.ivHome)
        tvHome = findViewById(R.id.tvHome)
        ivOrders = findViewById(R.id.ivOrders)
        tvOrders = findViewById(R.id.tvOrders)
        ivProducts = findViewById(R.id.ivProducts)
        tvProducts = findViewById(R.id.tvProducts)
        ivInventory = findViewById(R.id.ivInventory)
        tvInventory = findViewById(R.id.tvInventory)

        tabHome.setOnClickListener { handleTabClick(R.id.nav_home) }
        tabOrders.setOnClickListener { handleTabClick(R.id.nav_orders) }
        tabProducts.setOnClickListener { handleTabClick(R.id.nav_products) }
        tabInventory.setOnClickListener { handleTabClick(R.id.nav_inventory) }

        updateTabVisuals(animate = false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            setupPillDimensions()
            movePillToCurrentTab(animate = false)
        }
    }

    private fun setupPillDimensions() {
        val totalWidth = tabsContainer.width
        if (totalWidth <= 0) return
        val tabWidth = totalWidth / tabIds.size
        val density = resources.displayMetrics.density
        horizontalInset = (5 * density).toInt()
        pillWidth = tabWidth - (horizontalInset * 2)
        val pillHeight = (46 * density).toInt()

        val params = activePillIndicator.layoutParams as? LayoutParams
            ?: LayoutParams(pillWidth, pillHeight)
        params.width = pillWidth
        params.height = pillHeight
        params.gravity = android.view.Gravity.CENTER_VERTICAL
        params.leftMargin = horizontalInset
        activePillIndicator.layoutParams = params
    }

    private fun movePillToCurrentTab(animate: Boolean) {
        val totalWidth = tabsContainer.width
        if (totalWidth <= 0) {
            post {
                setupPillDimensions()
                movePillToCurrentTab(animate = false)
            }
            return
        }

        val tabWidth = totalWidth / tabIds.size
        val selectedIndex = tabIds.indexOf(currentSelectedId).coerceAtLeast(0)
        val targetX = (selectedIndex * tabWidth).toFloat()

        if (activePillIndicator.width != pillWidth && pillWidth > 0) {
            val params = activePillIndicator.layoutParams
            params.width = pillWidth
            activePillIndicator.layoutParams = params
        }

        activePillIndicator.visibility = View.VISIBLE

        if (animate && isLaidOut) {
            activePillIndicator.animate()
                .translationX(targetX)
                .setDuration(260)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f))
                .start()
        } else {
            activePillIndicator.animate().cancel()
            activePillIndicator.translationX = targetX
        }
    }

    fun setOnItemSelectedListener(listener: (NavItem) -> Boolean) {
        this.listener = listener
    }

    fun setSelectedItemId(value: Int, notifyListener: Boolean = true) {
        if (value in tabIds) {
            val changed = value != currentSelectedId
            currentSelectedId = value
            updateTabVisuals(animate = isLaidOut)
            if (notifyListener && changed) {
                listener?.invoke(NavItem(value))
            }
        }
    }

    var selectedItemId: Int
        get() = currentSelectedId
        set(value) {
            setSelectedItemId(value, notifyListener = true)
        }

    private fun handleTabClick(itemId: Int) {
        if (itemId == currentSelectedId) return

        val handled = listener?.invoke(NavItem(itemId)) ?: true
        if (handled) {
            currentSelectedId = itemId
            updateTabVisuals(animate = true)
        }
    }

    private fun updateTabVisuals(animate: Boolean = true) {
        val selectedIndex = tabIds.indexOf(currentSelectedId)

        // Home
        setTabState(ivHome, tvHome, selectedIndex == 0, R.drawable.ic_home_filled, R.drawable.ic_home_outline, animate)
        // Orders
        setTabState(ivOrders, tvOrders, selectedIndex == 1, R.drawable.ic_orders_filled, R.drawable.ic_orders_outline, animate)
        // Products
        setTabState(ivProducts, tvProducts, selectedIndex == 2, R.drawable.ic_products_filled, R.drawable.ic_products_outline, animate)
        // Inventory
        setTabState(ivInventory, tvInventory, selectedIndex == 3, R.drawable.ic_inventory_filled, R.drawable.ic_inventory_outline, animate)

        movePillToCurrentTab(animate = animate)
    }

    private fun setTabState(
        iv: ImageView,
        tv: TextView,
        isSelected: Boolean,
        filledRes: Int,
        outlineRes: Int,
        animate: Boolean
    ) {
        if (isSelected) {
            iv.setImageResource(filledRes)
            iv.imageTintList = ColorStateList.valueOf(colorActive)
            tv.setTextColor(colorActive)
            tv.typeface = Typeface.DEFAULT_BOLD

            if (animate) {
                iv.animate().cancel()
                iv.scaleX = 0.88f
                iv.scaleY = 0.88f
                iv.animate()
                    .scaleX(1.12f)
                    .scaleY(1.12f)
                    .setDuration(160)
                    .withEndAction {
                        iv.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            } else {
                iv.scaleX = 1.0f
                iv.scaleY = 1.0f
            }
        } else {
            iv.setImageResource(outlineRes)
            iv.imageTintList = ColorStateList.valueOf(colorInactive)
            tv.setTextColor(colorInactive)
            tv.typeface = Typeface.DEFAULT
            iv.animate().cancel()
            iv.scaleX = 1.0f
            iv.scaleY = 1.0f
        }
    }
}
