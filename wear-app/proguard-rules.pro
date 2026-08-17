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
-dontwarn androidx.compose.**
-keep class com.ost.application.util.GitHubRelease { *; }
-keepclassmembers class com.ost.application.util.GitHubRelease { *; }
#-keep class androidx.compose.** { *; }

# Ktor & Netty Keep Rules
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class io.netty.** { *; }
-keepclassmembers class io.netty.** { *; }

-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**

# Netty / R8 missing classes on Android
-dontwarn io.netty.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn reactor.blockhound.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.**
-dontwarn org.jboss.marshalling.**
-dontwarn org.osgi.annotation.**
-dontwarn sun.security.x509.**

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,MethodParameters

# LocalSend & Share Models
-keep class com.ost.application.core.share.** { *; }
-keepclassmembers class com.ost.application.core.share.** { *; }
-keep class com.ost.application.share.** { *; }
-keepclassmembers class com.ost.application.share.** { *; }