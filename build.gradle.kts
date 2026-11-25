plugins {
    id("fabric-loom") version "1.18.4"
    id("java")
}

group = "com.nebulacrak"
version = "1.01_1.21.11"

base {
    archivesName.set("nebulacrakmod")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")
    modImplementation("meteordevelopment:meteor-client:${property("meteor_version")}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to project.property("minecraft_version"),
        "loader_version" to project.property("loader_version"),
        "jdk_version" to "21"
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}