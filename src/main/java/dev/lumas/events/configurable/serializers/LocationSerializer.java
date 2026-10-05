package dev.lumas.events.configurable.serializers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class LocationSerializer extends ScalarSerializer<Location> {

    public LocationSerializer() {
        super(Location.class);
    }

    @Override
    public Location deserialize(Type type, Object obj) throws SerializationException {
        return parse(obj.toString());
    }

    @Override
    protected Object serialize(Location item, Predicate<Class<?>> typeSupported) {
        return format(item);
    }

    public static Location parse(String data) throws SerializationException {
        String[] parts = data.split(",");
        if (parts.length != 4 && parts.length != 6) {
            throw new SerializationException("Invalid location format: " + data);
        }
        try {
            World world = Bukkit.getWorld(parts[0].trim());
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            if (parts.length == 6) {
                float yaw = Float.parseFloat(parts[4].trim());
                float pitch = Float.parseFloat(parts[5].trim());
                return new Location(world, x, y, z, yaw, pitch);
            }
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid location format: " + data);
        }
    }

    public static String format(Location data) {
        if (data.getYaw() == 0 && data.getPitch() == 0) {
            return data.getWorld().getName() + "," + data.getBlockX() + "," + data.getBlockY() + "," + data.getBlockZ();
        }
        return data.getWorld().getName() + "," + data.getBlockX() + "," + data.getBlockY() + "," + data.getBlockZ() + "," + data.getYaw() + "," + data.getPitch();
    }
}
