package com.pureqr.app.model

import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: QrType,
    val timestamp: Long = System.currentTimeMillis(),
    val isGenerated: Boolean = true // true if generated, false if scanned
)
