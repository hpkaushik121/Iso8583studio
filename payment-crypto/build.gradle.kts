plugins {
    id("java-library")
    alias(libs.plugins.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
    sourceSets {
        dependencies {
            api(project(":core-crypto"))
        }
    }
}
