# libxposed:api:101 ships consumer rules that pin the entry-point class and constructor
# (-adaptresourcefilecontents META-INF/xposed/java_init.list and -keep <init>()).
# Lifecycle overrides still need an explicit keep: the framework dispatches them via vtable
# from outside the module so R8 cannot trace them. Mirrors HyperCeiler/libxposed-api-101.
-keep,allowobfuscation,allowoptimization class * extends io.github.libxposed.api.XposedModule {
    public void onModuleLoaded(...);
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}

-dontwarn io.github.libxposed.api.**

# AGP 9.x's proguard-android-optimize.txt no longer keeps SourceFile/LineNumberTable.
# Useful stack traces in LSPosed module logs.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
