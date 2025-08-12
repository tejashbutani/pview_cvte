-keep class com.xbh**
-keepclassmembers class com.xbh** {*;}

# Suppress warnings for missing display classes
-dontwarn com.ifpdos.multidisplay.MultiDisplayApi
-dontwarn com.ifpdos.multidisplay.api.IDisplayDeviceManager

# Keep display classes
-keep class com.ifpdos.multidisplay.** { *; }
-keep interface com.ifpdos.multidisplay.** { *; }