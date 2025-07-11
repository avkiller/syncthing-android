import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    id("com.android.application")
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    androidTestImplementation(libs.annotation)
    androidTestImplementation(libs.rules)
    implementation(libs.aboutlibraries.compose.m2)
    implementation(libs.aboutlibraries.core)
    implementation(libs.activity.compose)
    implementation(libs.activity.ktx)
    implementation(libs.android.material)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.dagger)
    implementation(libs.documentfile)
    implementation(libs.fragment.ktx)
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.jbcrypt)
    implementation(libs.libsuperuser)
    implementation(libs.lingala.zip4j)
    implementation(libs.localbroadcastmanager)
    implementation(libs.preference.ktx)
    implementation(libs.recyclerview)
    implementation(libs.stream)
    implementation(libs.volley)
    implementation(libs.zxing.android.embedded) { isTransitive = false }
    implementation(libs.zxing.core)
    kapt(libs.dagger.compiler)
}

android {
    val ndkVersionShared = rootProject.extra.get("ndkVersionShared")
    val versionMajor: kotlin.Int by rootProject.extra
    val versionMinor: kotlin.Int by rootProject.extra
    val versionPatch: kotlin.Int by rootProject.extra
    val versionWrapper: kotlin.Int by rootProject.extra

    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "${ndkVersionShared}"

    namespace = "com.nutomic.syncthingandroid"

    buildFeatures {
        compose = true
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.fireworld.syncthing"
        minSdk = 21
        targetSdk = 36
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionWrapper
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}.${versionWrapper}"
        testApplicationId = "com.fireworld.syncthing.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = System.getenv("SYNCTHING_RELEASE_STORE_FILE")?.let(::file)
            storePassword = System.getenv("SIGNING_PASSWORD")
            keyAlias = System.getenv("SYNCTHING_RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.runCatching { getByName("release") }
                .getOrNull()
                .takeIf { it?.storeFile != null }
        }
        create("gplay") {
            initWith(getByName("release"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }


    packaging {
        jniLibs {
            // Otherwise libsyncthing.so doesn't appear where it should in installs
            // based on app bundles, and thus nothing works.
            useLegacyPackaging = true
        }
    }
    lint {
        abortOnError = true

        disable += "ExpiringTargetSdkVersion"
        disable += "ExpiredTargetSdkVersion"
    }
}

/**
 * Some languages are not supported by Google Play, so we ignore them.
 */
tasks.register<Delete>("deleteUnsupportedPlayTranslations") {
    delete(
            "src/main/play/listings/el-EL/",
            "src/main/play/listings/en/",
            "src/main/play/listings/eu/",
            "src/main/play/listings/nb/",
            "src/main/play/listings/nl_BE/",
            "src/main/play/listings/nl-BE/",
            "src/main/play/listings/nn/",
            "src/main/play/listings/ta/",
    )
}

project.afterEvaluate {
    android.buildTypes.forEach {
        val capitalizedName = it.name.replaceFirstChar { ch -> ch.uppercase() }
        tasks.named("merge${capitalizedName}JniLibFolders") {
            dependsOn(":syncthing:buildNative")
        }
    }
}
