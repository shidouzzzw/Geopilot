plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CI kalıcı imzalama anahtarını secret olarak verir; anahtar yoksa normal debug anahtarı kullanılır.
// Aynı anahtar kullanılmazsa Android yeni APK'yı güncelleme olarak kabul etmez (INSTALL_FAILED_UPDATE_INCOMPATIBLE).
val stableKeystore: File? = System.getenv("ERKGPS_KEYSTORE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }
    ?.takeIf { it.exists() }
val stableStorePassword: String = System.getenv("ERKGPS_STORE_PASSWORD").orEmpty()
val stableKeyAlias: String = System.getenv("ERKGPS_KEY_ALIAS").orEmpty().ifEmpty { "erkgps" }
val stableKeyPassword: String = System.getenv("ERKGPS_KEY_PASSWORD").orEmpty().ifEmpty { stableStorePassword }

android {
    namespace = "com.ymcgps.geopilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ymcgps.geopilot"
        // Android 8.0: uyarlanabilir simge, bildirim kanalları ve modern konum API'leri için yeterli taban.
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    signingConfigs {
        if (stableKeystore != null) {
            create("stable") {
                storeFile = stableKeystore
                storePassword = stableStorePassword
                keyAlias = stableKeyAlias
                keyPassword = stableKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Sabit anahtar varsa debug derlemesi de onunla imzalanır; sürümler birbirinin üzerine kurulabilir.
            signingConfigs.findByName("stable")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // Bu uygulamanın amacı resmî sahte konum (mock location) mekanizmasını kullanmaktır.
        disable += "MockLocation"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // enableEdgeToEdge() androidx.activity 1.8+ ile geldi; appcompat tek başına getirmez.
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.android.material:material:1.12.0")
    // OpenStreetMap haritası: ücretsiz, açık kaynak, API anahtarı gerektirmez.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    testImplementation("junit:junit:4.13.2")
}
