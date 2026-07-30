plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // server（JVM）と web（WasmJs）の両方から参照できるようにする
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(ktorLibs.serialization.kotlinx.json)
            }
        }
    }
}