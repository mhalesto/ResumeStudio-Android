plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Plain JVM, like :core:model. The store takes a File rather than a Context, so
// every persistence rule — the legacy fallback, the atomic write, the iOS date
// format — is provable in a unit test instead of needing a device.

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
