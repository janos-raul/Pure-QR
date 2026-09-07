package com.pureqr.app.model

data class WifiData(
    val ssid: String = "",
    val password: String = "",
    val encryption: String = "WPA" // WPA, WEP, or None
)

data class ContactData(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val note: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val website: String = "",
    val address: String = ""
)

