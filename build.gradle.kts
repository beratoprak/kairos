plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Proje klasörü iCloud ile eşitlenen Masaüstü'nde. Derleme sırasında hızla değişen dosyalar
// iCloud'un "dosya 2" çakışma kopyalarına yol açıp derlemeyi bozuyor; çıktılar iCloud dışında tutulur.
allprojects {
    layout.buildDirectory.set(file("${System.getProperty("user.home")}/.kairos-build/${project.name}"))
}
