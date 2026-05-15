package dev.lumas.events.explorer.order

import dev.lumas.events.EventMain
import dev.lumas.events.manager.EventPlayerManager
import dev.lumas.events.suspend.SuspendedWorldBiomeService
import me.outspending.biomesapi.biome.CustomBiome
import me.outspending.biomesapi.registry.BiomeResourceKey
import me.outspending.biomesapi.renderer.packet.PacketHandler
import me.outspending.biomesapi.renderer.packet.data.BlockReplacement
import me.outspending.biomesapi.renderer.packet.data.PhonyCustomBiome
import me.outspending.biomesapi.wrapper.BiomeSettings
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttributeMap
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttributes
import me.outspending.biomesapi.wrapper.environment.particle.ParticleCatalog
import me.outspending.biomesapi.wrapper.environment.particle.WrappedParticleTypes
import org.bukkit.Material

class PaleSideBiome(
    val key: BiomeResourceKey
) {

    companion object {
        const val DEFAULT_NAMESPACE = "explorer"
        private val CONFIG = EventMain.getOkaeriConfig()

        fun of(key: String) = PaleSideBiome(key)
    }

    constructor(key: String) : this(BiomeResourceKey.of(DEFAULT_NAMESPACE, key))

    var settings: BiomeSettings = BiomeSettings.defaultSettings()
    var fogColor: String = "#FFFFFF"
    var foliageColor: String = "#F5F2EB"
    var skyColor: String = "#FFFFFF"
    var waterColor: String = "#000000"
    var waterFogColor: String = "#000000"
    var grassColor: String = "#FAF5EA"
    var dryFoliageColor: String = "#FFFFFF"
    var blockReplacements: Map<Material, Material> = mapOf(
        Material.SAND to Material.WHITE_CONCRETE_POWDER,
        Material.RED_SAND to Material.WHITE_CONCRETE_POWDER,
        Material.GRAVEL to Material.WHITE_CONCRETE_POWDER,
        Material.BIRCH_LEAVES to Material.ACACIA_LEAVES,
        Material.SPRUCE_LEAVES to Material.WHITE_STAINED_GLASS,
        Material.SPRUCE_LOG to Material.STRIPPED_PALE_OAK_LOG
    )
    var attributes: WrappedEnvironmentAttributeMap = WrappedEnvironmentAttributeMap.builder()
        .setAttribute(WrappedEnvironmentAttributes.SKY_LIGHT_COLOR, "#FFE4E4")
        .build()
    var particles: ParticleCatalog = ParticleCatalog.builder()
        .addSimple(WrappedParticleTypes.SMOKE, 0.001f)
        .build()

    var biome: CustomBiome? = null
    var phonyBiome: PhonyCustomBiome? = null


    fun settings(settings: BiomeSettings) = apply { this.settings = settings }
    fun fogColor(color: String) = apply { this.fogColor = color }
    fun foliageColor(color: String) = apply { this.foliageColor = color }
    fun skyColor(color: String) = apply { this.skyColor = color }
    fun waterColor(color: String) = apply { this.waterColor = color }
    fun waterFogColor(color: String) = apply { this.waterFogColor = color }
    fun grassColor(color: String) = apply { this.grassColor = color }
    fun dryFoliageColor(color: String) = apply { this.dryFoliageColor = color }
    fun blockReplacements(vararg blockReplacements: Pair<Material, Material>) = apply { this.blockReplacements = blockReplacements.toMap() }
    fun attributes(attributes: WrappedEnvironmentAttributeMap) = apply { this.attributes = attributes }
    fun particles(particles: ParticleCatalog) = apply { this.particles = particles }

    fun simplify(explorerOrder: ExplorerOrder<*>): PaleSideBiome {
        biome = CustomBiome.builder()
            .resourceKey(key)
            .settings(settings)
            .fogColor(fogColor)
            .foliageColor(foliageColor)
            .skyColor(skyColor)
            .waterColor(waterColor)
            .waterFogColor(waterFogColor)
            .grassColor(grassColor)
            .dryFoliageColor(dryFoliageColor)
            .blockReplacements(*blockReplacements.map { BlockReplacement.of(it.component1(), it.component2()) }.toTypedArray())
            .setAttributes(attributes)
            .particleCatalog(particles)
            .build()

        phonyBiome = PhonyCustomBiome.builder()
            .setCustomBiome(biome)
            .setPriority(PacketHandler.Priority.NORMAL)
            .setConditional { player, chunkLocation ->
                val world = player.world
                if (!CONFIG.explorer.suspendedWorlds.contains(world.name)) {
                    return@setConditional false
                }

                val eventPlayer = EventPlayerManager.getByUUIDOrNull(player.uniqueId)?.takeIf { it.isSuspended } ?: return@setConditional false
                val lastExplorerOrder = eventPlayer.getLastExplorerOrder {
                    it.explorerOrder.biome != null && it.getImmutableCompletion().isCompleted()
                } ?: return@setConditional false
                return@setConditional lastExplorerOrder.explorerOrder == explorerOrder
            }
            .build()

        biome?.register() ?: throw IllegalStateException("CustomBiome is not initialized")

        val handler = SuspendedWorldBiomeService.getInstance()?.packetHandler ?: return this
        handler.appendBiome(phonyBiome ?: throw IllegalStateException("PhonyBiome is not initialized"))
        return this
    }

}