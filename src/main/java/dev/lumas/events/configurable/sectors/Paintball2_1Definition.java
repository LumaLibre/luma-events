package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Bed;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Paintball2_1Definition extends OkaeriConfig {

    private Location spawnLocation;
    private Location team1SpawnLocation;
    private Location team2SpawnLocation;
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
    private List<TeamBedPart> team1BedParts = List.of(new TeamBedPart(null, Bed.Part.HEAD), new TeamBedPart(null, Bed.Part.FOOT));
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
    @AllArgsConstructor
    public static class TeamBedPart extends OkaeriConfig {
        private Location blockLocation;
        private Bed.Part part;
    }
}
