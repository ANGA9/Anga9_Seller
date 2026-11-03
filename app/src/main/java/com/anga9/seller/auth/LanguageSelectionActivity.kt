package com.anga9.seller.auth

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.utils.LocaleHelper
import com.google.android.material.card.MaterialCardView

/**
 * Language Selection Screen — shown only on first app launch.
 * Features a smooth running selection indicator box that glides towards
 * the selected option across rows and columns.
 */
class LanguageSelectionActivity : BaseActivity() {

    private var selectedLanguageCode: String = "en"
    private var isInitialized = false

    private lateinit var gridContainer: FrameLayout
    private lateinit var selectedIndicator: MaterialCardView

    private lateinit var cardHindi: MaterialCardView
    private lateinit var cardEnglish: MaterialCardView
    private lateinit var cardMarathi: MaterialCardView
    private lateinit var cardGujarati: MaterialCardView
    private lateinit var cardPunjabi: MaterialCardView
    private lateinit var cardTamil: MaterialCardView

    private lateinit var checkHindi: View
    private lateinit var checkEnglish: View
    private lateinit var checkMarathi: View
    private lateinit var checkGujarati: View
    private lateinit var checkPunjabi: View
    private lateinit var checkTamil: View

    private lateinit var tvHindiTitle: TextView
    private lateinit var tvHindiSub: TextView
    private lateinit var tvEnglishTitle: TextView
    private lateinit var tvEnglishSub: TextView
    private lateinit var tvMarathiTitle: TextView
    private lateinit var tvMarathiSub: TextView
    private lateinit var tvGujaratiTitle: TextView
    private lateinit var tvGujaratiSub: TextView
    private lateinit var tvPunjabiTitle: TextView
    private lateinit var tvPunjabiSub: TextView
    private lateinit var tvTamilTitle: TextView
    private lateinit var tvTamilSub: TextView

    private lateinit var tvLangTitle: TextView
    private lateinit var tvLangSubtitle: TextView
    private lateinit var btnContinue: TextView

    private val unselectedStrokeColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_stroke) }
    private val selectedTextColor by lazy { ContextCompat.getColor(this, R.color.language_card_selected_text) }
    private val unselectedTextColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_text) }
    private val unselectedSubColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_sub) }

    private var currentActiveCard: MaterialCardView? = null
    private var currentActiveCheck: View? = null
    private var currentActiveTitle: TextView? = null
    private var currentActiveSub: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)
        applySystemInsets(findViewById(R.id.rootLangContent))

        initViews()
        setupClickListeners()

        val currentLang = LocaleHelper.getSavedLanguage(this)
        val initialLang = if (currentLang.isNotEmpty()) currentLang else "en"

        // Ensure views are laid out before calculating positions for smooth initial placement
        gridContainer.post {
            selectLanguage(initialLang, animated = false)
            isInitialized = true
        }
    }

    private fun initViews() {
        gridContainer = findViewById(R.id.gridContainer)
        selectedIndicator = findViewById(R.id.selectedIndicator)

        tvLangTitle = findViewById(R.id.tvLangTitle)
        tvLangSubtitle = findViewById(R.id.tvLangSubtitle)

        cardHindi = findViewById(R.id.cardHindi)
        cardEnglish = findViewById(R.id.cardEnglish)
        cardMarathi = findViewById(R.id.cardMarathi)
        cardGujarati = findViewById(R.id.cardGujarati)
        cardPunjabi = findViewById(R.id.cardPunjabi)
        cardTamil = findViewById(R.id.cardTamil)

        checkHindi = findViewById(R.id.checkHindi)
        checkEnglish = findViewById(R.id.checkEnglish)
        checkMarathi = findViewById(R.id.checkMarathi)
        checkGujarati = findViewById(R.id.checkGujarati)
        checkPunjabi = findViewById(R.id.checkPunjabi)
        checkTamil = findViewById(R.id.checkTamil)

        tvHindiTitle = findViewById(R.id.tvHindiTitle)
        tvHindiSub = findViewById(R.id.tvHindiSub)
        tvEnglishTitle = findViewById(R.id.tvEnglishTitle)
        tvEnglishSub = findViewById(R.id.tvEnglishSub)
        tvMarathiTitle = findViewById(R.id.tvMarathiTitle)
        tvMarathiSub = findViewById(R.id.tvMarathiSub)
        tvGujaratiTitle = findViewById(R.id.tvGujaratiTitle)
        tvGujaratiSub = findViewById(R.id.tvGujaratiSub)
        tvPunjabiTitle = findViewById(R.id.tvPunjabiTitle)
        tvPunjabiSub = findViewById(R.id.tvPunjabiSub)
        tvTamilTitle = findViewById(R.id.tvTamilTitle)
        tvTamilSub = findViewById(R.id.tvTamilSub)

        btnContinue = findViewById(R.id.btnContinue)
    }

    private fun setupClickListeners() {
        cardHindi.setOnClickListener { selectLanguage("hi", animated = true) }
        cardEnglish.setOnClickListener { selectLanguage("en", animated = true) }
        cardMarathi.setOnClickListener { selectLanguage("mr", animated = true) }
        cardGujarati.setOnClickListener { selectLanguage("gu", animated = true) }
        cardPunjabi.setOnClickListener { selectLanguage("pa", animated = true) }
        cardTamil.setOnClickListener { selectLanguage("ta", animated = true) }
        btnContinue.setOnClickListener { onContinue() }
    }

    private fun selectLanguage(code: String, animated: Boolean) {
        if (code == selectedLanguageCode && isInitialized) return
        selectedLanguageCode = code

        val (targetCard, targetCheck, targetTitle, targetSub) = when (code) {
            "hi" -> Quad(cardHindi, checkHindi, tvHindiTitle, tvHindiSub)
            "en" -> Quad(cardEnglish, checkEnglish, tvEnglishTitle, tvEnglishSub)
            "mr" -> Quad(cardMarathi, checkMarathi, tvMarathiTitle, tvMarathiSub)
            "gu" -> Quad(cardGujarati, checkGujarati, tvGujaratiTitle, tvGujaratiSub)
            "pa" -> Quad(cardPunjabi, checkPunjabi, tvPunjabiTitle, tvPunjabiSub)
            "ta" -> Quad(cardTamil, checkTamil, tvTamilTitle, tvTamilSub)
            else -> Quad(cardEnglish, checkEnglish, tvEnglishTitle, tvEnglishSub)
        }

        val (targetX, targetY) = getRelativePosition(targetCard, gridContainer)

        val prevCard = currentActiveCard
        val prevCheck = currentActiveCheck
        val prevTitle = currentActiveTitle
        val prevSub = currentActiveSub

        currentActiveCard = targetCard
        currentActiveCheck = targetCheck
        currentActiveTitle = targetTitle
        currentActiveSub = targetSub

        // Ensure indicator matches the card dimensions
        if (selectedIndicator.layoutParams.width != targetCard.width && targetCard.width > 0) {
            val lp = selectedIndicator.layoutParams
            lp.width = targetCard.width
            lp.height = targetCard.height
            selectedIndicator.layoutParams = lp
        }

        if (!animated || !isInitialized) {
            // Initial positioning without animation
            selectedIndicator.animate().cancel()
            selectedIndicator.x = targetX
            selectedIndicator.y = targetY
            selectedIndicator.visibility = View.VISIBLE

            // Set strokes
            val allCards = listOf(cardHindi, cardEnglish, cardMarathi, cardGujarati, cardPunjabi, cardTamil)
            allCards.forEach { card ->
                if (card == targetCard) {
                    card.strokeColor = ContextCompat.getColor(this, android.R.color.transparent)
                } else {
                    card.strokeColor = unselectedStrokeColor
                }
            }

            // Set checks
            val allChecks = listOf(checkHindi, checkEnglish, checkMarathi, checkGujarati, checkPunjabi, checkTamil)
            allChecks.forEach { it.visibility = View.GONE }
            targetCheck.visibility = View.VISIBLE
            targetCheck.scaleX = 1f
            targetCheck.scaleY = 1f
            targetCheck.alpha = 1f

            // Set texts
            val allTitles = listOf(tvHindiTitle, tvEnglishTitle, tvMarathiTitle, tvGujaratiTitle, tvPunjabiTitle, tvTamilTitle)
            val allSubtitles = listOf(tvHindiSub, tvEnglishSub, tvMarathiSub, tvGujaratiSub, tvPunjabiSub, tvTamilSub)
            allTitles.forEach { it.setTextColor(unselectedTextColor) }
            allSubtitles.forEach { it.setTextColor(unselectedSubColor) }
            targetTitle.setTextColor(selectedTextColor)
            targetSub.setTextColor(selectedTextColor)
        } else {
            // Smoothly animate the selection box running towards the newly selected option
            selectedIndicator.visibility = View.VISIBLE
            selectedIndicator.animate().cancel()

            // Restore previous card's stroke border
            prevCard?.strokeColor = unselectedStrokeColor

            // Animate previous checkmark out
            prevCheck?.animate()?.cancel()
            prevCheck?.animate()
                ?.scaleX(0f)
                ?.scaleY(0f)
                ?.alpha(0f)
                ?.setDuration(160)
                ?.withEndAction { prevCheck.visibility = View.GONE }
                ?.start()

            // Animate previous text back to unselected
            if (prevTitle != null && prevSub != null) {
                animateTextColor(prevTitle, selectedTextColor, unselectedTextColor, 240)
                animateTextColor(prevSub, selectedTextColor, unselectedSubColor, 240)
            }

            // Animate running indicator box
            selectedIndicator.animate()
                .x(targetX)
                .y(targetY)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator(1.8f))
                .withEndAction {
                    targetCard.strokeColor = ContextCompat.getColor(this, android.R.color.transparent)
                }
                .start()

            // Animate target text to selected blue
            animateTextColor(targetTitle, unselectedTextColor, selectedTextColor, 240)
            animateTextColor(targetSub, unselectedSubColor, selectedTextColor, 240)

            // Animate target checkmark popping in with bounce
            targetCheck.animate().cancel()
            targetCheck.visibility = View.VISIBLE
            targetCheck.scaleX = 0f
            targetCheck.scaleY = 0f
            targetCheck.alpha = 0f
            targetCheck.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(240)
                .setStartDelay(100)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()
        }

        updateLocalizedLabels(code)
    }

    private fun animateTextColor(textView: TextView, fromColor: Int, toColor: Int, duration: Long) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        animator.duration = duration
        animator.addUpdateListener { va ->
            textView.setTextColor(va.animatedValue as Int)
        }
        animator.start()
    }

    private fun getRelativePosition(child: View, ancestor: View): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        var v: View? = child
        while (v != null && v != ancestor) {
            x += v.x
            y += v.y
            v = v.parent as? View
        }
        return Pair(x, y)
    }

    private fun updateLocalizedLabels(code: String) {
        when (code) {
            "hi" -> {
                tvLangTitle.text = "अपनी भाषा चुनें"
                tvLangSubtitle.text = "आगे बढ़ने के लिए अपनी पसंदीदा भाषा चुनें"
                btnContinue.text = "आगे बढ़ें"
            }
            "gu" -> {
                tvLangTitle.text = "તમારી ભાષા પસંદ કરો"
                tvLangSubtitle.text = "આગળ વધવા માટે તમારી પસંદગીની ભાષા પસંદ કરો"
                btnContinue.text = "આગળ વધો"
            }
            "mr" -> {
                tvLangTitle.text = "तुमची भाषा निवडा"
                tvLangSubtitle.text = "पुढे जाण्यासाठी तुमची पसंतीची भाषा निवडा"
                btnContinue.text = "पुढे सुरू ठेवा"
            }
            "pa" -> {
                tvLangTitle.text = "ਆਪਣੀ ਭਾਸ਼ਾ ਚੁਣੋ"
                tvLangSubtitle.text = "ਅੱਗੇ ਵਧਣ ਲਈ ਆਪਣੀ ਪਸੰਦੀਦਾ ਭਾਸ਼ਾ ਚੁਣੋ"
                btnContinue.text = "ਅੱਗੇ ਵਧੋ"
            }
            "ta" -> {
                tvLangTitle.text = "உங்கள் மொழியை தேர்ந்தெடுக்கவும்"
                tvLangSubtitle.text = "தொடர உங்கள் விருப்பமான மொழியைத் தேர்ந்தெடுக்கவும்"
                btnContinue.text = "தொடரவும்"
            }
            else -> {
                tvLangTitle.text = "Select Your Language"
                tvLangSubtitle.text = "Choose your preferred language to continue"
                btnContinue.text = "Continue"
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun onContinue() {
        LocaleHelper.setLocale(this, selectedLanguageCode)
        val intent = Intent(this, SellerPhoneLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
