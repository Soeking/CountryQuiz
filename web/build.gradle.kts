import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                // 開発時は 8081 で配信し、/api/** は Ktor server (8080) に転送する
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                    proxy = mutableListOf(
                        KotlinWebpackConfig.DevServer.Proxy(
                            context = mutableListOf("/api"),
                            target = "http://localhost:8080",
                        )
                    )
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        // ブラウザの API（window・File など）を呼ぶ画面があるので、モジュール全体で JS 連携 API を使えるようにする
        all { languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop") }

        wasmJsMain {
            dependencies {
                implementation(project(":shared"))

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)

                implementation(libs.navigation.compose)
                // shared が ktorLibs（3.4.0）を使うので、client も同じバージョンに揃える
                implementation(ktorLibs.client.core)
                implementation(ktorLibs.client.js)
                implementation(ktorLibs.client.contentNegotiation)
                implementation(ktorLibs.serialization.kotlinx.json)
            }
        }
    }
}

// 本番用の wasm ビルド結果を server モジュールに渡すための configuration
val webDistribution by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(webDistribution.name, layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
        builtBy("wasmJsBrowserDistribution")
    }
}
