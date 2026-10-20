// Root build: the Android app plus the pure-Kotlin jyotish engine as an included build.
// The engine is a separate build so it stays free of Android and can move to Kotlin
// Multiplatform for iOS later (ADR 0001).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AstroLingo"

includeBuild("engine")
includeBuild("learn")
include(":app")
include(":notify")
include(":account") // Google sign-in and the updates opt-in (ADR 0012)
include(":widget") // the home-screen widget (ADR 0013)
