# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn com.jeremyliao.liveeventbus.**
-keep class com.jeremyliao.liveeventbus.LiveEventBus { *; }
-keep class androidx.lifecycle.ExternalLiveData { *; }
-dontwarn androidx.lifecycle.LiveData
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.LifecycleRegistry { *; }
-keep class androidx.arch.core.internal.SafeIterableMap { *; }
#-keep class com.example.shadowdemo.constant.Constant {*;}
-dontwarn android.support.annotation.Keep
#保留注解，如果不添加改行会导致我们的@Keep注解失效
-keepattributes *Annotation*

-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient
-keep public class android.webkit.WebView
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient
-keep class com.example.shadowdemo.bean.** { *; } #实体类不参与混淆