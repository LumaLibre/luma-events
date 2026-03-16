package dev.lumas.events.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import dev.lumas.events.EventMain;
import me.outspending.biomesapi.biome.CustomBiome;
import me.outspending.biomesapi.exceptions.MissingPacketManipulatorLibraryException;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import me.outspending.biomesapi.renderer.packet.PacketHandler;
import me.outspending.biomesapi.renderer.packet.data.PhonyCustomBiome;
import me.outspending.biomesapi.wrapper.BiomeSettings;

@Register(Autowire.SERVICE)
public class SuspendWorldBiomeService implements Service {

    private PacketHandler packetHandler;

    @Override
    public void register() {
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
                .skyColor("#000000")
                .waterColor("#F5F2EB") // #F5F2EB
                .waterFogColor("#000000")
                .grassColor("#FFFFFF")
                .dryFoliageColor("#FFFFFF")
                .build();

        baseWhiteBiome.register();

        PhonyCustomBiome phonyCustomBiome = PhonyCustomBiome.builder()
                .setCustomBiome(baseWhiteBiome)
                .setConditional((player, chunkLocation) ->
                        EventMain.getOkaeriConfig().getSuspendedWorlds().contains(player.getWorld().getName())
                )
                .setPriority(PacketHandler.Priority.LOW)
                .build();

        packetHandler.appendBiome(phonyCustomBiome);
        packetHandler.register();
    }

    @Override
    public void unregister() {
        packetHandler.unregister();
    }
}
