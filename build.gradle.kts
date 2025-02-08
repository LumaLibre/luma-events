plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm")
    kotlin("plugin.lombok") version "2.1.0"
    id("io.freefair.lombok") version "8.10"
}

group = "dev.jsinco.luma.lumaevents"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.jsinco.dev/releases")
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("dev.jsinco.luma.lumacore:LumaCore:1a54b7d")
    compileOnly("dev.jsinco.luma.lumaitems:LumaItems:7a3405c")
//    compileOnly("org.projectlombok:lombok:1.18.30")
//    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("com.github.Zrips:jobs:v4.17.2")
    compileOnly("dev.jsinco.chatheads:ChatHeads:1.6")

    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.5")
    implementation("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.5")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jar {
        enabled = false
    }

    shadowJar {
        relocate("eu.okaeri", "dev.jsinco.luma.lumaevents.okaeri")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}
kotlin {
    jvmToolchain(21)
}