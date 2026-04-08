# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class com.taskfinisher.data.database.** { *; }

# Keep Hilt generated components
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Keep data models (used in serialization/reflection by Room)
-keep class com.taskfinisher.data.model.** { *; }

# Keep WorkManager workers
-keep class com.taskfinisher.notifications.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
