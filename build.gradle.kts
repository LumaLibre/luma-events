plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm")
}

group = "dev.jsinco.luma.lumaevents"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("dev.jsinco.luma:LumaCore:3751a2f")
    compileOnly("dev.jsinco.luma:LumaItems:c41b8d5")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

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

    shadowJar {
        archiveClassifier.set("")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
kotlin {
    jvmToolchain(21)
}