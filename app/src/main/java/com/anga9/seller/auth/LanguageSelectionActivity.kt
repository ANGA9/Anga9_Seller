package com.anga9.seller.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.anga9.seller.R
import com.anga9.seller.utils.LocaleHelper

/**
 * Language Selection Screen — shown only on first app launch.
 * Language names are hardcoded (not from @string/) because locale is not yet set.
 * After selection, locale is applied and user proceeds to phone login.
 */
class LanguageSelectionActivity : AppCompatActivity() {

    private var selectedLanguageCode: String = "en"

    private lateinit var cardHindi: CardView
    private lateinit var cardEnglish: CardView
    private lateinit var cardMarathi: CardView
    private lateinit var cardGujarati: CardView
    private lateinit var cardPunjabi: CardView
    private lateinit var cardTamil: CardView

    private lateinit var checkHindi: View
    private lateinit var checkEnglish: View
    private lateinit var checkMarathi: View
    private lateinit var checkGujarati: View
    private lateinit var checkPunjabi: View
    private lateinit var checkTamil: View

    private lateinit var btnContinue: android.widget.TextView

    private val selectedColor by lazy { getColor(R.color.language_card_selected) }
    private val unselectedColor by lazy { getColor(R.color.language_card_unselected) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)

        initViews()
        setupClickListeners()
        selectLanguage("en") // Default
    }

    private fun initViews() {
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

        // Reset all
        allCards.forEach { it.setCardBackgroundColor(unselectedColor) }
        allChecks.forEach { it.visibility = View.GONE }

        // Highlight selected
        val (card, check) = when (code) {
            "hi" -> Pair(cardHindi, checkHindi)
            "en" -> Pair(cardEnglish, checkEnglish)
            "mr" -> Pair(cardMarathi, checkMarathi)
            "gu" -> Pair(cardGujarati, checkGujarati)
            "pa" -> Pair(cardPunjabi, checkPunjabi)
            "ta" -> Pair(cardTamil, checkTamil)
            else -> Pair(cardEnglish, checkEnglish)
        }
        card.setCardBackgroundColor(selectedColor)
        check.visibility = View.VISIBLE
    }

    private fun onContinue() {
        LocaleHelper.setLocale(this, selectedLanguageCode)
        val intent = Intent(this, SellerPhoneLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
