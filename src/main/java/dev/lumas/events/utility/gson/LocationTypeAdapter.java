package dev.lumas.events.utility.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;

public class LocationTypeAdapter extends TypeAdapter<Location> {

    @Override
    public void write(JsonWriter jsonWriter, Location location) throws IOException {
        if (location == null) {
            jsonWriter.nullValue();
            return;
        }

        jsonWriter.beginObject();
        jsonWriter.name("world").value(location.getWorld() != null ? location.getWorld().getName() : null);
        jsonWriter.name("x").value(location.getX());
        jsonWriter.name("y").value(location.getY());
        jsonWriter.name("z").value(location.getZ());
        jsonWriter.endObject();
    }

    @Override
    public Location read(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }

        String worldName = null;
        double x = 0, y = 0, z = 0;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            switch (name) {
                case "world":
                    worldName = jsonReader.peek() == JsonToken.NULL ? null : jsonReader.nextString();
                    if (worldName == null) jsonReader.nextNull();
                    break;
                case "x":
                    x = jsonReader.nextDouble();
                    break;
                case "y":
                    y = jsonReader.nextDouble();
                    break;
                case "z":
                    z = jsonReader.nextDouble();
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();

        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        return new Location(world, x, y, z);
    }
}