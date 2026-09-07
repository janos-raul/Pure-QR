package com.pureqr.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pureqr.app.model.ContactData
import com.pureqr.app.model.HistoryItem
import com.pureqr.app.model.QrColor
import com.pureqr.app.model.QrFrame
import com.pureqr.app.model.QrType
import com.pureqr.app.model.WifiData
import com.pureqr.app.util.ContactHelper
import com.pureqr.app.util.QrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GeneratorUiState(
    val qrType: QrType = QrType.TEXT,
    val textContent: String = "",
    val urlContent: String = "",
    val wifiData: WifiData = WifiData(),
    val contactData: ContactData = ContactData(),
    val cryptoContent: String = "",
    val barcodeContent: String = "",
    val qrBitmap: Bitmap? = null,
    val qrSize: Int = 512,
    val foregroundColor: Int = Color.BLACK,
    val backgroundColor: Int = Color.WHITE,
    val frameType: QrFrame = QrFrame.ROUNDED,
    val frameColor: Int = Color.BLACK,
    val isGenerating: Boolean = false,
    val scannedUrls: List<String> = emptyList()
)

class GeneratorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    var onSaveToHistory: ((HistoryItem) -> Unit)? = null
    private var historySaveJob: Job? = null

    fun setQrType(type: QrType) {
        _uiState.update { it.copy(qrType = type) }
        generateQr()
    }

    fun loadScannedData(type: QrType, data: Any, saveToHistory: Boolean = false) {
        _uiState.update { state ->
            val newState = when (type) {
                QrType.TEXT -> state.copy(qrType = type, textContent = data as String)
                QrType.URL -> state.copy(qrType = type, urlContent = data as String)
                QrType.WIFI -> state.copy(qrType = type, wifiData = data as WifiData)
                QrType.CONTACT -> state.copy(qrType = type, contactData = data as ContactData)
                QrType.CRYPTO -> state.copy(qrType = type, cryptoContent = data as String)
                QrType.BARCODE -> state.copy(qrType = type, barcodeContent = data as String)
            }
            
            // Extract URLs from the data if it's text-based
            val urls = extractUrls(data)
            newState.copy(scannedUrls = urls)
        }
        generateQr(skipHistory = !saveToHistory)
    }

    private fun extractUrls(data: Any): List<String> {
        val text = when (data) {
            is String -> data
            is ContactData -> data.website
            else -> ""
        }
        
        if (text.isBlank()) return emptyList()
        
        val urlRegex = "(https?://[\\w-]+(\\.[\\w-]+)+(/[\\w- ./?%&=]*)?)".toRegex(RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text).map { it.value }.toList().distinct()
    }

    fun updateText(content: String) {
        val urls = extractUrls(content)
        _uiState.update { it.copy(textContent = content, scannedUrls = urls) }
        generateQr()
    }

    fun updateUrl(content: String) {
        val urls = extractUrls(content)
        _uiState.update { it.copy(urlContent = content, scannedUrls = urls) }
        generateQr()
    }

    fun updateWifi(data: WifiData) {
        _uiState.update { it.copy(wifiData = data, scannedUrls = emptyList()) }
        generateQr()
    }

    fun updateContact(data: ContactData) {
        val urls = extractUrls(data)
        _uiState.update { it.copy(contactData = data, scannedUrls = urls) }
        generateQr()
    }

    fun importContact(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contactData = ContactHelper.getContactData(context, uri)
            contactData?.let { data ->
                withContext(Dispatchers.Main) {
                    updateContact(data)
                }
            }
        }
    }

    fun updateCrypto(content: String) {
        val urls = extractUrls(content)
        _uiState.update { it.copy(cryptoContent = content, scannedUrls = urls) }
        generateQr()
    }

    fun updateBarcode(content: String) {
        val urls = extractUrls(content)
        _uiState.update { it.copy(barcodeContent = content, scannedUrls = urls) }
        generateQr()
    }

    fun updateQrSize(size: Int) {
        _uiState.update { it.copy(qrSize = size) }
        generateQr()
    }

    fun updateForegroundColor(color: Int) {
        _uiState.update { it.copy(foregroundColor = color) }
        generateQr()
    }

    fun updateBackgroundColor(color: Int) {
        _uiState.update { it.copy(backgroundColor = color) }
        generateQr()
    }

    fun updateFrameType(frame: QrFrame) {
        _uiState.update { it.copy(frameType = frame) }
        generateQr()
    }

    fun updateFrameColor(color: Int) {
        _uiState.update { it.copy(frameColor = color) }
        generateQr()
    }

    private fun generateQr(skipHistory: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val content = when (state.qrType) {
                QrType.TEXT -> state.textContent
                QrType.URL -> state.urlContent
                QrType.WIFI -> QrGenerator.formatWifiContent(state.wifiData)
                QrType.CONTACT -> QrGenerator.formatVCardContent(state.contactData)
                QrType.CRYPTO -> state.cryptoContent
                QrType.BARCODE -> state.barcodeContent
            }

            if (content.isBlank()) {
                _uiState.update { it.copy(qrBitmap = null) }
                return@launch
            }

            _uiState.update { it.copy(isGenerating = true) }

            // Move heavy generation to a background thread
            val bitmap = withContext(Dispatchers.Default) {
                if (state.qrType == QrType.BARCODE) {
                    QrGenerator.generateBarcode(
                        content,
                        width = 800,
                        height = 400,
                        foregroundColor = state.foregroundColor,
                        backgroundColor = state.backgroundColor,
                        frameType = state.frameType,
                        frameColor = state.frameColor
                    )
                } else {
                    QrGenerator.generateQrCode(
                        content,
                        state.qrSize,
                        frameType = state.frameType,
                        frameColor = state.frameColor,
                        foregroundColor = state.foregroundColor,
                        backgroundColor = state.backgroundColor,
                        qrType = state.qrType
                    )
                }
            }

            _uiState.update { it.copy(qrBitmap = bitmap, isGenerating = false) }

            if (!skipHistory) {
                scheduleSaveToHistory(content, state.qrType)
            }
        }
    }

    private fun scheduleSaveToHistory(content: String, type: QrType) {
        historySaveJob?.cancel()
        historySaveJob = viewModelScope.launch {
            delay(2000) // Debounce 2 seconds
            onSaveToHistory?.invoke(
                HistoryItem(
                    content = content,
                    type = type,
                    isGenerated = true
                )
            )
        }
    }
}
