package dev.jsinco.luma.lumaevents.configurable;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationTransformer extends BidirectionalTransformer<String, Location> {

    @Override
    public GenericsPair<String, Location> getPair() {
        return this.genericsPair(String.class, Location.class);
    }

    @Override
    public Location leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        String[] parts = data.split(",");
        if (parts.length != 4 && parts.length != 6) {
            throw new IllegalArgumentException("Invalid location format: " + data);
        }
        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        if (parts.length == 6) {
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        }
        return new Location(world, x, y, z);
    }

    @Override
    public String rightToLeft(@NonNull Location data, @NonNull SerdesContext serdesContext) {
        if (data.getYaw() == 0 && data.getPitch() == 0) {
            return data.getWorld().getName() + "," + data.getBlockX() + "," + data.getBlockY() + "," + data.getBlockZ();
        }
        return data.getWorld().getName() + "," + data.getBlockX() + "," + data.getBlockY() + "," + data.getBlockZ() + "," + data.getYaw() + "," + data.getPitch();
    }
}
