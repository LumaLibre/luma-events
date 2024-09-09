package dev.jsinco.lumacarnival.obj;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public class Sphere {

    private static final double TAU = Math.PI * 2.0;

    private final Location center;
    private double radius;
    private double density;

    public Sphere(Location center, double radius, double density) {
        this.center = center;
        this.radius = radius;
        this.density = density;
    }

    // Getters

    public Location getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public double getDensity() {
        return density;
    }

    public Set<Block> getSphere() {
        Set<Block> blockList = new HashSet<>();

        for (int i = 0; i < radius; i++) {
            blockList.addAll(getHollowSphere(center, i, density));
        }
        return blockList;
    }



    public Set<Block> getHollowSphere() {
        return getHollowSphere(center, radius, density);
    }


    /**
     * Resizes the sphere.
     * @param newRadius The new radius of the sphere.
     * @param newDensity The new density of the sphere.
     * @return The same sphere, just resized.
     */
    public Sphere resize(double newRadius, double newDensity) {
        this.radius = newRadius;
        this.density = newDensity;
        return this;
    }

    public double getDiameter() {
        return radius * 2;
    }

    public double getArea() {
        return 4 * Math.PI * radius * radius;
    }

    public double getVolume() {
        return (4.0 / 3.0) * Math.PI * Math.pow(radius, 3);
    }

    /**
     * Gets a random location within the sphere.
     * @return A random location within the sphere.
     */
    public Location getRandomLocation() { // AI GENERATED THIS, haven't tested nor do I know if it's correct
        double x = (Math.random() * radius * 2) - radius;
        double y = (Math.random() * radius * 2) - radius;
        double z = (Math.random() * radius * 2) - radius;
        return center.clone().add(x, y, z);
    }


    /**
     * Checks if an entity is within the sphere.
     * @param entity The entity to check.
     * @return True if the entity is within the sphere.
     */
    public boolean isInSphere(Entity entity) {
        return isInSphere(entity.getLocation());
    }

    /**
     * Checks if a location is within the sphere.
     * @param location The location to check.
     * @return True if the location is within the sphere.
     */
    public boolean isInSphere(Location location) {
        double x = location.getX() - center.getX();
        double y = location.getY() - center.getY();
        double z = location.getZ() - center.getZ();
        return x * x + y * y + z * z <= radius * radius;
    }


    /**
     * Checks if a location is within a certain marge of the sphere.
     * @param location The location to check.
     * @param marge The marge to check.
     * @return True if the location is within the marge of the sphere.
     */
    public boolean isWithinMarge(Location location, double marge) {
        double x = location.getX() - center.getX();
        double y = location.getY() - center.getY();
        double z = location.getZ() - center.getZ();
        return x * x + y * y + z * z <= (radius + marge) * (radius + marge);
    }


    public static Set<Block> getHollowSphere(Location center, double radius, double density) {
        Set<Block> blockList = new HashSet<>();

        double rateDiv = Math.PI / density;

        // To make a sphere we're going to generate multiple circles
        // next to each other.
        for (double phi = 0; phi <= Math.PI; phi += rateDiv) {

            double y1 = radius * Math.cos(phi);
            double y2 = radius * Math.sin(phi);

            for (double theta = 0; theta <= TAU; theta += rateDiv) {
                double x = Math.cos(theta) * y2;
                double z = Math.sin(theta) * y2;

                blockList.add(center.clone().add(x, y1, z).getBlock());
            }
        }
        return blockList;
    }
}
