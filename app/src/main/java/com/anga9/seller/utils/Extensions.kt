package com.anga9.seller.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Fragment.toast(msg: String) =
    requireContext().toast(msg)

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
}

fun String.isValidGst(): Boolean {
    val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
    return gstRegex.matches(this.uppercase())
}

fun String.isValidPan(): Boolean {
    val panRegex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
    return panRegex.matches(this.uppercase())
}

fun String.isValidPhone(): Boolean {
    return this.length == 10 && this.all { it.isDigit() }
}
