plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.brujuladelezo.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    // Con el Kotlin integrado de AGP 9, jvmTarget sale de targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
