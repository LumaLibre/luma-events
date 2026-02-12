package dev.lumas.events.games.interfaces.structures;

import com.google.common.base.Preconditions;
import dev.lumas.events.utility.Util;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A bound structure grid that generates points in the shape of a circle,
 * starting from the center and moving outward until the desired number of points is reached
 * or the maximum radius is exceeded.
 *
 * @see StructureGrid
 */
public class BoundCircularStructureGrid implements StructureGrid {

    private static final Random RANDOM = Util.RANDOM;
    private static final int MAX_REPOSITION_ATTEMPTS = 10;

    private final Location center;
    private final double maxRadius;
    private final int minSpacing;
    private final int maxSpacing;
    private final boolean jitter;

    public BoundCircularStructureGrid(Location center, double maxRadius, int spacing) {
        this(center, maxRadius, spacing, spacing, false);
    }

    public BoundCircularStructureGrid(Location center, double maxRadius, int minSpacing, int maxSpacing) {
        this(center, maxRadius, minSpacing, maxSpacing, false);
    }

    public BoundCircularStructureGrid(Location center, double maxRadius, int minSpacing, int maxSpacing, boolean jitter) {
        Preconditions.checkNotNull(center, "Center location cannot be null");
        Preconditions.checkArgument(maxRadius > 0, "Max radius must be greater than 0");
        Preconditions.checkArgument(minSpacing > 0, "Min spacing must be greater than 0");
        Preconditions.checkArgument(maxSpacing >= minSpacing, "Max spacing must be greater than or equal to min spacing");
        this.center = center;
        this.maxRadius = maxRadius;
        this.minSpacing = minSpacing;
        this.maxSpacing = maxSpacing;
        this.jitter = jitter;
    }

    @Override
    public List<Location> generateSpawnLocations(int desiredPoints) {
        List<Location> locations = new ArrayList<>();

        // center point
        locations.add(center.clone());
        if (desiredPoints == 1) {
            return locations;
        }

        // using rings to generate these points:
        // we check how many points can fit on each ring based on circumference and min spacing
        // then generate points within bounds on each ring until we reach desired count or
        // run out of space
        double currentRadius = minSpacing;
        int generatedPoints = 1;

        while (generatedPoints < desiredPoints) {
            int pointsOnRing = getPointsOnRing(desiredPoints, currentRadius);
            int pointsToGenerate = Math.min(pointsOnRing, desiredPoints - generatedPoints);
            
            // generate points for this ring
            for (int i = 0; i < pointsToGenerate; i++) {
                double angle = (2 * Math.PI * i) / pointsOnRing;
                Location candidateLoc = this.generatePointOnRing(currentRadius, angle, locations);
                
                if (candidateLoc != null) {
                    locations.add(candidateLoc);
                    generatedPoints++;
                }
            }

            double increment = Math.min(minSpacing, maxSpacing);
            if (increment <= 0) { // inf while-loop check, although this would probably be running async anyway
                throw new IllegalStateException("Ring radius increment is 0 or negative, cannot proceed.");
            }
            currentRadius += increment;
        }

        return locations;
    }

    private int getPointsOnRing(int desiredPoints, double currentRadius) {
        if (currentRadius > maxRadius) {
            throw new IllegalStateException(
                    String.format("Cannot fit %d spawn points with minimum %d block spacing within max radius %.2f. " +
                                    "Maximum points possible: %d",
                            desiredPoints, minSpacing, maxRadius,
                            (int) Math.floor(Math.PI * maxRadius * maxRadius / (minSpacing * minSpacing))
                    )
            );
        }

        // some circle math im borrowing from XSeries
        double circumference = 2 * Math.PI * currentRadius;
        return Math.max(1, (int) Math.floor(circumference / minSpacing));
    }

    @Nullable
    private Location generatePointOnRing(double radius, double baseAngle, List<Location> existingLocations) {
        return jitter ? this.tryJitter(radius, baseAngle, existingLocations) : this.candidateLoc(radius, baseAngle, existingLocations);
    }

    @Nullable
    private Location tryJitter(double radius, double baseAngle, List<Location> existingLocations) {
        for (int attempt = 0; attempt < MAX_REPOSITION_ATTEMPTS; attempt++) {
            // TODO: better jitter configuration
            double jitterRadius = (RANDOM.nextDouble() * 2 - 1) * (maxSpacing - minSpacing) * 0.3;
            double jitterAngle = (RANDOM.nextDouble() * 2 - 1) * 0.2;

            double actualRadius = radius + jitterRadius;
            double actualAngle = baseAngle + jitterAngle;

            if (actualRadius > maxRadius) {
                actualRadius = maxRadius;
            }


            Location loc = this.candidateLoc(actualRadius, actualAngle, existingLocations);
            if (loc != null) {
                return loc;
            }
        }
        return null;
    }

    @Nullable
    private Location candidateLoc(double radius, double angle, List<Location> existingLocations) {
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);
        Location candidate = new Location(center.getWorld(), x, center.getY(), z);
        if (this.isValidLocation(candidate, existingLocations)) {
            return candidate;
        }
        return null;
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