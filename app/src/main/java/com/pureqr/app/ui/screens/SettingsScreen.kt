package com.pureqr.app.ui.screens

//import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
//import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pureqr.app.R
import com.pureqr.app.viewmodel.SettingsViewModel
import com.pureqr.app.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showInspectorInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection(title = "Appearance")
            
            SettingsItem(
                icon = Icons.Default.Brightness4,
                title = "Theme",
                subtitle = when (themeMode) {
                    ThemeMode.SYSTEM -> "System Default"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                onClick = { showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Privacy & Security")
            SettingsItem(
                icon = Icons.Default.Shield,
                title = "100% Offline",
                subtitle = "This app works fully offline. No data ever leaves your device."
            )
            SettingsItem(
                icon = Icons.Default.Security,
                title = "Payload Inspector",
                subtitle = "Advanced analysis of QR code content. Learn more about security features.",
                onClick = { showInspectorInfo = true }
            )
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "No Tracking",
                subtitle = "No ads, no analytics, no tracking. Your privacy is our priority."
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "${stringResource(R.string.app_name)} v1.4",
                subtitle = "Privacy-first QR tools with offline branding and security analysis."
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeOption("System Default", themeMode == ThemeMode.SYSTEM) {
                        viewModel.setThemeMode(ThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption("Light", themeMode == ThemeMode.LIGHT) {
                        viewModel.setThemeMode(ThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption("Dark", themeMode == ThemeMode.DARK) {
                        viewModel.setThemeMode(ThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInspectorInfo) {
        InspectorInfoDialog(onDismiss = { showInspectorInfo = false })
    }
}

@Composable
fun InspectorInfoDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Security Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    SecurityInfoItem(
                        icon = Icons.Default.BugReport,
                        title = "Hidden Executables",
                        description = "Pure-QR automatically identifies links to .apk, .exe, or .zip files. These can contain malicious software that installs without your knowledge.",
                        color = Color(0xFFE57373)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SecurityInfoItem(
                        icon = Icons.Default.Policy,
                        title = "Phishing & Intents",
                        description = "Some codes trigger phone actions like SMS or calls. We parse these 'intents' so you can see exactly who the recipient is before you send anything.",
                        color = Color(0xFFFFB74D)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SecurityInfoItem(
                        icon = Icons.Default.Shield,
                        title = "Deep Link Redirection",
                        description = "Attackers use complex deep links to bypass browser security. The inspector reveals the raw intent structure for complete transparency.",
                        color = Color(0xFF64B5F6)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SecurityInfoItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Security Scoring",
                        description = "Our offline engine assigns a risk level (High, Moderate, or Low) to every scan, helping you make informed decisions instantly.",
                        color = Color(0xFF81C784)
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(
                        text = "Everything is processed 100% locally on your device. No data is ever transmitted or stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityInfoItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val isClickable = onClick != null
    
    Surface(
        onClick = { onClick?.invoke() },
        enabled = isClickable,
        shape = RoundedCornerShape(16.dp),
        color = if (isClickable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
