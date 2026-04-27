# libxposed:api is compileOnly so its consumer ProGuard rules are NOT applied here.
# All entry-point pinning must live in this file.

# LSPosed loads the module entry from META-INF/xposed/java_init.list. Without
# -adaptresourcefilecontents, R8 renames the class but the list keeps the original
# name and the module fails to load silently.
-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onModuleLoaded(...);
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}

# Hook implementations are dispatched reflectively by libxposed via Chain.
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }

-dontwarn io.github.libxposed.api.**

# kotlinx-serialization: $$serializer companions are discovered reflectively from
# ResolvedTargets.serializer(). Without these, the hook process throws when
# decoding the cached fingerprint JSON.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,InnerClasses,EnclosingMethod,Signature
-keepclasseswithmembers class eu.hxreborn.discoveradsfilter.discovery.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class eu.hxreborn.discoveradsfilter.discovery.**$$serializer { *; }
-if class eu.hxreborn.discoveradsfilter.discovery.**
-keepclassmembers class <1> {
    public static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# AGP 9.x's proguard-android-optimize.txt no longer keeps these.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
