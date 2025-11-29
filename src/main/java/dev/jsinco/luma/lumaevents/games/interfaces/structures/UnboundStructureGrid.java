package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A structure grid that is not bound to any specific area.
 * It generates spawn locations around a central point with specified spacing constraints.
 * @see StructureGrid
 */
public class UnboundStructureGrid implements StructureGrid {

    private static final Random RANDOM = Util.RANDOM;
    private static final int MAX_REPOSITION_ATTEMPTS = 10;

    private final Location centerLocation;
    private final int minSpacing;
    private final int maxSpacing;

    public UnboundStructureGrid(Location centerLocation, int minSpacing, int maxSpacing) {
        this.centerLocation = centerLocation;
        this.minSpacing = minSpacing;
        this.maxSpacing = maxSpacing;
    }

    @Override
    public List<Location> generateSpawnLocations(int desiredPoints) {
        List<Location> locations = new ArrayList<>();
        // area needed
        double areaPerPoint = maxSpacing * maxSpacing;
        double totalArea = desiredPoints * areaPerPoint;

        double sideLength = Math.sqrt(totalArea);

        GridDimensions grid = calculateGridDimensions(desiredPoints, sideLength, sideLength);


        grid = optimizeGridSpacing(grid, sideLength, sideLength, desiredPoints);

        double actualWidth = (grid.x() - 1) * minSpacing;
        double actualLength = (grid.z() - 1) * minSpacing;

        double spacingX = (grid.x() > 1) ? actualWidth / (grid.x() - 1) : 0;
        double spacingZ = (grid.z() > 1) ? actualLength / (grid.z() - 1) : 0;
        double jitterX = Math.min(spacingX * 0.4, (spacingX - minSpacing) / 2);
        double jitterZ = Math.min(spacingZ * 0.4, (spacingZ - minSpacing) / 2);

        generateLocations(locations, grid, actualWidth, actualLength, jitterX, jitterZ, desiredPoints);

        return locations;
    }

    private GridDimensions calculateGridDimensions(int desiredPoints, double width, double length) {
        int pointsX = (int) Math.ceil(Math.sqrt(desiredPoints * (width / length)));
        int pointsZ = (int) Math.ceil((double) desiredPoints / pointsX);

        // If we overshot the desired count, reduce
        while (pointsX * pointsZ > desiredPoints && pointsZ > 1) {
            pointsZ--;
        }

        return new GridDimensions(pointsX, pointsZ);
    }

    private GridDimensions optimizeGridSpacing(GridDimensions grid, double width, double length, int desiredPoints) {
        int pointsX = grid.x();
        int pointsZ = grid.z();
        double spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
        double spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;

        // Ensure minimum spacing is maintained
        while ((spacingX < minSpacing && pointsX > 1) || (spacingZ < minSpacing && pointsZ > 1)) {
            if (spacingX < minSpacing && pointsX > 1) {
                width += minSpacing;
            }
            if (spacingZ < minSpacing && pointsZ > 1) {
                length += minSpacing;
            }

            spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
            spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;
        }

        return new GridDimensions(pointsX, pointsZ);
    }

    private void generateLocations(List<Location> locations, GridDimensions grid,
                                   double width, double length, double jitterX, double jitterZ, int desiredPoints) {
        double spacingX = (grid.x() > 1) ? width / (grid.x() - 1) : 0;
        double spacingZ = (grid.z() > 1) ? length / (grid.z() - 1) : 0;

        // start from center and work outward
        double startX = centerLocation.getX() - (width / 2);
        double startZ = centerLocation.getZ() - (length / 2);

        int generatedPoints = 0;

        for (int x = 0; x < grid.x() && generatedPoints < desiredPoints; x++) {
            for (int z = 0; z < grid.z() && generatedPoints < desiredPoints; z++) {
                Location spawnLoc = null;

                // Try to find a valid location with jitter
                for (int attempt = 0; attempt < MAX_REPOSITION_ATTEMPTS; attempt++) {
                    Location candidateLoc = candidateLoc(x, z, startX, startZ, spacingX, spacingZ, jitterX, jitterZ);

                    if (isValidLocation(candidateLoc, locations)) {
                        spawnLoc = candidateLoc;
                        break;
                    }
                }
                if (spawnLoc == null) {
                    spawnLoc = candidateLoc(x, z, startX, startZ, spacingX, spacingZ, 0, 0);
                }

                locations.add(spawnLoc);
                generatedPoints++;
            }
        }
    }

    private Location candidateLoc(int x, int z, double startX, double startZ, double spacingX,
                                  double spacingZ, double jitterX, double jitterZ) {
        double locX = startX + (x * spacingX);
        double locZ = startZ + (z * spacingZ);

        // Add jitter
        double offsetX = (RANDOM.nextDouble() * 2 - 1) * jitterX;
        double offsetZ = (RANDOM.nextDouble() * 2 - 1) * jitterZ;

        locX += offsetX;
        locZ += offsetZ;

        return new Location(centerLocation.getWorld(), locX, centerLocation.getY(), locZ);
    }

    private boolean isValidLocation(Location candidate, List<Location> existing) {
        double minDistanceSquared = minSpacing * minSpacing;

        for (Location loc : existing) {
            double distanceSquared = loc.distanceSquared(candidate);

            if (distanceSquared < minDistanceSquared) {
                return false;
            }
        }

        return true;
    }
}