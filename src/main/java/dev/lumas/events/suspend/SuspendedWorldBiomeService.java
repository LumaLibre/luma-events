package dev.lumas.events.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import dev.lumas.events.EventMain;
import me.outspending.biomesapi.biome.CustomBiome;
import me.outspending.biomesapi.exceptions.MissingPacketManipulatorLibraryException;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import me.outspending.biomesapi.renderer.packet.PacketHandler;
import me.outspending.biomesapi.renderer.packet.data.BlockReplacement;
import me.outspending.biomesapi.renderer.packet.data.PhonyCustomBiome;
import me.outspending.biomesapi.wrapper.BiomeSettings;
import org.bukkit.Material;

@SuppressWarnings({"UnstableApiUsage", "LombokGetterMayBeUsed"})
@Register(Autowire.SERVICE)
public class SuspendedWorldBiomeService implements Service {

    private static SuspendedWorldBiomeService instance;
    private PacketHandler packetHandler;

    @Override
    public void register() {
        instance = this;
        try {
            packetHandler = PacketHandler.of(EventMain.getInstance(), PacketHandler.Manipulator.PROTOCOLLIB);
        } catch (MissingPacketManipulatorLibraryException e) {
            e.printStackTrace();
            return;
        }

        CustomBiome baseWhiteBiome = CustomBiome.builder()
                .resourceKey(BiomeResourceKey.of("lumaevents", "pale"))
                .settings(BiomeSettings.defaultSettings())
                .fogColor("#FFFFFF") // #db4929
                .foliageColor("#F5F2EB")
                .skyColor("#FFFFFF")
                .waterColor("#000000") // #F5F2EB
                .waterFogColor("#000000")
                .grassColor("#FAF5EA")
                .dryFoliageColor("#FFFFFF")
                .blockReplacements(
                        BlockReplacement.of(Material.SAND, Material.WHITE_CONCRETE_POWDER),
                        BlockReplacement.of(Material.RED_SAND, Material.WHITE_CONCRETE_POWDER),
                        BlockReplacement.of(Material.GRAVEL, Material.WHITE_CONCRETE_POWDER),
                        BlockReplacement.of(Material.BIRCH_LEAVES, Material.ACACIA_LEAVES),
                        BlockReplacement.of(Material.SPRUCE_LEAVES, Material.WHITE_STAINED_GLASS),
                        BlockReplacement.of(Material.SPRUCE_LOG, Material.STRIPPED_PALE_OAK_LOG)
                )
                .build();

        baseWhiteBiome.register();

        PhonyCustomBiome phonyCustomBiome = PhonyCustomBiome.builder()
                .setCustomBiome(baseWhiteBiome)
                .setConditional((player, chunkLocation) ->
                        EventMain.getOkaeriConfig().getExplorer().getSuspendedWorlds().contains(player.getWorld().getName())
                )
                .setPriority(PacketHandler.Priority.LOW)
                .build();

        packetHandler.appendBiome(phonyCustomBiome);
        packetHandler.register();
    }

    @Override
    public void unregister() {
        packetHandler.unregister();
        instance = null;
    }

    @SuppressWarnings("") // lombok - kotlin
    public static SuspendedWorldBiomeService getInstance() {
        return instance;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }
}
