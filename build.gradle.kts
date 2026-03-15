import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.kotlin.dsl.filter
import java.nio.charset.Charset

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    kotlin("jvm")
    kotlin("plugin.lombok") version "2.3.10"
}

group = "dev.lumas.events"
version = commitHash()

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
    maven("https://mvn.lib.co.nz/public/")
}


dependencies {
    compileOnly("dev.lumas.lumacore:LumaCore:7f54b3b")
    compileOnly("dev.lumas.lumaitems:LumaItems:a5ce560")
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

    implementation(platform("com.intellectualsites.bom:bom-newest:1.55")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }

    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.047-CUSTOM") {
        isTransitive = false
    }
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
        val pack = "dev.lumas.events.lib"

        relocate("eu.okaeri", "$pack.okaeri")
        relocate("dev.thorinwasher.schem", "$pack.schem")
        exclude("kotlin/**", "net/kyori/**", "org/joml/**")
        minimize()
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
    }

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        )).filteringCharset = "UTF-8"
    }
}


kotlin {
    jvmToolchain(21)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

fun commitHash(): String {
    return try {
        val process = ProcessBuilder("git", "log", "-1", "--format=%h")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader(Charset.defaultCharset()).readText().trim()
        result.ifBlank { "none" }
    } catch (_: Exception) {
        "none"
    }
}