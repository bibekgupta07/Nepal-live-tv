package com.app.nepallivetv.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.Modifier

import androidx.core.net.toUri

/**
 * Shows a short toast message.
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Opens a URL in the web browser.
 */
fun Context.openBrowser(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    } catch (_: Exception) {
        showToast("Unable to open browser")
    }
}

/**
 * Compare semantic versions.
 * Returns true if this string represents a version greater than [other].
 */
fun String.isVersionGreaterThan(other: String): Boolean {
    val latestParts = this.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = other.split(".").mapNotNull { it.toIntOrNull() }

    val length = maxOf(latestParts.size, currentParts.size)
    for (i in 0 until length) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l > c) return true
        if (l < c) return false
    }
    return false
}

/**
 * Conditionally applies a modifier.
 */
inline fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
