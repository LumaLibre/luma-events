package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.vector.Location3i;
import dev.lumas.events.utility.vector.Vector2i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ArtifactRegion(Map<Vector2i, Integer> floorMap, WorldTiedBoundingBox boundingBox) {

    private static final int MINIMUM_EMPTY_SPACE_BETWEEN_FLOORS = 3;
    private static final int MIN_FLOOR_AREA = 4;
    private static final Set<Material> BANNED_BLOCKS = Set.of(
        Material.BARRIER
    );


    public static List<ArtifactRegion> within(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        // Element 0 is going to be the top y value
        List<List<Vector2i>> freeSpots = new ArrayList<>();
        Map<Vector2i, Integer> emptySpace = new HashMap<>();
        for (int y = y2; y >= y1; y--) {
            List<Vector2i> freeLevelSpots = new ArrayList<>();
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Vector2i pos = new Vector2i(x, z);
                    if (BANNED_BLOCKS.contains(block.getType())) {
                        emptySpace.remove(pos);
                        continue;
                    } else if (ArtifactEntity.canBeContainedWithin(block)) {
                        emptySpace.merge(pos, 1, Integer::sum);
                        continue;
                    } else if (emptySpace.getOrDefault(pos, 0) >= MINIMUM_EMPTY_SPACE_BETWEEN_FLOORS) {
                        freeLevelSpots.add(new Vector2i(x, z));
                    }
                    emptySpace.remove(pos);
                }
            }
            freeSpots.add(freeLevelSpots);
        }
        List<ArtifactRegion> output = new ArrayList<>();
        Map<Vector2i, Integer> floorMap = new HashMap<>();
        int emptyHeight = 0;
        for (int dy = 0; dy < freeSpots.size(); dy++) {
            if (freeSpots.get(dy).size() < MIN_FLOOR_AREA) {
                if (!floorMap.isEmpty()) {
                    int minValue = floorMap.values().stream().min(Integer::compareTo)
                        .orElseThrow();
                    output.add(new ArtifactRegion(Map.copyOf(floorMap), new WorldTiedBoundingBox(
                        world, x1, minValue, z1, x2, minValue + emptyHeight, z2
                    )));
                    floorMap.clear();
                    emptyHeight = 0;
                }
                emptyHeight++;
                continue;
            }
            int dyFinal = dy;
            freeSpots.get(dy).forEach(vector2i -> floorMap.put(vector2i, y2 - dyFinal + 1));
        }
        if (!floorMap.isEmpty()) {
            int minValue = floorMap.values().stream().min(Integer::compareTo)
                .orElseThrow();
            output.add(new ArtifactRegion(Map.copyOf(floorMap), new WorldTiedBoundingBox(
                world, x1, minValue, z1, x2, minValue + emptyHeight, z2
            )));
        }
        return output;
    }

    public Location3i randomPos() {
        Vector2i xzPos = Util.getRandom(floorMap().keySet());
        int yPos = floorMap().get(xzPos);
        return new Location3i(boundingBox().getWorld(), xzPos.x(), yPos, xzPos.z());
    }
}
