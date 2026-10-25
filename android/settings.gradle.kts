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

rootProject.name = "NFCraft"

include(":app")
include(":core-model")
include(":core-nfc")
include(":core-ndef")
include(":core-chip")
include(":core-tag")
include(":feature-tasks")
include(":feature-hce")
include(":feature-profiles")
