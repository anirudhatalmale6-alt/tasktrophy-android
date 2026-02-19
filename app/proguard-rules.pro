-ignorewarnings

# Health Connect
-keep class androidx.health.connect.** { *; }
-keep class androidx.health.platform.** { *; }

# Kotlin coroutines (needed for Health Connect)
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep StepKingBridge JS interface methods
-keepclassmembers class com.webviewgold.myappname.StepKingBridge {
    @android.webkit.JavascriptInterface <methods>;
}