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
            implementation("org.jetbrains.kotlin:kotlin-script-util:1.8.22")
            implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.21")
            implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
            implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
            implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
            implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.19.0")
            implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")
            implementation("io.ktor:ktor-client-cio-jvm:3.1.3")
            implementation("io.ktor:ktor-client-auth-jvm:3.1.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.3")
            implementation("io.ktor:ktor-client-logging-jvm:3.1.3")
            implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
            implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta02")
            implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta02")
            implementation("com.google.code.gson:gson:2.13.2")
            implementation(project(":cryptocalc"))
            implementation(project(":iso-core-lib"))
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

        jvmArgs += listOf(
            "-Dfile.encoding=UTF-8",
            "-Dsun.java2d.d3d=false",
            "-Dsun.java2d.opengl=false",
            "-Djava.awt.headless=false",
            "-Dapple.awt.application.appearance=system"
        )

        buildTypes.release.proguard {
            isEnabled.set(true)
            obfuscate.set(true)
            configurationFiles.from(project.file("proguard-rules.pro"))


            // Only packaged builds get the release marker
//            jvmArgs += listOf(
//                "-Dlicense.release=true",
//                "-Dlicense.server.url=https://license.iso8583.studio"
//            )


        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            includeAllModules = true
            packageName = "ISO8583Studio"
            packageVersion = "1.0.1"
            description = "ISO8583 Message Processing Studio"
            copyright = "© 2025 AICortex. All rights reserved."
            vendor = "AICortex"

            windows {
                iconFile.set(project.file("resources/windows/app.ico"))
                menuGroup = packageName
                upgradeUuid = "18159995-d967-4cd2-8885-77bfa97cfa9f"
                dirChooser = true
                perUserInstall = true
                shortcut = true
            }

            macOS {
                iconFile.set(project.file("resources/mac/app.icns"))
                bundleID = "in.aicortex.iso8583studio"
                appCategory = "public.app-category.developer-tools"
                dmgPackageVersion = packageVersion
                signing {
                    sign.set(false)
                }
            }


            linux {
                iconFile.set(project.file("resources/linus/app.png"))
                shortcut = true
                debMaintainer = "support@aicortex.in"
                menuGroup = "Development"
            }
        }
    }
}
