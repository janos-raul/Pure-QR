package com.pureqr.app.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.OptIn

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onCodeScanned: (Barcode) -> Unit,
    onBack: () -> Unit,
    onWifiConnect: (String) -> Unit = {},
    onSaveContact: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var lastScannedBarcode by remember { mutableStateOf<Barcode?>(null) }
    var detectedBarcodes by remember { mutableStateOf<List<Barcode>>(emptyList()) }
    
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    val highRiskSeen = remember { mutableStateOf(setOf<String>()) }

    // Alert for new high risk barcodes
    LaunchedEffect(detectedBarcodes) {
        val currentDetected = detectedBarcodes
        val newHighRisk = currentDetected.filter {
            val rawValue = it.rawValue ?: ""
            calculateRisk(rawValue) == RiskLevel.HIGH && rawValue !in highRiskSeen.value
        }
        
        if (newHighRisk.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
            highRiskSeen.value += newHighRisk.mapNotNull { it.rawValue }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanSucceeded by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    val cameraControl = remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    
    // To map coordinates
    var imageSize by remember { mutableStateOf<Size?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(isFlashOn) {
        cameraControl.value?.enableTorch(isFlashOn)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Scan QR / Barcode" else "Payload Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTab == 0 && hasCameraPermission) {
                        IconButton(onClick = { isFlashOn = !isFlashOn }) {
                            Icon(
                                if (isFlashOn) Icons.Filled.FlashOn 
                                else Icons.Filled.FlashOff,
                                contentDescription = "Toggle Flashlight"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text("Scanner") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text("Inspector") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setResolutionSelector(resolutionSelector)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl.value = camera.cameraControl

                                val maxZoomRatio = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                                val options = BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                    .setZoomSuggestionOptions(
                                        com.google.mlkit.vision.barcode.ZoomSuggestionOptions.Builder { zoomFactor ->
                                            camera.cameraControl.setZoomRatio(zoomFactor)
                                            true
                                        }
                                        .setMaxSupportedZoomRatio(maxZoomRatio)
                                        .build()
                                    )
                                    .build()
                                val scanner = BarcodeScanning.getClient(options)

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !scanSucceeded) {
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val image = InputImage.fromMediaImage(mediaImage, rotation)
                                        
                                        // Store image size for coordinate mapping
                                        imageSize = if (rotation == 90 || rotation == 270) {
                                            Size(imageProxy.height, imageProxy.width)
                                        } else {
                                            Size(imageProxy.width, imageProxy.height)
                                        }

                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                detectedBarcodes = barcodes.take(10)
                                                if (barcodes.isNotEmpty() && !scanSucceeded) {
                                                    if (lastScannedBarcode == null) {
                                                        lastScannedBarcode = barcodes[0]
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                                        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                        val delta = detector.scaleFactor
                                        camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                                        return true
                                    }
                                }
                                val scaleGestureDetector = ScaleGestureDetector(ctx, listener)
                                previewView.setOnTouchListener { view, event ->
                                    scaleGestureDetector.onTouchEvent(event)
                                    view.performClick()
                                    true
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (selectedTab == 0) {
                    ScannerOverlay(
                        detectedBarcodes = detectedBarcodes,
                        imageSize = imageSize,
                        selectedBarcode = lastScannedBarcode,
                        onBarcodeSelected = { lastScannedBarcode = it }
                    )
                    
                    val selectedRisk = lastScannedBarcode?.rawValue?.let { calculateRisk(it) } ?: RiskLevel.LOW
                    val isSelectedMalicious = selectedRisk == RiskLevel.HIGH

                    if (lastScannedBarcode != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Security Status Badge
                                Surface(
                                    color = if (isSelectedMalicious) Color.Red.copy(alpha = 0.9f) else Color(0xFF4CAF50).copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelectedMalicious) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isSelectedMalicious) "MALICIOUS PAYLOAD" else "SAFE PAYLOAD",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { selectedTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Inspect")
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            scanSucceeded = true
                                            onCodeScanned(lastScannedBarcode!!) 
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelectedMalicious) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isSelectedMalicious) "Use Anyway" else "Use Code")
                                    }
                                }
                            }
                        }
                    }
else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                "Pinch to zoom && auto-zoom active",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PayloadInspector(
                            rawValue = lastScannedBarcode?.rawValue,
                            onUse = {
                                lastScannedBarcode?.let {
                                    scanSucceeded = true
                                    onCodeScanned(it)
                                }
                            },
                            onGoBack = { selectedTab = 0 },
                            onWifiConnect = { raw -> onWifiConnect(raw) },
                            onSaveContact = { raw -> onSaveContact(raw) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required")
                }
            }
        }
    }
}

enum class RiskLevel(val label: String, val color: Color) {
    HIGH("HIGH RISK", Color(0xFFE57373)),
    MEDIUM("MODERATE RISK", Color(0xFFFFB74D)),
    LOW("LOW RISK", Color(0xFF81C784)),
    INFO("INFO", Color(0xFF64B5F6))
}

@Composable
fun PayloadInspector(
    rawValue: String?,
    onUse: () -> Unit,
    onGoBack: () -> Unit,
    onWifiConnect: (String) -> Unit = {},
    onSaveContact: (String) -> Unit = {}
) {
    if (rawValue == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No code scanned yet", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onGoBack) {
                    Text("Return to Scanner")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Security Score Header
                SecurityScoreBadge(rawValue)
                
                Spacer(Modifier.height(16.dp))

                Text(
                    "Raw Content",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = rawValue,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Detailed Analysis
                SecurityAnalysisSection(
                    rawValue = rawValue,
                    onWifiConnect = onWifiConnect,
                    onSaveContact = onSaveContact
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back")
                }
                Button(
                    onClick = onUse,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Use Code")
                }
            }
        }
    }
}

@Composable
fun SecurityScoreBadge(rawValue: String) {
    val risk = calculateRisk(rawValue)
    
    Surface(
        color = risk.color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, risk.color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(risk) {
                    RiskLevel.HIGH -> Icons.Default.Warning
                    RiskLevel.MEDIUM -> Icons.Default.Security
                    RiskLevel.LOW -> Icons.Default.CheckCircle
                    RiskLevel.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                tint = risk.color
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = risk.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = risk.color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getRiskDescription(risk, rawValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SecurityAnalysisSection(
    rawValue: String,
    onWifiConnect: (String) -> Unit = {},
    onSaveContact: (String) -> Unit = {}
) {
    val findings = analyzePayload(rawValue)
    
    Text(
        "Payload Analysis",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    
    if (findings.isEmpty()) {
        ListItem(
            headlineContent = { Text("No suspicious patterns detected") },
            leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784)) }
        )
    } else {
        findings.forEach { finding ->
            ListItem(
                headlineContent = { Text(finding.title) },
                supportingContent = { Text(finding.description) },
                leadingContent = { Icon(finding.icon, contentDescription = null, tint = finding.risk.color) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }

    // Specialized UI for Wi-Fi/Contact
    if (rawValue.startsWith("WIFI:", ignoreCase = true)) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onWifiConnect(rawValue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Connect to Network")
        }
    } else if (rawValue.startsWith("BEGIN:VCARD", ignoreCase = true)) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSaveContact(rawValue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Contact")
        }
    }
}

data class AnalysisFinding(
    val title: String,
    val description: String,
    val risk: RiskLevel,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

fun analyzePayload(rawValue: String): List<AnalysisFinding> {
    val findings = mutableListOf<AnalysisFinding>()
    
    // Executables
    if (rawValue.contains(".apk", ignoreCase = true)) {
        findings.add(AnalysisFinding("Executable File", "Links to an Android app package (.apk). Use extreme caution.", RiskLevel.HIGH, Icons.Default.Warning))
    }
    if (rawValue.contains(".exe", ignoreCase = true) || rawValue.contains(".zip", ignoreCase = true)) {
        findings.add(AnalysisFinding("Potentially Malicious File", "Contains references to executable or compressed files.", RiskLevel.HIGH, Icons.Default.Warning))
    }
    
    // Intents & Actions
    if (rawValue.startsWith("SMSTO:", ignoreCase = true)) {
        val parts = rawValue.split(":")
        val recipient = parts.getOrNull(1) ?: "Unknown"
        findings.add(AnalysisFinding("SMS Action", "Triggers an SMS to $recipient. Verify message body before sending.", RiskLevel.MEDIUM, Icons.Default.Security))
    }
    if (rawValue.startsWith("tel:", ignoreCase = true)) {
        findings.add(AnalysisFinding("Phone Call Action", "Triggers a phone call. May lead to unauthorized charges.", RiskLevel.MEDIUM, Icons.Default.Security))
    }
    if (rawValue.startsWith("intent:", ignoreCase = true)) {
        findings.add(AnalysisFinding("System Intent", "Attempts to trigger a deep-link action in a specific app.", RiskLevel.MEDIUM, Icons.Default.Security))
    }
    
    // Privacy / Tracking
    val trackers = listOf("utm_", "ref", "fbclid", "gclid", "click_id", "uid", "affiliate_id")
    val foundTrackers = trackers.filter { rawValue.contains(it, ignoreCase = true) }
    if (foundTrackers.isNotEmpty()) {
        findings.add(AnalysisFinding("Tracking Detected", "Contains markers used to track your behavior (${foundTrackers.joinToString(", ")}).", RiskLevel.INFO, Icons.Default.Info))
    }
    
    return findings
}

fun calculateRisk(rawValue: String): RiskLevel {
    val findings = analyzePayload(rawValue)
    return when {
        findings.any { it.risk == RiskLevel.HIGH } -> RiskLevel.HIGH
        findings.any { it.risk == RiskLevel.MEDIUM } -> RiskLevel.MEDIUM
        findings.any { it.risk == RiskLevel.INFO } -> RiskLevel.INFO
        else -> RiskLevel.LOW
    }
}

fun getRiskDescription(risk: RiskLevel, @Suppress("UNUSED_PARAMETER") rawValue: String): String {
    return when(risk) {
        RiskLevel.HIGH -> "Critical security threat detected in the payload."
        RiskLevel.MEDIUM -> "Action-based payload detected. Verify before proceeding."
        RiskLevel.LOW -> "Standard payload. No immediate threats detected."
        RiskLevel.INFO -> "Content is safe but contains privacy trackers."
    }
}

@Composable
fun ScannerOverlay(
    detectedBarcodes: List<Barcode>,
    imageSize: Size?,
    selectedBarcode: Barcode?,
    onBarcodeSelected: (Barcode) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val linePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "linePosition"
    )

    // Keep the latest barcodes in a state that the pointerInput can access without restarting
    val currentBarcodes by rememberUpdatedState(detectedBarcodes)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageSize) {
                detectTapGestures { offset ->
                    imageSize?.let { imgSize ->
                        val width = size.width
                        val height = size.height
                        
                        // Calculate scale and offsets (CenterCrop logic)
                        val scale = maxOf(width / imgSize.width.toFloat(), height / imgSize.height.toFloat())
                        val offsetX = (width - imgSize.width * scale) / 2f
                        val offsetY = (height - imgSize.height * scale) / 2f
                        
                        val tappedBarcode = currentBarcodes.find { barcode ->
                            barcode.boundingBox?.let { box ->
                                // Map box coordinates to screen coordinates
                                val left = box.left * scale + offsetX
                                val top = box.top * scale + offsetY
                                val right = box.right * scale + offsetX
                                val bottom = box.bottom * scale + offsetY
                                
                                // Expand touch target for easier selection
                                val pad = 50f 
                                offset.x in (left - pad)..(right + pad) && 
                                offset.y in (top - pad)..(bottom + pad)
                            } ?: false
                        }
                        tappedBarcode?.let { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBarcodeSelected(it) 
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // 1. Draw the dim overlay with a hole in the middle
            val overlaySize = width * 0.7f
            val left = (width - overlaySize) / 2
            val top = (height - overlaySize) / 2
            
            val rect = Rect(left, top, left + overlaySize, top + overlaySize)
            val roundRect = RoundRect(rect, CornerRadius(24.dp.toPx()))
            
            val path = Path().apply {
                addRect(Rect(0f, 0f, width, height))
                addRoundRect(roundRect)
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.5f))
            
            // 2. Draw the scanning line
            val lineY = top + (overlaySize * linePosition)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Cyan, Color.Transparent)
                ),
                start = Offset(left + 8.dp.toPx(), lineY),
                end = Offset(left + overlaySize - 8.dp.toPx(), lineY),
                strokeWidth = 3.dp.toPx()
            )

            // 3. Draw bounding boxes for detected barcodes
            imageSize?.let { imgSize ->
                val scale = maxOf(width / imgSize.width.toFloat(), height / imgSize.height.toFloat())
                val offsetX = (width - imgSize.width * scale) / 2f
                val offsetY = (height - imgSize.height * scale) / 2f

                detectedBarcodes.forEach { barcode ->
                    barcode.boundingBox?.let { box ->
                        val rawValue = barcode.rawValue ?: ""
                        val risk = calculateRisk(rawValue)
                        
                        // Compare raw values to identify the selected code across frames
                        val isSelected = selectedBarcode != null && 
                                       !selectedBarcode.rawValue.isNullOrEmpty() && 
                                       rawValue == selectedBarcode.rawValue
                        
                        val leftF = box.left * scale + offsetX
                        val topF = box.top * scale + offsetY
                        val rightF = box.right * scale + offsetX
                        val bottomF = box.bottom * scale + offsetY

                        // NEW COLOR LOGIC:
                        // Selected + Malicious -> Red
                        // Selected + Safe -> Green
                        // Not Selected + Malicious -> Dimmed Red
                        // Not Selected + Safe -> White
                        val rectColor = if (isSelected) {
                            if (risk == RiskLevel.HIGH) Color.Red else Color.Green
                        } else {
                            if (risk == RiskLevel.HIGH) Color.Red.copy(alpha = 0.95f) else Color.Blue.copy(alpha = 0.95f)
                        }

                        val strokeWidth = if (isSelected) 6.dp.toPx() else 2.dp.toPx()

                        drawRoundRect(
                            color = rectColor,
                            topLeft = Offset(leftF, topF),
                            size = androidx.compose.ui.geometry.Size(rightF - leftF, bottomF - topF),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = strokeWidth)
                        )
                        
                        // Visual feedback for selection: Label and Arrow
                        if (isSelected) {
                            val themeColor = if (risk == RiskLevel.HIGH) Color.Red else Color.Green
                            val label = if (risk == RiskLevel.HIGH) "RISKY !" else "SAFE"
                            val centerX = leftF + (rightF - leftF) / 2f

                            // 1. Prepare paint for text measurement
                            val textPaint = Paint().apply {
                                textSize = 38f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }

                            val textWidth = textPaint.measureText(label)
                            val fontMetrics = textPaint.fontMetrics
                            val textHeight = fontMetrics.descent - fontMetrics.ascent

                            val paddingH = 12.dp.toPx()
                            val paddingV = 6.dp.toPx()
                            val boxWidth = textWidth + paddingH * 2
                            val boxHeight = textHeight + paddingV * 2

                            // Position the box above the arrow
                            val boxTop = topF - 36.dp.toPx() - boxHeight
                            val boxRect = Rect(centerX - boxWidth / 2f, boxTop, centerX + boxWidth / 2f, boxTop + boxHeight)

                            // 2. Draw the rounded box background
                            drawRoundRect(
                                color = themeColor,
                                topLeft = Offset(boxRect.left, boxRect.top),
                                size = androidx.compose.ui.geometry.Size(boxRect.width, boxRect.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )

                            // 3. Draw an arrow pointing down from the box to the barcode
                            val arrowPath = Path().apply {
                                moveTo(centerX, topF - 8.dp.toPx())
                                lineTo(centerX - 10.dp.toPx(), boxTop + boxHeight)
                                lineTo(centerX + 10.dp.toPx(), boxTop + boxHeight)
                                close()
                            }
                            drawPath(arrowPath, themeColor)

                            // 4. Draw text inside the box using white for high contrast
                            drawContext.canvas.nativeCanvas.apply {
                                textPaint.color = android.graphics.Color.WHITE
                                drawText(
                                    label,
                                    centerX,
                                    boxTop + paddingV - fontMetrics.ascent,
                                    textPaint
                                )
                            }
                        }

                        if (risk == RiskLevel.HIGH && !isSelected) {
                            drawCircle(
                                color = Color.Red.copy(alpha = 0.6f),
                                radius = 8.dp.toPx(),
                                center = Offset(leftF, topF)
                            )
                        }
                    }
                }
            }
        }
    }
}
