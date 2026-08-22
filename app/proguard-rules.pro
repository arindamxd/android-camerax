# CameraX consumer rules already keep the public API. Keep line numbers for Crashlytics
# deobfuscation on Play Console.
-keepattributes SourceFile,LineNumberTable,InnerClasses,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# CameraX / Camera2 interop (reflection + extension modes).
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# FileProvider paths and Compose previews must survive shrinking.
-keep class androidx.core.content.FileProvider { *; }

# Crashlytics
-keep public class * extends java.lang.Exception
