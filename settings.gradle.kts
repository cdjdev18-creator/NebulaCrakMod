rootProject.name = "NebulaCrakMod"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.meteordevelopment.com/") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "fabric-loom") {
                useVersion("1.18.4")
            }
        }
    }
}