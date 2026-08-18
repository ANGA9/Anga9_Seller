package com.anga9.seller.utils

/**
 * Indian phone number utilities for Seller App.
 *
 * Supabase requires E.164 format: +91XXXXXXXXXX
 *
 * Handles all common input formats:
 *   "9876543210"      -> "+919876543210"
 *   "09876543210"     -> "+919876543210"
 *   "919876543210"    -> "+919876543210"
 *   "+91 98765 43210" -> "+919876543210"
 *
 * TRAI rule: Indian mobile numbers start with 6, 7, 8, or 9 only.
 */
object PhoneUtils {

    /**
     * Normalize raw input to E.164 format (+91XXXXXXXXXX).
     * Returns null if the number is invalid.
     */
    fun normalizeIndianPhone(raw: String): String? {
        val digits = raw.replace(Regex("\\D"), "")

        val localNumber = when {
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0")  -> digits.substring(1)
            digits.length == 10                             -> digits
            else                                            -> return null
        }

        if (!localNumber.matches(Regex("^[6-9]\\d{9}$"))) return null
        return "+91$localNumber"
    }

    /**
     * Returns true if the raw input is a valid Indian mobile number.
     */
    fun isValidIndianPhone(raw: String): Boolean = normalizeIndianPhone(raw) != null

    /**
     * Mask phone for display. Example: "+919876543210" -> "+91 ******3210"
     */
    fun maskPhone(e164Phone: String): String {
        val local = e164Phone.removePrefix("+91").trim()
        return if (local.length >= 10) "+91 ******${local.takeLast(4)}" else e164Phone
    }
}
