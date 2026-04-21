# LSPosed loads the module entry point from META-INF/xposed/java_init.list.
# Keep the class and let -adaptresourcefilecontents rewrite the list on rename.
-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onModuleLoaded(...);
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}
-keep class eu.hxreborn.discoveradsfilter.DiscoverAdsFilterModule { *; }

# LSPosed resolves hookers reflectively.
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }

# kotlinx-serialization resolves these companions reflectively.
# Without these rules, release builds fail when the hook process decodes cached targets.
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

# Keep DexKit on top of its own consumer rules.
-keep class org.luckypray.dexkit.** { *; }
-dontwarn org.luckypray.dexkit.**

# libxposed is compileOnly.
-dontwarn io.github.libxposed.api.**

-keepattributes SourceFile, LineNumberTable
