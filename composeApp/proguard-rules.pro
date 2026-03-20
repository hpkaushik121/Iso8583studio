# ═══════════════════════════════════════════════════════════════
#  ISO8583Studio ProGuard/R8 Configuration for Release Builds
# ═══════════════════════════════════════════════════════════════

# --- License System: Aggressively obfuscate ---
# Keep entry points but obfuscate everything else
-keep class in.aicortex.iso8583studio.ISO8583Studio {
    public static void main(java.lang.String[]);
}

# Obfuscate all license classes (but keep them functional)
-keepclassmembers class in.aicortex.iso8583studio.license.** {
    <init>(...);
}

# Keep serializable data classes (kotlinx.serialization needs them)
-keepclassmembers class in.aicortex.iso8583studio.license.LicenseStorage$StoredData {
    *;
}
-keep class in.aicortex.iso8583studio.license.LicenseServerClient$ApiResponse { *; }
-keep class in.aicortex.iso8583studio.license.LicenseServerClient$ValidateRequest { *; }
-keep class in.aicortex.iso8583studio.license.LicenseServerClient$TrialRegisterRequest { *; }

# --- Compose Desktop ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- Voyager Navigator ---
-keep class cafe.adriel.voyager.** { *; }

# --- Ktor Client ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class in.aicortex.iso8583studio.**$$serializer { *; }
-keepclassmembers class in.aicortex.iso8583studio.** {
    *** Companion;
}
-keepclasseswithmembers class in.aicortex.iso8583studio.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Java Security (crypto, certificates) ---
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class javax.net.ssl.** { *; }

# --- Netty ---
-dontwarn io.netty.**
-keep class io.netty.** { *; }

# --- Jackson ---
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# --- Exposed ORM ---
-keep class org.jetbrains.exposed.** { *; }
-dontwarn org.jetbrains.exposed.**

# --- General ---
-dontoptimize
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Obfuscation settings
-repackageclasses ''
-allowaccessmodification
-overloadaggressively
