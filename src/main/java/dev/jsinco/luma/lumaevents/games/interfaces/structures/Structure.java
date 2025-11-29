package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.thorinwasher.schem.Schematic;
import dev.thorinwasher.schem.SchematicReader;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.Leaves;
import org.joml.Matrix3d;
import org.joml.Vector3i;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

// TODO: Maybe swap this out for FAWE because these schematics can get big
@Getter
public class Structure {

    Matrix3d transformation = new Matrix3d();

    private final Location origin;
    private final Schematic schematic;

    public Structure(Location origin, String localSchemPath) {
        this.origin = origin;
        Path path = EventMain.getInstance().getDataPath().resolve("/schematics/").resolve(localSchemPath);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("Schematic file does not exist at path: " + path);
        }
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.schematic = new SchematicReader().read(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Liberally borrowed from ThorinWasher in Garden

    public void paste() {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (blockData.getMaterial().isAir()) {
                return;
            }
            if (blockData instanceof Leaves leaves) {
                leaves.setPersistent(false);
            }
            vector3i.sub(offset);
            Location posToReplace = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

            world.setBlockData(posToReplace, blockData);
        });
    }

    public void remove() {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (blockData.getMaterial().isAir()) {
                return;
            }
            vector3i.sub(offset);
            Location location = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

            location.getBlock().setType(Material.AIR);
        });
    }
}
