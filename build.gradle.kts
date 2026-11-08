plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
}

spotless {
    // The default GIT_ATTRIBUTES policy hashes locked files under .gradle/ and breaks the configuration cache.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", ".gradle/**", ".kotlin/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", ".gradle/**", ".kotlin/**")
        ktlint(libs.versions.ktlint.get())
    }
}
