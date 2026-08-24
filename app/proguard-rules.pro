# Android & Kotlin Base Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Android Native JavaScript Bridge
-keepclassmembers class com.example.antigravityremote.AndroidNativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.antigravityremote.AndroidNativeBridge { *; }

# AndroidX Compose
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }
-keepclassmembers class androidx.compose.ui.platform.ComposeView { *; }
-dontwarn androidx.compose.**

# ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-dontwarn com.google.mlkit.vision.**

# CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.**

# OkHttp & Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# AndroidX Biometrics
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**
