// Account module: Google sign-in (Credential Manager + Firebase Auth) and the user's choice of updates (ADR 0012).
// Kept apart from the app so sign-in can be reused by later modules and tested on its own.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Firebase settings from gradle.properties (or -P / ORG_GRADLE_PROJECT_ variables). Empty until the PO's project exists. */
fun firebase(name: String): String = providers.gradleProperty("astrolingo.firebase.$name").getOrElse("")

android {
    namespace = "com.astrolingo.account"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        // Public identifiers, not secrets: they ship inside every APK. Security comes from App Check and Firestore rules.
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"${firebase("webClientId")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebase("apiKey")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebase("appId")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebase("projectId")}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.testLogging {
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showCauses = false
                    showStackTraces = false
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)

    // Sign in with Google through Credential Manager, then a Firebase account for Firestore and Sitara (ADR 0004).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
}
