package dev.lumas.events.configurable;

import dev.lumas.events.model.WorldTiedBoundingBox;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;

public class BoundingBoxTransformer extends BidirectionalTransformer<String, WorldTiedBoundingBox> {

    private static final LocationTransformer LOCATIONS = new LocationTransformer();

    @Override
    public GenericsPair<String, WorldTiedBoundingBox> getPair() {
        return this.genericsPair(String.class, WorldTiedBoundingBox.class);
    }

    @Override
    public WorldTiedBoundingBox leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        String[] corners = data.split("/");
        if (corners.length != 2) {
            throw new IllegalArgumentException("Invalid box format (expected two corners separated by a /): " + data);
        }

        Location corner1 = LOCATIONS.leftToRight(corners[0].trim(), serdesContext);
        Location corner2 = LOCATIONS.leftToRight(corners[1].trim(), serdesContext);
        return WorldTiedBoundingBox.ofBlocks(corner1, corner2);
    }

    @Override
    public String rightToLeft(@NonNull WorldTiedBoundingBox data, @NonNull SerdesContext serdesContext) {
        World world = data.getWorld();
        String name = world != null ? world.getName() : "world";
        return name + "," + (int) data.getMinX() + "," + (int) data.getMinY() + "," + (int) data.getMinZ()
                + "/" + name + "," + ((int) data.getMaxX() - 1) + "," + ((int) data.getMaxY() - 1) + "," + ((int) data.getMaxZ() - 1);
    }
}
