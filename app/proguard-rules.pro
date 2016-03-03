# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/kowlgi/Downloads/adt-bundle-linux-x86_64-20131030/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# https://github.com/ocpsoft/prettytime
-keep class org.ocpsoft.prettytime.i18n.**

# https://github.com/square/okio/issues/60
-dontwarn okio.**

# Allow obfuscation of android.support.v7.internal.view.menu.**
# to avoid problem on certain Android 4.2.2/4.4.2 devices
# see https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.view.menu.**,!android.support.design.internal.NavigationMenu,!android.support.design.internal.NavigationMenuPresenter,!android.support.design.internal.NavigationSubMenu,android.support.** {*;}


# Fabric: We’ve made it simple to set up ProGuard and Dexguard to automatically upload your
# mapping.txt file to de-obfuscate your crash reports. Add the following line to your
# ProGuard configuration file:
-keepattributes SourceFile,LineNumberTable,*Annotation*
