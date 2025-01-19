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
        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(world, x, y, z);
    }

    @Override
    public String rightToLeft(@NonNull Location data, @NonNull SerdesContext serdesContext) {
        return data.getWorld().getName() + "," + data.getBlockX() + "," + data.getBlockY() + "," + data.getBlockZ();
    }
}
