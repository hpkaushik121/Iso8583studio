import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The calculator tool screens shared by the desktop app (JVM) and the browser tools (Wasm).
// A library only: the browser executable lives in :tools-web, the desktop one in :composeApp.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(libs.material.icons)
            implementation(libs.kotlinx.coroutinesCore)
            api(project(":cryptocalc"))
            api(project(":iso-core-lib"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
            }
        }
    }
}
