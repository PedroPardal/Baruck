# Keep Firestore data model classes (no-arg constructors + fields)
-keepclassmembers class com.pedro.baruck.data.** {
    <init>();
    <fields>;
}
-keep class com.pedro.baruck.data.** { *; }
-keepattributes Signature,*Annotation*
