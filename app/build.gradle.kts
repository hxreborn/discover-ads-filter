import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eu.hxreborn.discoveradsfilter"
    compileSdk = 36

    defaultConfig {
        applicationId = "eu.hxreborn.discoveradsfilter"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"

        buildConfigField(
            "String",
            "BUILD_TIMESTAMP",
            "\"${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}\"",
        )

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs.useLegacyPackaging = false
        resources {
            // Keep META-INF/xposed/* untouched.
            merges += listOf("META-INF/xposed/**")
            excludes +=
                listOf(
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/DEPENDENCIES",
                    "META-INF/*.version",
                    "META-INF/*.kotlin_module",
                    "kotlin/**",
                    "DebugProbesKt.bin",
                )
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli)

    // LSPosed provides this at runtime.
    compileOnly(libs.libxposed.api)

    // Bind XposedService so the hook process can read prefs.
    implementation(libs.libxposed.service)

    // Verify scans AGSA with DexKit.
    implementation(libs.dexkit)

    // Root shell for reading hook metrics from AGSA cache.
    implementation(libs.libsu.core)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.preferences)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}

val ktlintFormat by tasks.registering(JavaExec::class) {
    group = "formatting"
    description = "Auto-format Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}

tasks.named("preBuild").configure {
    dependsOn(ktlintFormat)
}

tasks.named("check").configure {
    dependsOn(ktlintCheck)
}
