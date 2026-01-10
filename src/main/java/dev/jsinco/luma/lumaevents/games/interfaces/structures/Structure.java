package dev.jsinco.luma.lumaevents.games.interfaces.structures;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.thorinwasher.schem.Schematic;
import dev.thorinwasher.schem.SchematicReader;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.joml.Matrix3d;
import org.joml.Vector3i;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

// TODO: Maybe swap this out for FAWE because these schematics can get big
@Getter
public class Structure {

    private static final Path SCHEMATIC_DIR = EventMain.getInstance().getDataPath().resolve("/schematics/");
    private static final BlockData AIR_BLOCK_DATA = Material.AIR.createBlockData();
    Matrix3d transformation = new Matrix3d();

    private final Location origin;
    private final Schematic schematic;

    public Structure(Location origin, String localSchemPath) {
        this.origin = origin;
        Path path = SCHEMATIC_DIR.resolve(localSchemPath);
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

    public void paste(BiPredicate<Vector3i, BlockData> prePastePredicate) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (blockData.getMaterial().isAir()) return;

            vector3i.sub(offset);

            if (!prePastePredicate.test(vector3i, blockData)) return;
            Location posToReplace = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

            world.setBlockData(posToReplace, blockData);
        });
    }

    public void remove(BiPredicate<Vector3i, BlockData> preRemovePredicate) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (blockData.getMaterial().isAir()) return;

            vector3i.sub(offset);
            if (!preRemovePredicate.test(vector3i, blockData)) return;
            Location location = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

            location.getBlock().setBlockData(AIR_BLOCK_DATA);
        });
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Structure structure = (Structure) obj;

        if (!origin.equals(structure.origin)) return false;
        return schematic.equals(structure.schematic);
    }

    @Override
    public int hashCode() {
        int result = origin.hashCode();
        result = 31 * result + schematic.hashCode();
        return result;
    }

    static {
        SCHEMATIC_DIR.toFile().mkdirs();
    }
}
