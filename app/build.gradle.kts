import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun localBuildValue(propertyName: String, environmentName: String): String {
    return localProperties.getProperty(propertyName)?.trim().orEmpty()
        .ifEmpty { System.getenv(environmentName)?.trim().orEmpty() }
}

fun asJavaStringLiteral(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "\\n")
    return "\"$escaped\""
}

android {
    namespace = "com.jxguo92.mykarooextension"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.jxguo92.mykarooextension"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "QWEATHER_API_HOST",
            asJavaStringLiteral(localBuildValue("qweather.apiHost", "QWEATHER_API_HOST")),
        )
        buildConfigField(
            "String",
            "QWEATHER_API_KEY",
            asJavaStringLiteral(localBuildValue("qweather.apiKey", "QWEATHER_API_KEY")),
        )
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
    testImplementation(libs.json)
}
