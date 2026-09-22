# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Rename hidden source file
-renamesourcefileattribute SourceFile

# Keep app classes
-keep public class com.a3dmodelviewer.** { *; }

# Keep Filament native methods
-keepclasseswithmembernames, includedescriptorclasses class com.google.android.filament.** {
    native <methods>;
}

# Keep Filament reflection markers
-keep class com.google.android.filament.proguard.UsedByNative { *; }
-keep class com.google.android.filament.proguard.UsedByReflection { *; }

# Keep SceneView
-keep class io.github.sceneview.** { *; }
-keepclassmembers class io.github.sceneview.node.ModelNode { *; }