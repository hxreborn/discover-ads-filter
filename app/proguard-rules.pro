# libxposed API 101 entry point - loaded reflectively via META-INF/xposed/java_init.list.
# -adaptresourcefilecontents rewrites the list if the class is renamed; -keep below pins both
# name and lifecycle members so LSPosed can instantiate it and dispatch callbacks.
-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onModuleLoaded(...);
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}
-keep class eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule { *; }

# Hooker implementations are invoked reflectively via module.hook(...).intercept(hooker).
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }

# kotlinx-serialization: $$serializer companions are discovered reflectively from
# ResolvedTargets.serializer(). Without these rules, release builds throw SerializationException
# when the hooked process decodes cached fingerprints.
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

# DexKit: bundled library; belt-and-suspenders on top of its own consumer rules.
-keep class org.luckypray.dexkit.** { *; }
-dontwarn org.luckypray.dexkit.**

# libxposed API is compileOnly - do not warn when its classes aren't on the classpath.
-dontwarn io.github.libxposed.api.**

-keepattributes SourceFile, LineNumberTable
