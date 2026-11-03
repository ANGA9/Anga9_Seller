package com.anga9.seller.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anga9.seller.BaseActivity
import com.anga9.seller.R
import com.anga9.seller.utils.LocaleHelper
import com.google.android.material.card.MaterialCardView

/**
 * Language Selection Screen — shown only on first app launch.
 * Follows ANGA9 Customer App brand styling:
 * Royal Blue accent (#1A6FD4), crisp defined card strokes, and balanced typography.
 */
class LanguageSelectionActivity : BaseActivity() {

    private var selectedLanguageCode: String = "en"

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

    private val selectedBgColor by lazy { ContextCompat.getColor(this, R.color.language_card_selected) }
    private val unselectedBgColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected) }
    private val selectedStrokeColor by lazy { ContextCompat.getColor(this, R.color.language_card_selected_stroke) }
    private val unselectedStrokeColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_stroke) }
    private val selectedTextColor by lazy { ContextCompat.getColor(this, R.color.language_card_selected_text) }
    private val unselectedTextColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_text) }
    private val unselectedSubColor by lazy { ContextCompat.getColor(this, R.color.language_card_unselected_sub) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)
        applySystemInsets(findViewById(R.id.rootLangContent))

        initViews()
        setupClickListeners()
        val currentLang = LocaleHelper.getSavedLanguage(this)
        selectLanguage(if (currentLang.isNotEmpty()) currentLang else "en")
    }

    private fun initViews() {
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
        cardHindi.setOnClickListener { selectLanguage("hi") }
        cardEnglish.setOnClickListener { selectLanguage("en") }
        cardMarathi.setOnClickListener { selectLanguage("mr") }
        cardGujarati.setOnClickListener { selectLanguage("gu") }
        cardPunjabi.setOnClickListener { selectLanguage("pa") }
        cardTamil.setOnClickListener { selectLanguage("ta") }
        btnContinue.setOnClickListener { onContinue() }
    }

    private fun selectLanguage(code: String) {
        selectedLanguageCode = code

        val allCards = listOf(cardHindi, cardEnglish, cardMarathi, cardGujarati, cardPunjabi, cardTamil)
        val allChecks = listOf(checkHindi, checkEnglish, checkMarathi, checkGujarati, checkPunjabi, checkTamil)
        val allTitles = listOf(tvHindiTitle, tvEnglishTitle, tvMarathiTitle, tvGujaratiTitle, tvPunjabiTitle, tvTamilTitle)
        val allSubtitles = listOf(tvHindiSub, tvEnglishSub, tvMarathiSub, tvGujaratiSub, tvPunjabiSub, tvTamilSub)

        val density = resources.displayMetrics.density
        val strokeSelected = (2 * density).toInt()
        val strokeUnselected = (1.5f * density).toInt()

        // Reset all cards to clean unselected state with defined outlines
        allCards.forEach { card ->
            card.setCardBackgroundColor(unselectedBgColor)
            card.strokeColor = unselectedStrokeColor
            card.strokeWidth = strokeUnselected
        }
        allChecks.forEach { it.visibility = View.GONE }
        allTitles.forEach { it.setTextColor(unselectedTextColor) }
        allSubtitles.forEach { it.setTextColor(unselectedSubColor) }

        // Determine which card is selected
        val (selectedCard, selectedCheck, selectedTitle, selectedSub) = when (code) {
            "hi" -> Quad(cardHindi, checkHindi, tvHindiTitle, tvHindiSub)
            "en" -> Quad(cardEnglish, checkEnglish, tvEnglishTitle, tvEnglishSub)
            "mr" -> Quad(cardMarathi, checkMarathi, tvMarathiTitle, tvMarathiSub)
            "gu" -> Quad(cardGujarati, checkGujarati, tvGujaratiTitle, tvGujaratiSub)
            "pa" -> Quad(cardPunjabi, checkPunjabi, tvPunjabiTitle, tvPunjabiSub)
            "ta" -> Quad(cardTamil, checkTamil, tvTamilTitle, tvTamilSub)
            else -> Quad(cardEnglish, checkEnglish, tvEnglishTitle, tvEnglishSub)
        }

        // Apply ANGA9 brand active state
        selectedCard.setCardBackgroundColor(selectedBgColor)
        selectedCard.strokeColor = selectedStrokeColor
        selectedCard.strokeWidth = strokeSelected
        selectedCheck.visibility = View.VISIBLE
        selectedTitle.setTextColor(selectedTextColor)
        selectedSub.setTextColor(selectedTextColor)

        // Dynamically update screen texts in selected language
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
