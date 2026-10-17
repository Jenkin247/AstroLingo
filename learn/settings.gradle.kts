// Standalone build for the Learn module's logic and course content. It can be built on its own
// (`./gradlew -p learn test`) or as part of the app through includeBuild (ADR 0011).
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "learn"
