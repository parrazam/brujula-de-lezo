plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Lee git en tiempo de configuración con providers.exec, la API que soporta el configuration
// cache. ignoreExitValue evita que la build falle sin git o sin historial; se comprueba el
// exitValue antes de dar por buena la salida.
fun gitOutput(vararg args: String): Provider<String> = providers.exec {
    commandLine("git", *args)
    isIgnoreExitValue = true
}.let { result ->
    result.result.zip(result.standardOutput.asText) { execResult, output ->
        if (execResult.exitValue == 0) output.trim() else ""
    }
}

val gitVersionName: String = gitOutput("describe", "--tags", "--always")
    .map { it.removePrefix("v").ifEmpty { "dev" } }
    .getOrElse("dev")

// versionCode = major*1_000_000 + minor*10_000 + patch*100 + commits desde la etiqueta (tope 99).
// Sale de `git describe --tags --long` ("v1.2.0-6-g3d017ee") y no de `rev-list --count`, que baja
// tras un rebase y haría que Play rechazase el AAB. v1.1.0 da 1_010_000, por encima del
// versionCode 2 que se publicó a mano.
fun versionCodeFromDescribe(describe: String): Int {
    val m = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-(\d+)-g[0-9a-f]+)?$""").find(describe) ?: return 1
    val (major, minor, patch, ahead) = m.destructured
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 100 +
        (ahead.toIntOrNull() ?: 0).coerceAtMost(99)
}

val gitVersionCode: Int = gitOutput("describe", "--tags", "--long", "--match", "v[0-9]*")
    .map { versionCodeFromDescribe(it) }
    .getOrElse(1)

println("[brujula-de-lezo] versionCode=$gitVersionCode versionName=$gitVersionName")

android {
    namespace = "com.brujuladelezo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.brujuladelezo"
        minSdk = 26
        targetSdk = 36
        versionCode = gitVersionCode
        versionName = gitVersionName
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore/release.jks")
            if (keystoreFile.exists()) {
                val storePass = System.getenv("KEYSTORE_PASSWORD") ?: ""
                val keyPass = System.getenv("KEY_PASSWORD") ?: storePass
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = System.getenv("KEY_ALIAS") ?: "brujula-de-lezo"
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Con el Kotlin integrado de AGP 9, jvmTarget sale de targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))
    implementation(project(":design-system"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
