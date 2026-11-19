import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

ksp {
    // Veritabanı şeması her sürümde app/schemas altına yazılır; ileride güvenli geçiş (migration) için.
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "app.kairos"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.kairos"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
        // Uygulama içi güncelleme: yeni sürümlerin yayınlandığı GitHub deposu (sahip/ad).
        buildConfigField("String", "UPDATE_REPO", "\"beratoprak/kairos\"")
    }

    // Yayın imzası: anahtar ve şifresi proje dışında, ~/.kairos-keys/ altında (git'e ve iCloud'a girmez).
    // Dosya yoksa release derlemesi imzasız çıkar; debug derlemesi etkilenmez.
    val keyProps = Properties().apply {
        val f = file("${System.getProperty("user.home")}/.kairos-keys/keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keyProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
                storeType = "PKCS12"
                // v2: hızlı ve tam APK bütünlüğü; v3: ileride anahtar değişimine (rotation) izin verir.
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

    testOptions {
        // Ekran görüntüsü testleri (Roborazzi + Robolectric) kaynakları ve yazı tiplerini okuyabilsin.
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    outputDir.set(file("screenshots"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
