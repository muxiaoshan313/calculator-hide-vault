-optimizationpasses 7
-dontskipnonpubliclibraryclassmembers
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-obfuscationdictionary dw_proguard.txt
-classobfuscationdictionary dw_proguard.txt
-packageobfuscationdictionary dw_proguard.txt

-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

#-keep class androidx.core.util.Consumer