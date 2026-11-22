# Striders Howl - Production R8 / ProGuard Rules

# Preserve line numbers and source filenames for Play Console crash symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations used by Firestore, Moshi, Retrofit, and Room
-keepattributes RuntimeVisibleAnnotations,Signature,InnerClasses,EnclosingMethod

# Keep all Firestore @Keep data models and Kotlin data classes
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.model.** { *; }

# Keep Moshi DTOs and generated adapters
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.data.integrations.**JsonAdapter { *; }
-keep class com.example.data.integrations.**Dto { *; }
-keep class com.example.data.integrations.**Response { *; }

# Keep Retrofit service interfaces and method signatures
-keepinterface com.example.data.integrations.*ApiService
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Room entities and DAOs
-keep class com.example.data.local.**Entity { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
