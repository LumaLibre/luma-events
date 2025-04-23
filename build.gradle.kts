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
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://maven.devs.beer/")
    //maven("https://repo.ronanplugins.com/releases"): down
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.codemc.io/repository/EvenMoreFish/")
    maven("https://nexus.neetgames.com/repository/maven-releases/")
    maven("https://repo.olziedev.com/")
}


dependencies {
    compileOnly("com.ghostchu:quickshop-bukkit:6.2.0.9-RELEASE-1")
    compileOnly("com.ghostchu:quickshop-api:6.2.0.9-RELEASE-1")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.1.0")
    compileOnly("com.github.Zrips:jobs:v4.17.2")
    compileOnly("com.dre.brewery:BreweryX:3.4.10")
    compileOnly("com.ronanplugins:BetterRTP:3.6.13")
    compileOnly("com.discordsrv:discordsrv:1.28.0")
    compileOnly("com.oheers.evenmorefish:even-more-fish-plugin:2.0.0-SNAPSHOT")
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.030")
    compileOnly("com.github.NuVotifier:NuVotifier:2.7.2")
    compileOnly("com.olziedev:playerwarps-api:7.7.1")
    compileOnly("dev.jsinco.luma.lumacore:LumaCore:568ff58")
    compileOnly("dev.jsinco.luma.lumaitems:LumaItems:e32431b")
    compileOnly("dev.jsinco.lumaglowapi:LumaGlowAPI:3cb670d")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("me.libraryaddict.disguises:libsdisguises:11.0.0")
    compileOnly("me.hexedhero.pp:PinataParty:2.67.11")




    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
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