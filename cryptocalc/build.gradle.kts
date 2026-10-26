import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Payment cryptography engines. Pure Kotlin on every platform: the block ciphers, MACs and
// digests live in io.cryptocalc.crypto.primitives, so the desktop app and the browser tools
// compute identical results. javax.crypto is only referenced from jvmTest as a test oracle.
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
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":iso-core-lib"))
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.kotlincrypto.hash.sha1)
            implementation(libs.kotlincrypto.hash.sha2)
            implementation(libs.kotlincrypto.hash.md)
            implementation(libs.kotlincrypto.macs.hmac.sha1)
            implementation(libs.kotlincrypto.macs.hmac.sha2)
            implementation(libs.kotlincrypto.macs.hmac.md)
            api(libs.bignum)
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
