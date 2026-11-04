const fs = require('fs');
const file = 'C:/Users/shaws/ANGA9/anga9_seller/app/src/main/java/com/anga9/seller/ui/dashboard/DashboardActivity.kt';
let lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);

// Find onResume line
let onResumeIdx = lines.findIndex(l => l.includes('override fun onResume()'));
if (onResumeIdx !== -1) {
    let onResumeEndIdx = -1;
    for (let i = onResumeIdx; i < lines.length; i++) {
        if (lines[i].trim() === '}') {
            onResumeEndIdx = i;
            break;
        }
    }
    if (onResumeEndIdx !== -1) {
        const replacement = [
            '    override fun onNewIntent(intent: Intent) {',
            '        super.onNewIntent(intent)',
            '        setIntent(intent)',
            '        handleNavIntent(intent)',
            '    }',
            '',
            '    private fun handleNavIntent(intent: Intent?) {',
            '        val tabId = intent?.getIntExtra(EXTRA_NAV_TAB, -1) ?: -1',
            '        if (tabId != -1) {',
            '            switchToTab(tabId)',
            '        }',
            '    }',
            '',
            '    override fun onResume() {',
            '        super.onResume()',
            '        if (activeTabId == R.id.nav_home) {',
            '            viewModel.refresh()',
            '        } else if (activeTabId == R.id.nav_orders && ordersFragment.isAdded) {',
            '            ordersFragment.refreshOrders()',
            '        } else if (activeTabId == R.id.nav_products && productsFragment.isAdded) {',
            '            productsFragment.refreshProducts()',
            '        } else if (activeTabId == R.id.nav_inventory && inventoryFragment.isAdded) {',
            '            inventoryFragment.refreshInventory()',
            '        }',
            '    }'
        ];
        lines.splice(onResumeIdx, onResumeEndIdx - onResumeIdx + 1, ...replacement);
    }
}

// Find setupBottomNavigation
let bNavIdx = lines.findIndex(l => l.includes('private fun setupBottomNavigation()'));
if (bNavIdx !== -1) {
    let bNavEndIdx = -1;
    let braceCount = 0;
    for (let i = bNavIdx; i < lines.length; i++) {
        if (lines[i].includes('{')) braceCount++;
        if (lines[i].includes('}')) {
            braceCount--;
            if (braceCount === 0) {
                bNavEndIdx = i;
                break;
            }
        }
    }
    if (bNavEndIdx !== -1) {
        const replacement = [
            '    private fun setupBottomNavigation() {',
            '        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->',
            '            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())',
            '            view.setPadding(',
            '                view.paddingLeft,',
            '                view.paddingTop,',
            '                view.paddingRight,',
            '                sysBarInsets.bottom',
            '            )',
            '            insets',
            '        }',
            '',
            '        bottomNavigation.setOnItemSelectedListener { item ->',
            '            showTab(item.itemId)',
            '            true',
            '        }',
            '    }',
            '',
            '    fun switchToTab(tabId: Int) {',
            '        bottomNavigation.setSelectedItemId(tabId, notifyListener = false)',
            '        showTab(tabId)',
            '    }',
            '',
            '    private fun showTab(tabId: Int) {',
            '        if (tabId == activeTabId) return',
            '',
            '        if (tabId == R.id.nav_home) {',
            '            tabFragmentContainer.animate()',
            '                .alpha(0f)',
            '                .setDuration(160)',
            '                .withEndAction {',
            '                    tabFragmentContainer.visibility = View.GONE',
            '                    val tx = supportFragmentManager.beginTransaction()',
            '                    listOf(ordersFragment, productsFragment, inventoryFragment).forEach {',
            '                        if (it.isAdded) tx.hide(it)',
            '                    }',
            '                    tx.commitAllowingStateLoss()',
            '                }',
            '                .start()',
            '',
            '            swipeRefresh.visibility = View.VISIBLE',
            '            swipeRefresh.alpha = 0f',
            '            swipeRefresh.animate()',
            '                .alpha(1f)',
            '                .setDuration(180)',
            '                .start()',
            '        } else {',
            '            swipeRefresh.animate()',
            '                .alpha(0f)',
            '                .setDuration(160)',
            '                .withEndAction {',
            '                    swipeRefresh.visibility = View.GONE',
            '                }',
            '                .start()',
            '',
            '            tabFragmentContainer.visibility = View.VISIBLE',
            '            tabFragmentContainer.alpha = 1f',
            '',
            '            val targetFragment = when (tabId) {',
            '                R.id.nav_orders -> ordersFragment',
            '                R.id.nav_products -> productsFragment',
            '                R.id.nav_inventory -> inventoryFragment',
            '                else -> ordersFragment',
            '            }',
            '',
            '            val tx = supportFragmentManager.beginTransaction()',
            '                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)',
            '',
            '            if (!targetFragment.isAdded) {',
            '                tx.add(R.id.tabFragmentContainer, targetFragment, targetFragment.javaClass.simpleName)',
            '            }',
            '',
            '            listOf(ordersFragment, productsFragment, inventoryFragment).forEach { frag ->',
            '                if (frag.isAdded) {',
            '                    if (frag == targetFragment) tx.show(frag) else tx.hide(frag)',
            '                }',
            '            }',
            '            tx.commitAllowingStateLoss()',
            '        }',
            '',
            '        activeTabId = tabId',
            '    }'
        ];
        lines.splice(bNavIdx, bNavEndIdx - bNavIdx + 1, ...replacement);
    }
}

let newContent = lines.join('\r\n');

// Update click listeners
newContent = newContent.replace(
    /btnQuickOrders\.setOnClickListener\s*\{[\s\S]*?\}/,
    'btnQuickOrders.setOnClickListener { switchToTab(R.id.nav_orders) }'
);
newContent = newContent.replace(
    /tvViewAllOrders\.setOnClickListener\s*\{[\s\S]*?\}/,
    'tvViewAllOrders.setOnClickListener { switchToTab(R.id.nav_orders) }'
);
newContent = newContent.replace(
    /tvViewAllTopProducts\.setOnClickListener\s*\{[\s\S]*?\}/,
    'tvViewAllTopProducts.setOnClickListener { switchToTab(R.id.nav_products) }'
);
newContent = newContent.replace(
    /topProductsAdapter\s*=\s*TopProductsAdapter\s*\{[\s\S]*?\}/,
    'topProductsAdapter = TopProductsAdapter { switchToTab(R.id.nav_products) }'
);

fs.writeFileSync(file, newContent, 'utf8');
console.log('Applied bottom nav and onResume changes successfully');
