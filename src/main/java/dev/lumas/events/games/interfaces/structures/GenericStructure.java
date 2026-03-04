package dev.lumas.events.games.interfaces.structures;

import dev.lumas.events.utility.Executors;
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
import java.util.function.BiPredicate;

@Getter
public class GenericStructure extends Structure {

    private static final BlockData AIR_BLOCK_DATA = Material.AIR.createBlockData();
    Matrix3d transformation = new Matrix3d();

    private final Schematic schematic;

    public GenericStructure(Location origin, String localSchemPath) {
        super(origin, localSchemPath);
        Path path = SCHEMATIC_DIR.resolve(localSchemPath);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("Schematic file does not exist at path: " + path.toAbsolutePath());
        }
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.schematic = new SchematicReader().read(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void paste() {
        paste((vector3i, blockData) -> true);
    }

    @Override
    public void remove() {
        remove((vector3i, blockData) -> true);
    }

    // Liberally borrowed from ThorinWasher in Garden

    public void paste(BiPredicate<Vector3i, BlockData> prePastePredicate) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {

            int worldX = origin.getBlockX() + vector3i.x() - offset.x();
            int worldZ = origin.getBlockZ() + vector3i.z() - offset.z();
            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;

            Executors.runSync(world, chunkX, chunkZ, () -> {
                if (blockData.getMaterial().isAir()) return;

                vector3i.sub(offset);

                if (!prePastePredicate.test(vector3i, blockData)) return;
                Location posToReplace = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

                world.setBlockData(posToReplace, blockData);
            });
        });
    }

    public void remove(BiPredicate<Vector3i, BlockData> preRemovePredicate) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {

            int worldX = origin.getBlockX() + vector3i.x() - offset.x();
            int worldZ = origin.getBlockZ() + vector3i.z() - offset.z();
            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;

            Executors.runSync(world, chunkX, chunkZ, () -> {
                if (blockData.getMaterial().isAir()) return;

                vector3i.sub(offset);
                if (!preRemovePredicate.test(vector3i, blockData)) return;
                Location location = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

                location.getBlock().setBlockData(AIR_BLOCK_DATA);
            });
        });
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GenericStructure structure = (GenericStructure) obj;

        if (!origin.equals(structure.origin)) return false;
        return schematic.equals(structure.schematic);
    }

    @Override
    public int hashCode() {
        int result = origin.hashCode();
        result = 31 * result + schematic.hashCode();
        return result;
    }

}
