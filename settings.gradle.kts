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
rootProject.name = "OfferProSdk"

// keep your sample app if you have it
//include(":app")

// Map module name → folder name (your folder is underscore)
include(":offerpro-sdk")
project(":offerpro-sdk").projectDir = file("app")
