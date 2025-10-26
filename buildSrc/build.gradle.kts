plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.solo-studios.ca/releases") {
        name = "Solo Studios Releases"
        content {
            excludeGroupByRegex("io\\.papermc(\\..*)?")
        }
    }

    maven("https://maven.solo-studios.ca/snapshots") {
        name = "Solo Studios Snapshots"
        content {
            excludeGroupByRegex("io\\.papermc(\\..*)?")
        }
    }

    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
        content {
            includeGroupByRegex("io\\.papermc(\\..*)?")
        }
    }

    maven("https://repo.codemc.org/repository/maven-public") {
        name = "CodeMC"
    }
}

dependencies {
    //TODO Allow pulling from Versions.kt
    implementation("com.gradleup.shadow", "shadow-gradle-plugin", "8.3.9")

    implementation("io.papermc.paperweight.userdev", "io.papermc.paperweight.userdev.gradle.plugin", "2.0.0-beta.18")
    implementation("org.ow2.asm", "asm", "9.9")
    implementation("org.ow2.asm", "asm-tree", "9.9")
    implementation("com.dfsek.tectonic", "common", "4.3.1")
    implementation("org.yaml", "snakeyaml", "2.5")
}
