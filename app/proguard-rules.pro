-keep class com.lewishadden.flighttracker.data.api.dto.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.lewishadden.flighttracker.**$$serializer { *; }
-keepclassmembers class com.lewishadden.flighttracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.lewishadden.flighttracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
