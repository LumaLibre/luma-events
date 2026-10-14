import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.charset.Charset

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "3.0.1"
    kotlin("jvm")
    kotlin("plugin.lombok") version "2.3.10"
}

group = "dev.lumas.events"
version = commitHash()

repositories {
    mavenCentral()
    maven("https://maven.canvasmc.io/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://mvn.lib.co.nz/public/")
    maven("https://repo.glaremasters.me/repository/towny/") // towny
    maven("https://repo.opencollab.dev/main/") // floodgate

    // temp: trick or treat
    maven("https://repo.nexomc.com/releases")
    maven("https://maven.citizensnpcs.co/repo")
}


dependencies {
    compileOnly("dev.lumas.core:LumaCore:dd53fbc")
    compileOnly("dev.lumas.lumaitems:LumaItems:69ea766")
    compileOnly("dev.lumas.glowapi:LumaGlowAPI:325d91d")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.nuclyon.technicallycoded.inventoryrollback:InventoryRollbackPlus:1.7.3")
    compileOnly("com.github.Zrips:jobs:v4.17.2") {
        isTransitive = false
    }
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.palmergames.bukkit.towny:towny:0.103.1.1")

    //compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("io.canvasmc.canvas:canvas-api:26.2.build.+")
    compileOnly("me.libraryaddict.disguises:libsdisguises:11.0.13") {
        isTransitive = false
    }
    compileOnly("com.github.retrooper:packetevents-api:2.7.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }

    implementation("dev.thorinwasher.schem:schem-reader:1.0.0")
    compileOnly("net.kyori:adventure-nbt")
    implementation("net.objecthunter:exp4j:0.4.8")

    // paper includes configurate and configurate does not get updated that often
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")

    implementation(platform("com.intellectualsites.bom:bom-newest:1.55")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }

    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.047-CUSTOM") {
        isTransitive = false
    }

    compileOnly("com.github.LumaLibre:HiddenArmor-Folia:96bcba105e") // TODO: needs to be on repo.jsinco.dev
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("dev.lumas.sleepy:Sleepy:1.1.0")

    // temp: trick or treat
    compileOnly("com.nexomc:nexo:1.29.1") { isTransitive = false }
    compileOnly(files("libs/Interactions-2.17.3-dev.jar"))
    compileOnly("net.citizensnpcs:citizens-main:2.0.41-SNAPSHOT") { isTransitive = false }
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

        relocate("dev.thorinwasher.schem", "$pack.schem")
        relocate("net.objecthunter.exp4j", "$pack.exp4j")
        exclude("kotlin/**", "net/kyori/**", "org/joml/**")
        //minimize()
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
    }

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(
            mapOf(
                "tokens" to mapOf("version" to project.version.toString()),
                "beginToken" to "\${",
                "endToken" to "}"
            )
        ).filteringCharset = "UTF-8"
    }
}


kotlin {
    jvmToolchain(25)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
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
