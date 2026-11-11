import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    // BidiSpoofing se desactiva porque el detector de lint de AGP 9.3 revienta
    // con JDK 21 (NoSuchMethodError en List.removeLast) y tumba el análisis
    // completo. El proyecto no tiene textos bidireccionales, así que el chequeo
    // no aporta nada acá.
    lint { disable += "BidiSpoofing" }
    // Necesario para inflar layouts en las pruebas JVM (Robolectric).
    testOptions { unitTests.isIncludeAndroidResources = true }
    namespace = "com.android.sistemabodegaappandroid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.android.sistemabodegaappandroid"
        minSdk = 30
        targetSdk = 37
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // La firma de la entrega vive en local.properties (no versionado), igual que
    // sdk.dir: el repositorio no lleva la clave privada. Sin esas propiedades el
    // build de release sigue compilando, solo que sin firmar.
    signingConfigs {
        create("entrega") {
            val props = Properties()
            val archivo = rootProject.file("local.properties")
            if (archivo.exists()) {
                archivo.inputStream().use { props.load(it) }
            }
            val ruta: String? = props.getProperty("release.storeFile")
            if (!ruta.isNullOrBlank()) {
                storeFile = file(ruta)
                storePassword = props.getProperty("release.storePassword")
                keyAlias = props.getProperty("release.keyAlias")
                keyPassword = props.getProperty("release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (signingConfigs.getByName("entrega").storeFile != null) {
                signingConfigs.getByName("entrega")
            } else {
                null
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.lifecycle.livedata)
    implementation(libs.security.crypto)
    implementation(libs.zxing.embedded)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}