package com.belluxx.transfire.utils

fun normalizeUrl(url: String): String {
    return if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
}