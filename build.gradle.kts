buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.android.gradlePlugin)
    }
}

plugins {
    // Kotlin Multiplatform plugin
    alias(libs.plugins.kotlinMultiplatform).version("1.9.20").apply(false)
    alias(libs.plugins.androidApplication).version("8.1.0").apply(false)
    alias(libs.plugins.androidLibrary).version("8.1.0").apply(false)
    alias(libs.plugins.kotlinSerialization).version("1.9.20").apply(false)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
