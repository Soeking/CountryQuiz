
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("MainKt")
//    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

// web モジュールの wasm ビルド結果を resources/web として同梱する
val webDistribution by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val copyWebDistribution by tasks.registering(Sync::class) {
    from(webDistribution) { into("web") }
    into(layout.buildDirectory.dir("generated/webResources"))
}

sourceSets.main {
    resources.srcDir(copyWebDistribution)
}

dependencies {
    implementation(project(":shared"))
    webDistribution(project(path = ":web", configuration = "webDistribution"))

    implementation(ktorLibs.network.tls)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.autoHeadResponse)
    implementation(ktorLibs.server.cachingHeaders)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.resources)
    implementation(ktorLibs.server.sessions)
    implementation(ktorLibs.server.sse)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.websockets)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2database.h2)
    implementation(libs.h2database.r2dbc)
    implementation(libs.logback.classic)
    implementation(libs.sqlite.jdbc)
    implementation(libs.ucasoft.ktorSimpleCache)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
