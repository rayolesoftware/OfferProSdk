plugins {
    id("com.android.library") version "8.6.1" apply false
    kotlin("android") version "2.0.20" apply false
}
allprojects {
    group = "com.github.<YourGitHubUserOrOrg>"   // JitPack convention
    version = "0.0.0-SNAPSHOT"                   // actual versions come from git tags
}
