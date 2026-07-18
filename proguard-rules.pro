# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimize code and shrink resource/class size safely
-repackageclasses ''
-allowaccessmodification

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$ScheduledRunnable {
    *** run();
}

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp keep rules
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# OkHttp3 keep rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Moshi rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
# Keep Kotlin Codegen-generated adapters
-keep class *JsonAdapter { *; }
-keep @com.squareup.moshi.JsonQualifier public @interface *

# Keep our data entities and models
-keep class com.example.data.** { *; }

# Coil Keep Rules
-dontwarn coil.**
-keep class coil.** { *; }

# Media3 & ExoPlayer Keep Rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
