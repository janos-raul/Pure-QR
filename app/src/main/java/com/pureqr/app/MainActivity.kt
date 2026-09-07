package com.pureqr.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pureqr.app.model.HistoryItem
import com.pureqr.app.model.QrType
import com.pureqr.app.ui.screens.GeneratorScreen
import com.pureqr.app.ui.screens.HistoryScreen
import com.pureqr.app.ui.screens.HomeScreen
import com.pureqr.app.ui.screens.ScannerScreen
import com.pureqr.app.ui.screens.SettingsScreen
import com.pureqr.app.ui.theme.PureQRTheme
import com.pureqr.app.util.BarcodeParser
import com.pureqr.app.util.QrGenerator
import com.pureqr.app.viewmodel.GeneratorViewModel
import com.pureqr.app.viewmodel.HistoryViewModel
import com.pureqr.app.viewmodel.SettingsViewModel
import com.pureqr.app.viewmodel.ThemeMode

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val windowSizeClass = calculateWindowSizeClass(this)
            val useTwoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            val navController = rememberNavController()
            val generatorViewModel: GeneratorViewModel = viewModel()
            val historyViewModel: HistoryViewModel = viewModel()

            PureQRTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PureQRApp(
                        useTwoPane = useTwoPane,
                        settingsViewModel = settingsViewModel,
                        navController = navController,
                        generatorViewModel = generatorViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }

            // Handle incoming share intent
            LaunchedEffect(intent) {
                handleIntent(intent, generatorViewModel, navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(
        intent: Intent,
        viewModel: GeneratorViewModel,
        navController: NavHostController
    ) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                val type = if (sharedText.startsWith("http")) QrType.URL else QrType.TEXT
                viewModel.loadScannedData(type, sharedText, saveToHistory = true)
                navController.navigate("generator/${type.name}") {
                    popUpTo("home")
                }
            }
        }
    }
}

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PureQRApp(
    useTwoPane: Boolean,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController,
    generatorViewModel: GeneratorViewModel,
    historyViewModel: HistoryViewModel
) {
    // Configure history saving
    generatorViewModel.onSaveToHistory = { item ->
        historyViewModel.addToHistory(item)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onTypeSelected = { type ->
                    navController.navigate("generator/${type.name}")
                },
                onScanClick = {
                    navController.navigate("scanner")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onHistoryClick = {
                    navController.navigate("history")
                }
            )
        }
        composable("scanner") {
            val context = androidx.compose.ui.platform.LocalContext.current
            ScannerScreen(
                onCodeScanned = { barcode ->
                    val result = BarcodeParser.parseBarcode(barcode)
                    if (result != null) {
                        val content = when(result.first) {
                            QrType.WIFI -> QrGenerator.formatWifiContent(result.second as com.pureqr.app.model.WifiData)
                            QrType.CONTACT -> QrGenerator.formatVCardContent(result.second as com.pureqr.app.model.ContactData)
                            else -> result.second as String
                        }
                        
                        historyViewModel.addToHistory(
                            HistoryItem(
                                content = content,
                                type = result.first,
                                isGenerated = false
                            )
                        )
                        
                        generatorViewModel.loadScannedData(result.first, result.second, saveToHistory = false)
                        navController.navigate("generator/${result.first.name}") {
                            popUpTo("home")
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onWifiConnect = { raw ->
                    BarcodeParser.parseWifi(raw)
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    context.startActivity(intent)
                },
                onSaveContact = { raw ->
                    val contactData = BarcodeParser.parseVCard(raw)
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(android.provider.ContactsContract.Intents.Insert.NAME, "${contactData.firstName} ${contactData.lastName}".trim())
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, contactData.phone)
                        putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, contactData.email)
                    }
                    context.startActivity(intent)
                }
            )
        }
        composable(
            route = "generator/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeName = backStackEntry.arguments?.getString("type")
            val type = QrType.valueOf(typeName ?: QrType.TEXT.name)
            
            GeneratorScreen(
                type = type,
                viewModel = generatorViewModel,
                useTwoPane = useTwoPane,
                onBack = { navController.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = historyViewModel,
                onItemClick = { item ->
                    val result = BarcodeParser.parseHistoryItem(item)
                    if (result != null) {
                        generatorViewModel.loadScannedData(result.first, result.second, saveToHistory = false)
                        navController.navigate("generator/${result.first.name}")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
