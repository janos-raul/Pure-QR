# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\janos\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserving the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ZXing
-keep class com.google.zxing.** { *; }

# Prevent shrinking of important classes
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.** { *; }

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# Project Models - Keep for JSON serialization and persistence
-keep class com.pureqr.app.model.** { *; }
-keepclassmembers enum com.pureqr.app.model.** { *; }

# AndroidX Navigation
-keepclassmembers class * extends androidx.navigation.Navigator {
    public <init>(...);
}
-keepclassmembers class * extends androidx.navigation.NavigatorProvider {
    public <init>(...);
}
