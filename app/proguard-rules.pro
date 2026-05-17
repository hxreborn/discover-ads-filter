# libxposed:api is compileOnly so its consumer ProGuard rules are NOT applied here.
# All entry-point pinning must live in this file.

# LSPosed loads the module entry from META-INF/xposed/java_init.list. Without
# -adaptresourcefilecontents, R8 renames the class but the list keeps the original
# name and the module fails to load silently.
-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Hook implementations are dispatched reflectively by libxposed via Chain.
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker {
    public java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}

-dontwarn io.github.libxposed.api.**

# kotlinx-serialization plugin ships consumer rules for $$serializer classes and
# Companion.serializer(); these attributes are still needed at runtime.
-keepattributes RuntimeVisibleAnnotations,InnerClasses,EnclosingMethod,Signature

# AGP 9.x's proguard-android-optimize.txt no longer keeps these.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
