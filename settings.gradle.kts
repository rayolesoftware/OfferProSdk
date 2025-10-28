pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "OfferProSdk"

include(":offerpro-sdk")
project(":offerpro-sdk").projectDir = file("offerpro_sdk") // folder on disk
