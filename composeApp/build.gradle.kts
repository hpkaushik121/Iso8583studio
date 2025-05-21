import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.pluginSerialization)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.exposed.core)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.crypt)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.json)
            implementation(libs.exposed.spring.boot.starter)
            implementation(libs.sqlite.jdbc)
            implementation(libs.koin.core)
            implementation(libs.netty.all)
            implementation(libs.jaxb.api)
            implementation(libs.serializer)
            implementation(libs.kotlinx.serializer)
            implementation(libs.material.icons)
            implementation(libs.material)
            implementation(libs.serial.comms)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}



compose.desktop {
    application {
        mainClass = "in.aicortex.iso8583studio.ISO8583Studio"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)

            packageName = "ISO8583Studio"
            packageVersion = "1.0.0"
            description = "ISO8583 Message Processing Studio"
            copyright = "© 2025 AICortex. All rights reserved."
            vendor = "AICortex"

            // Windows specific configuration
            windows {
                // Set the icon for Windows
                iconFile.set(project.file("resources/windows/app.ico"))
                // Additional Windows configuration
                menuGroup = packageName
                // Generate a unique UUID for your application (use a UUID generator)
                upgradeUuid = "18159995-d967-4cd2-8885-77bfa97cfa9f"
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menuGroup = packageName
            }

            // macOS specific configuration
            macOS {

                // Set the icon for macOS
                iconFile.set(project.file("resources/mac/app.icns"))
                // Additional macOS settings
                bundleID = "in.aicortex.iso8583studio"
                appCategory = "public.app-category.developer-tools"
                // Configure DMG options if needed
                dmgPackageVersion = packageVersion
                signing {
                    sign.set(false) // Set to true when you have a signing identity for distribution
                    // identity.set("Developer ID Application: YourName (TeamID)")
                }
            }

            // Linux specific configuration
            linux {
                // Set the icon for Linux
                iconFile.set(project.file("resources/linus/app.png"))
                // Additional Linux settings
                shortcut = true
                debMaintainer = "support@aicortex.in"
                menuGroup = "Development"
            }
        }
    }
}
