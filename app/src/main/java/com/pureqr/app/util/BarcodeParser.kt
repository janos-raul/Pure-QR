package com.pureqr.app.util

import com.google.mlkit.vision.barcode.common.Barcode
import com.pureqr.app.model.ContactData
import com.pureqr.app.model.HistoryItem
import com.pureqr.app.model.QrType
import com.pureqr.app.model.WifiData

object BarcodeParser {

    fun parseHistoryItem(item: HistoryItem): Pair<QrType, Any>? {
        if (item.type == QrType.BARCODE) {
            return QrType.BARCODE to item.content
        }
        return parseContent(item.content)
    }

    fun parseBarcode(barcode: Barcode): Pair<QrType, Any>? {
        // First check if it's a 1D Barcode format
        if (is1DBarcode(barcode.format)) {
            return QrType.BARCODE to (barcode.rawValue ?: "")
        }

        return when (barcode.valueType) {
            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                QrType.WIFI to WifiData(
                    ssid = wifi?.ssid ?: "",
                    password = wifi?.password ?: "",
                    encryption = when (wifi?.encryptionType) {
                        Barcode.WiFi.TYPE_OPEN -> "None"
                        Barcode.WiFi.TYPE_WEP -> "WEP"
                        else -> "WPA"
                    }
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val contact = barcode.contactInfo
                QrType.CONTACT to ContactData(
                    firstName = contact?.name?.first ?: "",
                    lastName = contact?.name?.last ?: "",
                    phone = contact?.phones?.firstOrNull()?.number ?: "",
                    email = contact?.emails?.firstOrNull()?.address ?: "",
                    organization = contact?.organization ?: "",
                    jobTitle = contact?.title ?: "",
                    website = contact?.urls?.firstOrNull() ?: "",
                    address = contact?.addresses?.firstOrNull()?.addressLines?.joinToString(" ") ?: ""
                )
            }
            Barcode.TYPE_URL -> {
                QrType.URL to (barcode.url?.url ?: barcode.rawValue ?: "")
            }
            else -> {
                val content = barcode.rawValue ?: ""
                parseContent(content)
            }
        }
    }

    private fun is1DBarcode(format: Int): Boolean {
        return format == Barcode.FORMAT_CODE_128 ||
               format == Barcode.FORMAT_CODE_39 ||
               format == Barcode.FORMAT_CODE_93 ||
               format == Barcode.FORMAT_CODABAR ||
               format == Barcode.FORMAT_EAN_13 ||
               format == Barcode.FORMAT_EAN_8 ||
               format == Barcode.FORMAT_ITF ||
               format == Barcode.FORMAT_UPC_A ||
               format == Barcode.FORMAT_UPC_E
    }

    fun parseContent(content: String): Pair<QrType, Any>? {
        return when {
            content.startsWith("WIFI:", ignoreCase = true) -> {
                QrType.WIFI to parseWifi(content)
            }
            content.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                QrType.CONTACT to parseVCard(content)
            }
            content.startsWith("http://", ignoreCase = true) ||
            content.startsWith("https://", ignoreCase = true) -> {
                QrType.URL to content
            }
            else -> QrType.TEXT to content
        }
    }

    fun parseWifi(content: String): WifiData {
        var ssid = ""
        var password = ""
        var encryption = "WPA"

        val parts = content.removePrefix("WIFI:").split(";")
        for (part in parts) {
            when {
                part.startsWith("S:") -> ssid = part.substring(2)
                part.startsWith("P:") -> password = part.substring(2)
                part.startsWith("T:") -> encryption = part.substring(2)
            }
        }
        return WifiData(ssid, password, encryption)
    }

    fun parseVCard(content: String): ContactData {
        var firstName = ""
        var lastName = ""
        var phone = ""
        var email = ""
        var organization = ""
        var jobTitle = ""
        var website = ""
        var address = ""

        val lines = content.split("\n")
        for (line in lines) {
            when {
                line.startsWith("FN:") -> {
                    val full = line.substring(3)
                    val split = full.split(" ")
                    if (split.size > 1) {
                        firstName = split[0]
                        lastName = split.drop(1).joinToString(" ")
                    } else {
                        firstName = full
                    }
                }
                line.startsWith("TEL:") -> phone = line.substring(4)
                line.startsWith("EMAIL:") -> email = line.substring(6)
                line.startsWith("ORG:") -> organization = line.substring(4)
                line.startsWith("TITLE:") -> jobTitle = line.substring(6)
                line.startsWith("URL:") -> website = line.substring(4)
                line.startsWith("ADR:") -> {
                    address = line.substring(4).replace(";", " ").trim()
                }
            }
        }
        return ContactData(firstName, lastName, phone, email, "", organization, jobTitle, website, address)
    }
}
