plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library")    version "8.6.1" apply false
    kotlin("android")            version "2.0.20" apply false
}

allprojects {
    group = "com.github.rayolesoftware" // for JitPack
    version = "0.0.0-SNAPSHOT"           // JitPack uses your git tag version
}
