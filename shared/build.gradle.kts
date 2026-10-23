plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // server と同じ JDK でビルドする（新しい JDK でビルドすると server の実行時に読み込めない）
    jvmToolchain(21)

    // server（JVM）と web（WasmJs）の両方から参照できるようにする
    jvm()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(ktorLibs.serialization.kotlinx.json)
            }
        }
    }
}