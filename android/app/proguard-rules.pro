-keep class com.xbh**
-keepclassmembers class com.xbh** {*;}

-keep class com.seewo**
-keepclassmembers class com.seewo** {*;}

# Keep multidisplay classes
-keep class com.ifpdos.multidisplay.** { *; }
-keep interface com.ifpdos.multidisplay.** { *; }

# Keep specific classes that are missing
-keep class com.ifpdos.multidisplay.MultiDisplayApi { *; }
-keep class com.ifpdos.multidisplay.api.IDisplayDeviceManager { *; }

# Don't warn about missing optional dependencies
-dontwarn com.ifpdos.multidisplay.**
-dontwarn com.ifpdos.multidisplay.MultiDisplayApi
-dontwarn com.ifpdos.multidisplay.api.IDisplayDeviceManager

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Additional rules for R8
-ignorewarnings