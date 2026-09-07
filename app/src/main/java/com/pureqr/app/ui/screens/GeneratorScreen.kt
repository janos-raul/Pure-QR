package com.pureqr.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pureqr.app.R
import com.pureqr.app.model.QrColor
import com.pureqr.app.model.QrFrame
import com.pureqr.app.model.QrType
import com.pureqr.app.util.FileSaver
import com.pureqr.app.util.PdfExporter
import com.pureqr.app.viewmodel.GeneratorUiState
import com.pureqr.app.viewmodel.GeneratorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    type: QrType,
    viewModel: GeneratorViewModel,
    useTwoPane: Boolean = false,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNamingDialog by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf<ExportType?>(null) }

    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { viewModel.importContact(context, it) }
    }

    val permissionDeniedMessage = stringResource(R.string.permission_denied_contacts)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            contactLauncher.launch(null)
        } else {
            scope.launch {
                snackBarHostState.showSnackbar(permissionDeniedMessage)
            }
        }
    }

    val onImportContact = {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)) {
            PackageManager.PERMISSION_GRANTED -> contactLauncher.launch(null)
            else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(type) {
        viewModel.setQrType(type)
    }

    val handleImmediateExport: (ExportType) -> Unit = { selectedType ->
        uiState.qrBitmap?.let { bitmap ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val defaultName = "QR_${type.label}_$timestamp"
            val title = when(type) {
                QrType.TEXT -> uiState.textContent
                QrType.URL -> uiState.urlContent
                QrType.WIFI -> "Wi-Fi: ${uiState.wifiData.ssid}"
                QrType.CONTACT -> "Contact: ${uiState.contactData.firstName} ${uiState.contactData.lastName}"
                QrType.CRYPTO -> "Crypto Address"
                QrType.BARCODE -> "Barcode: ${uiState.barcodeContent}"
            }

            when (selectedType) {
                ExportType.PRINT -> {
                    val uri = PdfExporter.generateQrPdf(context, bitmap, title, defaultName)
                    uri?.let { PdfExporter.printPdf(context, it, defaultName) }
                }
                ExportType.SHARE -> {
                    FileSaver.shareQrCode(context, bitmap, defaultName)
                }
                else -> {
                    exportType = selectedType
                    showNamingDialog = true
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(type.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val inputSection = @Composable { modifier: Modifier ->
            Column(modifier = modifier) {
                QrInputFields(
                    type = type,
                    uiState = uiState,
                    viewModel = viewModel,
                    onImportContact = onImportContact
                )
                
                SpacerPadding(24)
                QrCustomizationSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
                
                if (uiState.scannedUrls.isNotEmpty()) {
                    SpacerPadding(24)
                    ScannedUrlsSection(
                        urls = uiState.scannedUrls,
                        onUrlClick = { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                scope.launch {
                                    snackBarHostState.showSnackbar("Could not open link")
                                }
                            }
                        }
                    )
                }
            }
        }

        val previewSection = @Composable { modifier: Modifier ->
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QrPreviewCard(uiState.qrBitmap, type)
                SpacerPadding(24)
                QrActionButtons(
                    uiState = uiState,
                    onExport = handleImmediateExport
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (useTwoPane) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        inputSection(Modifier)
                    }
                    previewSection(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QrPreviewCard(uiState.qrBitmap, type)
                    SpacerPadding(24)
                    inputSection(Modifier)
                    SpacerPadding(32)
                    QrActionButtons(
                        uiState = uiState,
                        onExport = handleImmediateExport
                    )
                }
            }
        }
    }

    if (showNamingDialog && exportType != null) {
        val defaultNamePrefix = when(type) {
            QrType.TEXT -> "Text"
            QrType.URL -> "Link"
            QrType.WIFI -> "Wi-fi"
            QrType.CONTACT -> "Contact"
            QrType.CRYPTO -> "Crypto"
            QrType.BARCODE -> "Barcode"
        }
        
        FileNamingDialog(
            defaultName = defaultNamePrefix,
            onDismiss = { showNamingDialog = false },
            onConfirm = { fileName ->
                val currentExportType = exportType
                showNamingDialog = false
                val finalName = "QR_${defaultNamePrefix}_$fileName"
                
                if (currentExportType != null) {
                    when (currentExportType) {
                        ExportType.SAVE_PNG -> {
                            uiState.qrBitmap?.let { bitmap ->
                                val uri = FileSaver.saveQrToGallery(context, bitmap, finalName)
                                if (uri != null) {
                                    scope.launch { snackBarHostState.showSnackbar("Saved to Pictures: $finalName") }
                                }
                            }
                        }
                        ExportType.PDF -> {
                            uiState.qrBitmap?.let { bitmap ->
                                val title = when(type) {
                                    QrType.TEXT -> uiState.textContent
                                    QrType.URL -> uiState.urlContent
                                    QrType.WIFI -> "Wi-Fi: ${uiState.wifiData.ssid}"
                                    QrType.CONTACT -> "Contact: ${uiState.contactData.firstName} ${uiState.contactData.lastName}"
                                    QrType.CRYPTO -> "Crypto Address"
                                    QrType.BARCODE -> "Barcode: ${uiState.barcodeContent}"
                                }
                                val uri = PdfExporter.generateQrPdf(context, bitmap, title, finalName)
                                if (uri != null) {
                                    scope.launch {
                                        val result = snackBarHostState.showSnackbar(
                                            message = "PDF Saved: $finalName",
                                            actionLabel = "Share",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            FileSaver.shareFile(context, uri, "application/pdf")
                                        }
                                    }
                                }
                            }
                        }
                        else -> { /* SHARE and PRINT Handled directly in handleImmediateExport */ }
                    }
                }
            }
        )
    }
}

enum class ExportType {
    SAVE_PNG, SHARE, PDF, PRINT
}

@Composable
fun FileNamingDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export File") },
        text = {
            Column {
                Text(
                    text = "Enter a name for your file:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("[filename]") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    prefix = { Text("QR_${defaultName}_") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fileName.ifBlank { "Unamed" }) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SpacerPadding(size: Int) {
    Spacer(modifier = Modifier.height(size.dp))
}

@Composable
fun QrActionButtons(
    uiState: GeneratorUiState,
    onExport: (ExportType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onExport(ExportType.SAVE_PNG) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.qrBitmap != null
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_png))
            }

            OutlinedButton(
                onClick = { onExport(ExportType.SHARE) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.qrBitmap != null
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.share))
            }
        }

        Button(
            onClick = { onExport(ExportType.PDF) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = uiState.qrBitmap != null
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.export_pdf))
        }

        OutlinedButton(
            onClick = { onExport(ExportType.PRINT) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = uiState.qrBitmap != null
        ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Print QR Code")
        }
    }
}

@Composable
fun QrPreviewCard(bitmap: Bitmap?, type: QrType) {
    val isBarcode = type == QrType.BARCODE
    
    Box(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val targetModifier = if (isBarcode) {
            Modifier.fillMaxWidth().height(280.dp)
        } else {
            Modifier.size(280.dp)
        }

        Box(
            modifier = targetModifier,
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.qr_preview),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.start_typing),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedUrlsSection(
    urls: List<String>,
    onUrlClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (urls.size > 1) "Links Found" else "Link Found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            urls.forEach { url ->
                Surface(
                    onClick = { onUrlClick(url) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QrCustomizationSection(
    uiState: GeneratorUiState,
    viewModel: GeneratorViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Customize Colors & Styles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FrameSelection(
                        selectedFrame = uiState.frameType,
                        onFrameSelected = { viewModel.updateFrameType(it) }
                    )

                    if (uiState.frameType != QrFrame.NONE) {
                        ColorSelection(
                            title = "Frame Color",
                            selectedColor = uiState.frameColor,
                            onColorSelected = { viewModel.updateFrameColor(it) }
                        )
                    }

                    ColorSelection(
                        title = "Foreground Color",
                        selectedColor = uiState.foregroundColor,
                        onColorSelected = { viewModel.updateForegroundColor(it) }
                    )

                    ColorSelection(
                        title = "Background Color",
                        selectedColor = uiState.backgroundColor,
                        onColorSelected = { viewModel.updateBackgroundColor(it) },
                        isBackground = true
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSelection(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    isBackground: Boolean = false
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.ColorLens,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(QrColor.entries.toTypedArray()) { qrColor ->
                val isSelected = selectedColor == qrColor.color
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(qrColor.color), CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.LightGray.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(qrColor.color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (qrColor.color == android.graphics.Color.WHITE) Color.Black else Color.White,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrameSelection(
    selectedFrame: QrFrame,
    onFrameSelected: (QrFrame) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.CropOriginal,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Frame Style",
                style = MaterialTheme.typography.titleSmall
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(QrFrame.entries.toTypedArray()) { frame ->
                val isSelected = selectedFrame == frame
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onFrameSelected(frame) },
                    label = { Text(frame.label) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun QrInputFields(
    modifier: Modifier = Modifier,
    type: QrType,
    uiState: GeneratorUiState,
    viewModel: GeneratorViewModel,
    onImportContact: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (type) {
            QrType.TEXT -> {
                OutlinedTextField(
                    value = uiState.textContent,
                    onValueChange = { viewModel.updateText(it) },
                    label = { Text(stringResource(R.string.enter_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            QrType.URL -> {
                OutlinedTextField(
                    value = uiState.urlContent,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text(stringResource(R.string.enter_url)) },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            QrType.WIFI -> {
                OutlinedTextField(
                    value = uiState.wifiData.ssid,
                    onValueChange = { viewModel.updateWifi(uiState.wifiData.copy(ssid = it)) },
                    label = { Text("SSID (Network Name)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.wifiData.password,
                    onValueChange = { viewModel.updateWifi(uiState.wifiData.copy(password = it)) },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            QrType.CONTACT -> {
                Button(
                    onClick = onImportContact,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContactPage, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Contacts")
                }
                SpacerPadding(16)
                OutlinedTextField(
                    value = uiState.contactData.firstName,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(firstName = it)) },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.lastName,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(lastName = it)) },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.phone,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(phone = it)) },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.organization,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(organization = it)) },
                    label = { Text("Organization") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.jobTitle,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(jobTitle = it)) },
                    label = { Text("Job Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.website,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(website = it)) },
                    label = { Text("Website") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                SpacerPadding(12)
                OutlinedTextField(
                    value = uiState.contactData.address,
                    onValueChange = { viewModel.updateContact(uiState.contactData.copy(address = it)) },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            QrType.CRYPTO -> {
                OutlinedTextField(
                    value = uiState.cryptoContent,
                    onValueChange = { viewModel.updateCrypto(it) },
                    label = { Text("Crypto Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            QrType.BARCODE -> {
                OutlinedTextField(
                    value = uiState.barcodeContent,
                    onValueChange = { 
                        if (it.length <= 48) viewModel.updateBarcode(it) 
                    },
                    label = { Text("Barcode Content") },
                    placeholder = { Text("Up to 48 characters") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    supportingText = {
                        Text("${uiState.barcodeContent.length}/48")
                    }
                )
            }
        }
    }
}
