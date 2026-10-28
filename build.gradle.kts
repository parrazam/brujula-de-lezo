// AGP 9 integra Kotlin (built-in Kotlin) y arrastra su propia versión de KGP en tiempo de
// ejecución. Fijarla aquí a `libs.versions.kotlin` mantiene alineados el compilador, el plugin
// de Compose y los módulos `kotlin.jvm`; sin esto ganaría la versión que traiga AGP.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
