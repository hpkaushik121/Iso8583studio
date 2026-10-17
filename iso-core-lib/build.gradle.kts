import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared ISO 8583 / EMV utility types. Kotlin Multiplatform so the same code runs in the
// desktop app (JVM) and in the browser tools (Kotlin/Wasm).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.pluginSerialization)
}

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Node lets the known-answer tests run without a browser (wasmJsNodeTest).
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
