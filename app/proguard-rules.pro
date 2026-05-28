# Preserve stack trace line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data classes — keep field names so copy() and serialisation work
-keep class com.example.planespotter.data.** { *; }

# Kotlin metadata (needed by reflection-based libraries)
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# OkHttp / Okio (used by Coil under the hood)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coil
-dontwarn coil.**

# Compose — consumer rules are bundled with the library, nothing extra needed here
# but suppress any leftover warnings from internal Compose tooling classes
-dontwarn androidx.compose.ui.tooling.**
