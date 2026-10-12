package dev.lumas.events.games.logic.copycat;

import dev.lumas.core.util.PluginContextLogger;
import dev.thorinwasher.schem.BlockUtil;
import dev.thorinwasher.schem.Schematic;
import dev.thorinwasher.schem.SchematicReader;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public final class CopycatBuild {

    static final int MAX_FOOTPRINT = 16;
    static final int MAX_HEIGHT = 16;
    private static final List<BlockFace> SIXTEENTHS = List.of(
        BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
        BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
        BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
        BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST);
    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();

    private final String name;
    private final Map<BlockPos, BlockData> blocks;
    private final List<List<BlockPos>> units;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private CopycatBuild(String name, Map<BlockPos, BlockData> blocks) {
        this.name = name;
        this.blocks = Collections.unmodifiableMap(blocks);
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (BlockPos blockPos : blocks.keySet()) {
            maxX = Math.max(maxX, blockPos.x());
            maxY = Math.max(maxY, blockPos.y());
            maxZ = Math.max(maxZ, blockPos.z());
        }
        this.sizeX = maxX + 1;
        this.sizeY = maxY + 1;
        this.sizeZ = maxZ + 1;
        this.units = Collections.unmodifiableList(groupUnits(this.blocks));
    }

    public static List<CopycatBuild> loadAll(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".schem"));
        List<CopycatBuild> builds = new ArrayList<>();
        if (files == null) {
            return builds;
        }
        for (File file : files) {
            CopycatBuild build = load(file);
            if (build != null) {
                builds.add(build);
            }
        }
        return builds;
    }

    @Nullable
    private static CopycatBuild load(File file) {
        Schematic schematic;
        try (InputStream in = new FileInputStream(file)) {
            schematic = new SchematicReader().read(in);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read Copycat build " + file.getName(), e);
            return null;
        }

        Map<BlockPos, BlockData> raw = new HashMap<>();
        List<String> problems = new ArrayList<>();
        schematic.apply(new Matrix3d(), (vector, data) -> {
            if (data.getMaterial().isAir()) return;
            String problem = reject(data);
            if (problem != null) {
                if (problems.size() < 3) problems.add(problem);
                return;
            }
            raw.put(new BlockPos(vector.x(), vector.y(), vector.z()), data);
        });

        if (!problems.isEmpty()) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + String.join(", ", problems));
            return null;
        }
        if (raw.isEmpty()) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": it is empty");
            return null;
        }

        Map<BlockPos, BlockData> normalized = normalize(raw);
        String pairProblem = checkPairs(normalized);
        if (pairProblem != null) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + pairProblem);
            return null;
        }

        CopycatBuild build = new CopycatBuild(file.getName(), normalized);
        if (build.sizeX > MAX_FOOTPRINT || build.sizeZ > MAX_FOOTPRINT || build.sizeY > MAX_HEIGHT) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + build.sizeX + "x" + build.sizeY + "x" + build.sizeZ
                + " exceeds " + MAX_FOOTPRINT + "x" + MAX_HEIGHT + "x" + MAX_FOOTPRINT);
            return null;
        }
        return build;
    }

    @Nullable
    private static String reject(BlockData data) {
        Material material = data.getMaterial();
        if (isFluid(material) && data instanceof Levelled levelled && levelled.getLevel() != 0) {
            return "flowing " + material.name().toLowerCase() + " cannot be placed";
        }
        if (!placementItem(data).isItem()) {
            return material.name().toLowerCase() + " has no item";
        }
        if (data instanceof Bed) {
            return material.name().toLowerCase() + " is two blocks wide";
        }
        return null;
    }

    @Nullable
    private static String checkPairs(Map<BlockPos, BlockData> blocks) {
        for (Map.Entry<BlockPos, BlockData> entry : blocks.entrySet()) {
            BlockData data = entry.getValue();
            if (!isTallPair(data)) continue;
            BlockData partner = blocks.get(partnerOf(entry.getKey(), data));
            if (partner == null || partner.getMaterial() != data.getMaterial() || !isTallPair(partner)
                || ((Bisected) partner).getHalf() == ((Bisected) data).getHalf()) {
                return data.getMaterial().name().toLowerCase() + " at " + entry.getKey() + " is missing its other half";
            }
        }
        return null;
    }

    private static List<List<BlockPos>> groupUnits(Map<BlockPos, BlockData> blocks) {
        List<List<BlockPos>> units = new ArrayList<>();
        blocks.forEach((pos, data) -> {
            if (!isTallPair(data)) {
                units.add(List.of(pos));
            } else if (((Bisected) data).getHalf() == Bisected.Half.BOTTOM) {
                units.add(List.of(pos, partnerOf(pos, data)));
            }
        });
        return units;
    }

    public static boolean isTallPair(BlockData data) {
        return data instanceof Bisected && !(data instanceof Stairs) && !(data instanceof TrapDoor);
    }

    public static BlockPos partnerOf(BlockPos pos, BlockData data) {
        return ((Bisected) data).getHalf() == Bisected.Half.BOTTOM ? pos.add(0, 1, 0) : pos.add(0, -1, 0);
    }

    public static boolean isWaterlogged(@Nullable BlockData data) {
        return data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private static Map<BlockPos, BlockData> normalize(Map<BlockPos, BlockData> raw) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos blockPos : raw.keySet()) {
            minX = Math.min(minX, blockPos.x());
            minY = Math.min(minY, blockPos.y());
            minZ = Math.min(minZ, blockPos.z());
        }
        Map<BlockPos, BlockData> normalized = new LinkedHashMap<>();
        BlockPos min = new BlockPos(minX, minY, minZ);
        raw.forEach((pos, data) -> normalized.put(pos.subtract(min), data));
        return normalized;
    }

    public CopycatBuild rotated(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) {
            return this;
        }
        Matrix3d rotation = new Matrix3d().rotationY(Math.toRadians(90.0 * turns));
        Map<BlockPos, BlockData> rotated = new HashMap<>();
        blocks.forEach((pos, data) -> {
            int x = pos.x();
            int z = pos.z();
            for (int i = 0; i < turns; i++) {
                int rotatedX = z;
                z = -x;
                x = rotatedX;
            }
            rotated.put(new BlockPos(x, pos.y(), z), rotateSixteenths(BlockUtil.transformBlockData(data, rotation), turns));
        });
        return new CopycatBuild(name, normalize(rotated));
    }

    private static BlockData rotateSixteenths(BlockData data, int turns) {
        if (!(data instanceof Rotatable rotatable)) return data;
        int index = SIXTEENTHS.indexOf(rotatable.getRotation());
        if (index < 0) return data;
        rotatable.setRotation(SIXTEENTHS.get(Math.floorMod(index - 4 * turns, SIXTEENTHS.size())));
        return rotatable;
    }

    public int blockCount() {
        return blocks.size();
    }

    public static ItemStack itemFor(BlockData data) {
        return new ItemStack(placementItem(data), Math.max(1, itemCount(data)));
    }

    public static Material placementItem(BlockData data) {
        return switch (data.getMaterial()) {
            case WATER -> Material.WATER_BUCKET;
            case LAVA -> Material.LAVA_BUCKET;
            default -> data.getPlacementMaterial();
        };
    }

    public static boolean isFluid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    public static int itemCount(BlockData data) {
        if (isTallPair(data)) return ((Bisected) data).getHalf() == Bisected.Half.BOTTOM ? 1 : 0;
        if (data instanceof Slab slab) return slab.getType() == Slab.Type.DOUBLE ? 2 : 1;
        if (data instanceof Snow snow) return snow.getLayers();
        if (data instanceof Candle candle) return candle.getCandles();
        if (data instanceof SeaPickle pickle) return pickle.getPickles();
        if (data instanceof TurtleEgg egg) return egg.getEggs();
        return 1;
    }

    public static boolean matches(BlockData target, BlockData actual) {
        if (target.getMaterial() != actual.getMaterial()) return false;
        if (itemCount(target) != itemCount(actual)) return false;
        if (isWaterlogged(target) != isWaterlogged(actual)) return false;
        if (target instanceof Rotatable rotatable && actual instanceof Rotatable other && rotatable.getRotation() != other.getRotation()) return false;
        return switch (target) {
            case
                Directional directional when actual instanceof Directional other && directional.getFacing() != other.getFacing() ->
                false;
            case
                Orientable orientable when actual instanceof Orientable other && orientable.getAxis() != other.getAxis() ->
                false;
            case Bisected bisected when actual instanceof Bisected other && bisected.getHalf() != other.getHalf() ->
                false;
            default ->
                !(target instanceof Slab slab) || !(actual instanceof Slab other) || Objects.equals(slab.getType(), other.getType());
        };
    }
}
