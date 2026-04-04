plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

dependencies {
    implementation(project(":shared"))

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Ktor Client (for outgoing API calls to Google Places / Yelp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)

    // Logging
    implementation(libs.logback.classic)
    
    // Dotenv for environment variables
    implementation(libs.dotenv)
}

application {
    mainClass.set("org.bytestorm.bitesync.server.ApplicationKt")
}
