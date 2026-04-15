package dev.lumas.events.model;

import dev.lumas.events.utility.Executors;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Setter
public class CylinderBoundingBox implements MinigameBoundingBox {

    private final Location center;
    private int radius;
    private int density;
    private int height;

    public CylinderBoundingBox(Location center, int radius, int density, int height) {
        this.center = center;
        this.radius = radius;
        this.density = density;
        this.height = height;
    }

    public List<CylinderLayer> getLayers() {
        List<CylinderLayer> layers = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            layers.add(new CylinderLayer(center.clone().add(0, i, 0), radius, density));
        }
        return layers;
    }

    public CylinderLayer getLayer(int y) {
        if (y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Layer index out of bounds: " + y + "/" + height);
        }
        return new CylinderLayer(center.clone().add(0, y, 0), radius, density);
    }

    public Set<Block> blockList() { // TODO: Optimize this method
        Set<Block> blockList = new HashSet<>();
        for (int i = 0; i < height; i++) {
            blockList.addAll(getLayer(i).blockList());
        }
        return blockList;
    }

    @Override
    public void operate(Consumer<Block> consumer) {
        for (int i = 0; i < height; i++) {
            final int finalI = i; // TODO: Unsafe
            Executors.runSync(this.center, () -> {
                consumer.accept(getLayer(finalI).center.getBlock());
            });
        }
    }

    public double getVolume() {
        return Math.PI * Math.pow(radius, 2) * height;
    }

    @Override
    public boolean contains(Location location) {
        double x = location.getX();
        double z = location.getZ();
        double y = location.getY();
        double x1 = center.getX();
        double z1 = center.getZ();
        double y1 = center.getY();
        return Math.pow(x - x1, 2) + Math.pow(z - z1, 2) <= Math.pow(radius, 2) && y >= y1 && y <= y1 + height;
    }

    public boolean isInMarge(Location location, double marge) {
        double x = location.getX();
        double z = location.getZ();
        double y = location.getY();
        double x1 = center.getX();
        double z1 = center.getZ();
        double y1 = center.getY();
        return Math.pow(x - x1, 2) + Math.pow(z - z1, 2) <= Math.pow(radius + marge, 2) && y >= y1 && y <= y1 + height;
    }

    public Location getRandomLocation() {
        double x = center.getX() + (Math.random() * 2 - 1) * radius;
        double y = center.getY() + Math.random() * height;
        double z = center.getZ() + (Math.random() * 2 - 1) * radius;
        return new Location(center.getWorld(), x, y, z);
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> players = new LinkedList<>();
        for (Player player : this.getCenter().getWorld().getPlayers()) {
            if (this.contains(player)) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public <T extends Entity> List<T> getEntities(Class<T> clazz) {
        List<T> entities = new LinkedList<>();
        for (Entity entity : this.getCenter().getWorld().getEntitiesByClass(clazz)) {
            if (this.contains(entity)) {
                entities.add(clazz.cast(entity));
            }
        }
        return entities;
    }

    @Override
    public Location getCenterLocation() {
        return center;
    }

    public static CylinderBoundingBox of(Location center, int radius, int height) {
        return new CylinderBoundingBox(center, radius, 100, height);
    }


    @Getter
    public static class CylinderLayer {

        private final Location center;
        private final int radius;
        private final int density;

        public CylinderLayer(Location center, int radius, int density) {
            this.center = center;
            this.radius = radius;
            this.density = density;
        }

        public Set<Block> blockList() {
            Set<Block> blockList = new HashSet<>();
            int angleIncrement = 360 / this.density;
            for (int radius = 0; radius < this.radius; radius++) {
                int i = 0;
                while (i < 360) {
                    double angle = i * Math.PI / 180;
                    double x = Math.round(radius * Math.cos(angle));
                    double z = Math.round(radius * Math.sin(angle));
                    Location loc = this.center.clone().add(x, 0, z);
                    blockList.add(loc.getBlock());
                    i += angleIncrement;
                }
            }
            return blockList;
        }

        public boolean isInLayer(Location location) {
            double x = location.getX();
            double z = location.getZ();
            double x1 = center.getX();
            double z1 = center.getZ();
            return Math.pow(x - x1, 2) + Math.pow(z - z1, 2) <= Math.pow(radius, 2);
        }
    }
}
