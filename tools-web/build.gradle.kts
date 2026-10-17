import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

// The browser build of the calculator tools: a Kotlin/Wasm executable that renders the
// shared :tools-ui screens with Compose and is served from iso8583.studio/app/.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "iso8583-tools"
        browser {
            commonWebpackConfig {
                outputFileName = "iso8583-tools.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":tools-ui"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(libs.material.icons)
            implementation(libs.kotlinx.browser)
            implementation(compose.components.resources)
        }
    }
}

// Bundled IBM Plex fonts (tools-web/fonts/README.md). The generated class lives outside the
// backticked `in` package so the generator emits plain Kotlin.
compose.resources {
    packageOfResClass = "iso8583studio.web.res"
    generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
}

// Production bundle -> web/public/app (gitignored). Angular copies public/** into its dist,
// so the site serves the app at /app/. Sync removes stale content-hashed files.
val copyWebDist by tasks.registering(Sync::class) {
    group = "web"
    description = "Builds the production Wasm bundle and copies it into web/public/app"
    from(tasks.named("wasmJsBrowserDistribution"))
    into(rootProject.layout.projectDirectory.dir("web/public/app"))
}
