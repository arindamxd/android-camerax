# Keep line numbers for Crashlytics / Play Console deobfuscation.
# CameraX, FileProvider, and Crashlytics ship their own consumer keep rules —
# do not add package-wide -keep rules here (they block R8 shrink/optimize/obfuscate).
-keepattributes SourceFile,LineNumberTable,InnerClasses,Signature,*Annotation*
-renamesourcefileattribute SourceFile
