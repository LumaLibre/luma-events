plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm")
    kotlin("plugin.lombok") version "2.1.0"
    id("io.freefair.lombok") version "8.10"
}

// TODO: Change package name on next event
group = "dev.jsinco.luma.lumaevents"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://mvn.lib.co.nz/public/")
}


dependencies {
    compileOnly("dev.lumas.lumacore:LumaCore:d56563b")
    compileOnly("dev.lumas.lumaitems:LumaItems:d55b35f")
    compileOnly("dev.lumas.glowapi:LumaGlowAPI:c57567c")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.nuclyon.technicallycoded.inventoryrollback:InventoryRollbackPlus:1.7.3")
    compileOnly("com.github.Zrips:jobs:v4.17.2")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")


    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("me.libraryaddict.disguises:libsdisguises:11.0.13")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }

    implementation("dev.thorinwasher.schem:schem-reader:1.0.0")

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
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
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