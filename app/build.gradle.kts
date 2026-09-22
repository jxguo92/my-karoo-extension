plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jxguo92.mykarooextension"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jxguo92.mykarooextension"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.hammerhead.karoo.ext)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
