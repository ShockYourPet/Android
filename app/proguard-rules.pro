# Proguard / R8 configuration rules for ShockYourPet

# Keep Data Models and API classes used with Gson / Retrofit
-keep class app.shockyourpet.data.api.models.** { *; }
-keepclassmembers class app.shockyourpet.data.api.models.** { *; }

# Keep Retrofit interface annotations and generic signatures
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers enum * { *; }

# Keep Gson @SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okhttp3.**
-dontwarn retrofit2.**
