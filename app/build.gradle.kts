import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Read local.properties for the AMap key
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val amapKey: String = (localProps.getProperty("AMAP_KEY") ?: "").trim()

android {
    namespace = "com.memoryball.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.memoryball.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "1.0.11"

        // Expose the AMap key to both the manifest placeholder and BuildConfig
        manifestPlaceholders["AMAP_KEY"] = amapKey
        buildConfigField("String", "AMAP_KEY", "\"$amapKey\"")
    }

    // Optional release signing: create `keystore.properties` in the project root
    // (see keystore.properties.example). Without it, release builds are unsigned.
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        val ksProps = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(ksProps.getProperty("storeFile"))
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity / lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    implementation("androidx.core:core-ktx:1.13.1")

    // Navigation (Compose)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // AMap (Gaode) 3D map SDK. Note: 3dmap already bundles the location
    // (AMapLocationClient/APSService/GeoFence) and core utils, so we do NOT add
    // the standalone `location`/`search` artifacts (they cause duplicate classes).
    implementation("com.amap.api:3dmap:10.0.600")

    // Accompanist permissions (Compose-friendly permission handling)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
