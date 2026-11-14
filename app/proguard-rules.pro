# TailorBook ProGuard Rules

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Room POJOs, Relations, and Models
-keepclassmembers class * {
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Ignore *;
}
-keep class com.tailorbook.feature.orders.data.local.entity.** { *; }
-keep class com.tailorbook.feature.customers.data.local.entity.** { *; }
-keep class com.tailorbook.feature.measurements.data.local.entity.** { *; }
-keep class com.tailorbook.feature.payments.data.local.entity.** { *; }
-keep class com.tailorbook.feature.gallery.data.local.entity.** { *; }
-keep class com.tailorbook.feature.dashboard.domain.model.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines
-dontwarn kotlinx.coroutines.**

# Google Mobile Ads & UMP SDK
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep interface com.google.android.ump.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# ZXing QR Code Engine
-keep class com.google.zxing.** { *; }

# Keep Room DAOs & Database
-keep interface * extends androidx.room.Dao { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Network, Device, and DataStore Domain Models
-keep class com.tailorbook.core.device.** { *; }
-keep class com.tailorbook.core.network.** { *; }
-keep class com.tailorbook.core.billing.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}

# Strip debug Timber logs in release bytecode for performance & privacy
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}


