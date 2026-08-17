# Ktor & Netty Keep Rules
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class io.netty.** { *; }
-keepclassmembers class io.netty.** { *; }

# Suppress missing optional references for Netty & Ktor
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
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

# LocalSend & Share Core Models (Gson reflection)
-keep class com.ost.application.core.share.** { *; }
-keepclassmembers class com.ost.application.core.share.** { *; }
