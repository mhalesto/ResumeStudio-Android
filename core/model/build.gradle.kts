plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Deliberately a plain JVM module, not an Android one. The document model and the
// template vocabulary are pure data, so they can be unit-tested on the JVM in
// milliseconds — which is what makes the iOS parity suite cheap enough to run on
// every commit.

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
