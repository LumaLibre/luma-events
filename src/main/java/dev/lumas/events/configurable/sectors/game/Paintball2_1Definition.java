package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Bed;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigSerializable
public class Paintball2_1Definition {

    private Location spawnLocation;
    @MapTied
    private Location team1SpawnLocation;
    @MapTied
    private Location team2SpawnLocation;
    @MapTied
    private Region region = new Region();
    private List<Material> blacklistedBlocks = List.of(Material.SPRUCE_TRAPDOOR);
    private List<String> regexBlacklistedBlocks = List.of(
            ".*_TRAPDOOR",
            ".*_FENCE_GATE",
            ".*_DOOR",
            ".*_BUTTON",
            ".*_LEVER",
            ".*_SIGN",
            ".*_WALL_SIGN",
            ".*_PLATE"
    );
    @MapTied
    private List<TeamBedPart> team1BedParts = List.of(new TeamBedPart(null, Bed.Part.HEAD), new TeamBedPart(null, Bed.Part.FOOT));
    @MapTied
    private List<TeamBedPart> team2BedParts = List.of(new TeamBedPart(null, Bed.Part.HEAD), new TeamBedPart(null, Bed.Part.FOOT));
    private int throwCooldownTicks = 4;


    public List<Material> regexBlacklistedBlocksAsMaterials() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            for (String regex : regexBlacklistedBlocks) {
                if (material.name().matches(regex)) {
                    materials.add(material);
                }
            }
        }
        return materials;
    }

    public  List<Material> getAllBlacklistedBlocks() {
        List<Material> allBlacklistedBlocks = new ArrayList<>(blacklistedBlocks);
        allBlacklistedBlocks.addAll(regexBlacklistedBlocksAsMaterials());
        return allBlacklistedBlocks;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ConfigSerializable
    public static class TeamBedPart {
        private Location blockLocation;
        private Bed.Part part;
    }
}
