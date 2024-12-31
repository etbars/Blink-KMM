# Keep shared models
-keep class com.example.alertapp.models.** { *; }
-keep class com.example.alertapp.network.** { *; }
-keep class com.example.alertapp.services.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Keep Enum Values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Alert Processors
-keep class * extends com.example.alertapp.services.processors.AlertProcessor {
    public protected *;
}

# Keep Platform-Specific Implementations
-keep class * implements com.example.alertapp.services.processors.LocationProvider {
    public protected *;
}

# Keep Serializable Classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep API Response Models
-keepclassmembers class com.example.alertapp.network.ApiResponse$* {
    *;
}

# Keep WeatherData and Related Classes
-keep class com.example.alertapp.models.WeatherData { *; }
-keep class com.example.alertapp.models.WeatherConditionRule { *; }
-keep class com.example.alertapp.models.WeatherMetric { *; }
-keep class com.example.alertapp.models.WeatherOperator { *; }

# Keep Alert Models
-keep class com.example.alertapp.models.Alert { *; }
-keep class com.example.alertapp.models.AlertTrigger { *; }
-keep class com.example.alertapp.models.AlertType { *; }
-keep class com.example.alertapp.models.AlertStatus { *; }

# Keep Config Models
-keep class com.example.alertapp.models.config.** { *; }
