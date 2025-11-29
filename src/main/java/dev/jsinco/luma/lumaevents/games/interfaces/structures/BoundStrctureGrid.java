package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A structure grid that is bound to a specific area defined by a WorldTiedBoundingBox.
 * It generates spawn locations within the bounding box with specified spacing constraints.
 * @see StructureGrid
 */
public class BoundStrctureGrid implements StructureGrid {

    private static final Random RANDOM = Util.RANDOM;
    private static final int MAX_REPOSITION_ATTEMPTS = 10;

    private final WorldTiedBoundingBox boundingBox;
    private final int minSpacing;
    private final int maxSpacing;

    public BoundStrctureGrid(WorldTiedBoundingBox boundingBox, int minSpacing, int maxSpacing) {
        this.boundingBox = boundingBox;
        this.minSpacing = minSpacing;
        this.maxSpacing = maxSpacing;
    }

    @Override
    public List<Location> generateSpawnLocations(int desiredPoints) {
        Location center = boundingBox.getCenterLocation();
        List<Location> locations = new ArrayList<>();

        double width = boundingBox.getMaxX() - boundingBox.getMinX();
        double length = boundingBox.getMaxZ() - boundingBox.getMinZ();

        GridDimensions grid = calculateGridDimensions(desiredPoints, width, length);

        validateSpacing(grid, width, length, desiredPoints);

        grid = optimizeGridSpacing(grid, width, length, desiredPoints);

        // random offset within cell
        double spacingX = (grid.x() > 1) ? width / (grid.x() - 1) : 0;
        double spacingZ = (grid.z() > 1) ? length / (grid.z() - 1) : 0;
        double jitterX = Math.min(spacingX * 0.4, (spacingX - minSpacing) / 2);
        double jitterZ = Math.min(spacingZ * 0.4, (spacingZ - minSpacing) / 2);

        generateLocations(locations, center, grid, width, length, jitterX, jitterZ, desiredPoints);

        return locations;
    }

    private GridDimensions calculateGridDimensions(int desiredPoints, double width, double length) {
        int pointsX = (int) Math.ceil(Math.sqrt(desiredPoints * (width / length)));
        int pointsZ = (int) Math.ceil((double) desiredPoints / pointsX);

        // if we overshot the desired count
        while (pointsX * pointsZ > desiredPoints && pointsZ > 1) {
            pointsZ--;
        }

        return new GridDimensions(pointsX, pointsZ);
    }

    private void validateSpacing(GridDimensions grid, double width, double length, int desiredPoints) {
        double spacingX = (grid.x() > 1) ? width / (grid.x() - 1) : 0;
        double spacingZ = (grid.z() > 1) ? length / (grid.z() - 1) : 0;

        if ((spacingX < minSpacing && grid.x() > 1) || (spacingZ < minSpacing && grid.z() > 1)) {
            throw new IllegalStateException(
                    String.format("Cannot fit %d spawn points with minimum %d block spacing. " +
                                    "Bounding box is too small (%dx%d blocks). Maximum points: %d",
                            desiredPoints, minSpacing, (int)width, (int)length,
                            ((int)(width/minSpacing) + 1) * ((int)(length/minSpacing) + 1))
            );
        }
    }

    private GridDimensions optimizeGridSpacing(GridDimensions grid, double width, double length, int desiredPoints) {
        int pointsX = grid.x();
        int pointsZ = grid.z();
        double spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
        double spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;

        // increase grid density while spacing is too large
        while ((spacingX > maxSpacing || spacingZ > maxSpacing) && (pointsX * pointsZ < desiredPoints * 4)) {
            if (spacingX > maxSpacing && spacingX >= spacingZ) {
                pointsX++;
            } else if (spacingZ > maxSpacing) {
                pointsZ++;
            } else {
                break;
            }

            spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
            spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;
        }

        return new GridDimensions(pointsX, pointsZ);
    }

    private void generateLocations(List<Location> locations, Location center, GridDimensions grid,
                                   double width, double length, double jitterX, double jitterZ, int desiredPoints) {
        double spacingX = (grid.x() > 1) ? width / (grid.x() - 1) : 0;
        double spacingZ = (grid.z() > 1) ? length / (grid.z() - 1) : 0;
        double startX = boundingBox.getMinX();
        double startZ = boundingBox.getMinZ();

        int generatedPoints = 0;

        for (int x = 0; x < grid.x() && generatedPoints < desiredPoints; x++) {
            for (int z = 0; z < grid.z() && generatedPoints < desiredPoints; z++) {
                Location spawnLoc = null;

                // fix players spawning past minSpacing by repositioning within our cell
                for (int attempt = 0; attempt < MAX_REPOSITION_ATTEMPTS; attempt++) {
                    Location candidateLoc = candidateLoc(x, z, startX, startZ, spacingX, spacingZ, jitterX, jitterZ, center);

                    if (isValidLocation(candidateLoc, locations)) {
                        spawnLoc = candidateLoc;
                        break;
                    }
                }

                // Couldn't find one, just place it without jitter
                if (spawnLoc == null) {
                    spawnLoc = candidateLoc(x, z, startX, startZ, spacingX, spacingZ, 0, 0, center);
                }

                locations.add(spawnLoc);
                generatedPoints++;
            }
        }
    }

    private Location candidateLoc(int x, int z, double startX, double startZ, double spacingX,
                                  double spacingZ, double jitterX, double jitterZ, Location center) {
        double locX = startX + (x * spacingX);
        double locZ = startZ + (z * spacingZ);

        // Add jitter
        double offsetX = (RANDOM.nextDouble() * 2 - 1) * jitterX;
        double offsetZ = (RANDOM.nextDouble() * 2 - 1) * jitterZ;

        // clamp down to bounding box
        locX = Math.max(boundingBox.getMinX(), Math.min(boundingBox.getMaxX(), locX + offsetX));
        locZ = Math.max(boundingBox.getMinZ(), Math.min(boundingBox.getMaxZ(), locZ + offsetZ));

        return new Location(center.getWorld(), locX, center.getY(), locZ);
    }

    private boolean isValidLocation(Location candidate, List<Location> existing) {
        double minDistanceSquared = minSpacing * minSpacing;

        for (Location loc : existing) {
            double distanceSquared = loc.distanceSquared(candidate); // TODO: Can this method be called async?

            if (distanceSquared < minDistanceSquared) {
                return false;
            }
        }

        return true;
    }

}
