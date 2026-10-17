// Standalone build for the jyotish engine. It can be built on its own
// (`./gradlew -p engine test`) or as part of the app through includeBuild.
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

rootProject.name = "jyotish"
