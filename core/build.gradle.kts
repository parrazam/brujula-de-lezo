// Sin versión: KGP ya está en el classpath del buildscript raíz, fijado a libs.versions.kotlin.
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
