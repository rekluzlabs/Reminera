# Reminera — R8 / ProGuard Rules
# ====================================================================

# ── Room ───────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-dontwarn android.arch.persistence.room.paging.**

-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keep @androidx.room.TypeConverters class *

-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Room generates synthetic classes that must be preserved.
-keep class **.Room* { *; }
-keep class **.*Dao_** { *; }
-keep class **.*Database_** { *; }

# ── Reminera Data Layer ────────────────────────────────────────────
-keep class com.rekluzlabs.reminera.data.** { *; }

# ── Kotlin Coroutines ──────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Compose ────────────────────────────────────────────────────────
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <methods>;
    @androidx.compose.runtime.Immutable <methods>;
}
-dontwarn androidx.compose.**

# ── General ────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# Keep enum classes (valueOf / name used by Converters)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep ViewModel constructors invoked by reflection
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}