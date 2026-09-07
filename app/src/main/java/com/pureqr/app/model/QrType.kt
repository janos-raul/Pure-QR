package com.pureqr.app.model

enum class QrType(val label: String) {
    TEXT("Text"),
    URL("URL"),
    WIFI("WiFi"),
    CONTACT("Contact (vCard)"),
    CRYPTO("Crypto Address"),
    BARCODE("Barcode (Code 128)")
}
