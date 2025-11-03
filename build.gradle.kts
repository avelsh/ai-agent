plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application.mainClass.set("ai.koog.agent.findrules.MainKt")

dependencies {
    implementation(libs.kotlin.bom)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    

    implementation(libs.koog.all)

    implementation(libs.logback.classic)
    implementation(libs.oshai.logging)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    standardOutput = System.`out`
    errorOutput = System.err
}

tasks.test {
    useJUnitPlatform()
}
