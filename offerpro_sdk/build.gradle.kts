plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "com.rayole.offerpro.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-proguard.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { /* optional */ }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()      // <— this creates OfferProSdk-vX.Y.Z-sources.jar
            // withJavadocJar()   // optional; requires Dokka/javadoc setup if you want it
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.browser:browser:1.8.0")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            groupId = project.group.toString()      // e.g., com.github.rayolesoftware
            artifactId = "OfferProSdk"
            version = project.version.toString()
        }
    }
}
