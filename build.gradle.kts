plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm")
}

group = "dev.jsinco.luma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://jitpack.io")
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly(files("libs/JetsPrisonMines-4.6.8-rebuild.jar"))
    compileOnly(files("libs/LumaItems.jar"))

    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.5")
    implementation("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.5")
    implementation("com.github.Jsinco:AbstractJavaFileLib:2.2")
    implementation(kotlin("stdlib-jdk8"))
}

java {
}

tasks {

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        dependencies {

        }
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