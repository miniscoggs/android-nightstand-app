plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
}

spotless {
    // The default GIT_ATTRIBUTES policy hashes locked files under .gradle/ and breaks the configuration cache.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    // Targets avoid walking the root, which races lint writing under app/build.
    kotlin {
        target(
            fileTree("app/src") {
                include("**/*.kt")
            }
        )
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("build.gradle.kts", "settings.gradle.kts", "app/build.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}
