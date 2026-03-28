# Pure-QR 📱✨

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1+-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-100%25_Offline-success?style=for-the-badge)](docs/Pure-QR_PrivacyPolicy.md)

**Pure-QR** is a minimalist, privacy-first Android application designed for generating and scanning QR codes and barcodes. Built with modern Android technologies, it ensures all data stays on your device—no internet, no tracking, no compromise.

---

## 🚀 Key Features

### 🔍 Smart Scanner & Inspector
- **Auto-Detection:** Seamlessly switches between QR and 1D Barcode modes.
- **Auto-Zoom:** Intelligent focus on small or distant codes using Google ML Kit.
- **Payload Inspector (New):** Audit raw code content before opening. Detect tracking parameters (UTM, ref, etc.) instantly.
- **Visual Feedback:** Modern scanning animation with real-time feedback.

### ✍️ Advanced Generator
Generate high-quality, branded QR codes for:
- 📝 **Plain Text**
- 🔗 **URLs & Links**
- 📶 **WiFi Networks** (SSID, Password, Encryption)
- 📇 **Contacts** (vCard format with import from phonebook)
- 💰 **Crypto Addresses**
- 📊 **1D Barcodes** (Code 128)

### 🎨 Deep Customization
- **Offline Branding:** Choose Foreground, Background, and Frame colors.
- **Dynamic Frames:** Multiple styles including Rounded, Thick, Modern, and Dotted.
- **Smart Icons:** Automatic center logos for WiFi, V-Cards, and Crypto for professional branding.

### 📤 System Integration
- **Share-to-QR:** Generate codes directly from your browser or other apps via the Android Share sheet.
- **Export Formats:** Save as high-resolution PNG or professional PDF.

---

## 🔒 Privacy First

We believe your data belongs to you. 
- **100% Offline:** No internet permissions requested.
- **Zero Tracking:** No analytics, no ads, no background services.
- **Local Analysis:** The Payload Inspector identifies tracking links locally without external lookups.
- **[Read our full Privacy Policy](docs/Pure-QR_PrivacyPolicy.md)**

---

## 🛠 Tech Stack

- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Compose Navigation
- **Camera:** CameraX API
- **ML Engine:** Google ML Kit Barcode Scanning
- **Generation Engine:** ZXing (Zebra Crossing)
- **Storage:** Jetpack DataStore (Preferences)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Developed with ❤️ for a cleaner, safer mobile experience.
