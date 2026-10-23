package dev.lumas.events.games.logic.copycat;

import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.utility.vector.BlockPos;
import dev.thorinwasher.schem.BlockUtil;
import dev.thorinwasher.schem.Schematic;
import dev.thorinwasher.schem.SchematicReader;
import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Campfire;
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
import org.joml.Vector3i;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public final class CopycatBuild {

    static final int MAX_FOOTPRINT = 16;
    static final int MAX_HEIGHT = 16;
    private static final List<BlockFace> SIXTEENTHS = List.of(
        BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
        BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
        BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
        BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST);
    private static final List<BlockFace> EXPOSURE_FACES = List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();
    private static final Set<Material> INVISIBLE = Set.of(Material.BARRIER, Material.STRUCTURE_VOID, Material.LIGHT);
    private static final String POTTED_PREFIX = "POTTED_";
    private static final Set<Material> WATER_PLANTS = Set.of(Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT);
    private static final Map<Material, Material> PLANT_HEADS = Map.of(
        Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES,
        Material.TWISTING_VINES_PLANT, Material.TWISTING_VINES,
        Material.CAVE_VINES_PLANT, Material.CAVE_VINES,
        Material.KELP_PLANT, Material.KELP,
        Material.BIG_DRIPLEAF_STEM, Material.BIG_DRIPLEAF);
    private static final Map<Material, Material> SLAB_BLOCKS = slabBlocks();

    private final String name;
    private final Map<BlockPos, BlockData> blocks;
    private final Map<BlockPos, TileDetail> details;
    private final BuildSettings settings;
    private final List<List<BlockPos>> units;
    private final List<List<BlockPos>> removableUnits;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private CopycatBuild(String name, Map<BlockPos, BlockData> blocks, Map<BlockPos, TileDetail> details, BuildSettings settings) {
        this.name = name;
        this.blocks = Collections.unmodifiableMap(blocks);
        this.details = Collections.unmodifiableMap(details);
        this.settings = settings;
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
        this.removableUnits = this.units.stream()
            .filter(unit -> unit.stream().allMatch(pos -> settings.removable(this.blocks.get(pos).getMaterial())))
            .filter(unit -> unit.stream().anyMatch(this::exposed))
            .toList();
    }

    private boolean exposed(BlockPos pos) {
        for (BlockFace face : EXPOSURE_FACES) {
            BlockPos neighbour = pos.add(face.getModX(), face.getModY(), face.getModZ());
            if (neighbour.y() < 0 && !settings.offGround()) continue;
            if (!blocks.containsKey(neighbour)) return true;
        }
        return false;
    }

    public static List<CopycatBuild> loadAll(File folder, Map<String, BuildSettings> overrides) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".schem"));
        List<CopycatBuild> builds = new ArrayList<>();
        if (files == null) {
            return builds;
        }
        for (File file : files) {
            CopycatBuild build = load(file, overrides.getOrDefault(file.getName(), BuildSettings.DEFAULT));
            if (build != null) {
                LOGGER.info("Loaded Copycat build " + build.name + " (" + build.sizeX + "x" + build.sizeY + "x" + build.sizeZ
                    + ", " + build.blockCount() + " blocks, off-ground: " + build.settings.offGround() + ")");
                builds.add(build);
            }
        }
        return builds;
    }

    @Nullable
    private static CopycatBuild load(File file, BuildSettings settings) {
        Schematic schematic;
        Map<BlockPos, TileDetail> rawDetails;
        try (InputStream in = new FileInputStream(file)) {
            schematic = new SchematicReader().read(in);
            rawDetails = readDetails(file, schematic.offset());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read Copycat build " + file.getName(), e);
            return null;
        }

        Map<BlockPos, BlockData> raw = new HashMap<>();
        List<String> problems = new ArrayList<>();
        schematic.apply(new Matrix3d(), (vector, data) -> {
            if (data.getMaterial().isAir() || INVISIBLE.contains(data.getMaterial())) return;
            String problem = reject(data);
            if (problem != null) {
                if (problems.size() < 3) problems.add(problem);
                return;
            }
            raw.put(new BlockPos(vector.x(), vector.y(), vector.z()), settle(data));
        });

        if (!problems.isEmpty()) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + String.join(", ", problems));
            return null;
        }
        if (raw.isEmpty()) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": it is empty");
            return null;
        }

        rawDetails.keySet().retainAll(raw.keySet());
        BlockPos min = minOf(raw);
        Map<BlockPos, BlockData> normalized = shift(raw, min);
        String pairProblem = checkPairs(normalized);
        if (pairProblem != null) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + pairProblem);
            return null;
        }

        CopycatBuild build = new CopycatBuild(file.getName(), normalized, shift(rawDetails, min), settings);
        if (build.sizeX > MAX_FOOTPRINT || build.sizeZ > MAX_FOOTPRINT || build.sizeY > MAX_HEIGHT) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": " + build.sizeX + "x" + build.sizeY + "x" + build.sizeZ
                + " exceeds " + MAX_FOOTPRINT + "x" + MAX_HEIGHT + "x" + MAX_FOOTPRINT);
            return null;
        }
        if (build.removableCount() == 0) {
            LOGGER.warning("Skipping Copycat build " + file.getName() + ": no block is both exposed to air and outside ignored-materials");
            return null;
        }
        return build;
    }

    private static Map<BlockPos, TileDetail> readDetails(File file, Vector3i offset) throws IOException {
        CompoundBinaryTag root = BinaryTagIO.unlimitedReader().read(file.toPath(), BinaryTagIO.Compression.GZIP);
        CompoundBinaryTag schematic = root.get("Schematic") instanceof CompoundBinaryTag nested ? nested : root;
        ListBinaryTag entities = schematic.getInt("Version") == 3
            ? schematic.getCompound("Blocks").getList("BlockEntities", BinaryTagTypes.COMPOUND)
            : schematic.getList("BlockEntities", BinaryTagTypes.COMPOUND);

        Map<BlockPos, TileDetail> details = new HashMap<>();
        for (BinaryTag entry : entities) {
            CompoundBinaryTag entity = (CompoundBinaryTag) entry;
            int[] pos = entity.getIntArray("Pos");
            if (pos.length != 3) continue;
            CompoundBinaryTag data = entity.get("Data") instanceof CompoundBinaryTag nested ? nested : entity;
            TileDetail detail = TileDetail.parse(entity.getString("Id"), data);
            if (detail != null) {
                details.put(new BlockPos(pos[0] + offset.x(), pos[1] + offset.y(), pos[2] + offset.z()), detail);
            }
        }
        return details;
    }

    @Nullable
    private static String reject(BlockData data) {
        Material material = data.getMaterial();
        if (isFluid(material) && data instanceof Levelled levelled && levelled.getLevel() != 0) {
            return "flowing " + material.name().toLowerCase() + " cannot be placed";
        }
        Material item = placementItem(data);
        if (!item.isItem()) {
            return material.name().toLowerCase() + " has no item";
        }
        if (isPotted(material) && pottedPlant(data) == null) {
            return material.name().toLowerCase() + " has no plant item";
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

    public static BlockData settle(BlockData data) {
        if (data instanceof Campfire campfire && campfire.isLit()) {
            Campfire unlit = (Campfire) campfire.clone();
            unlit.setLit(false);
            return unlit;
        }
        if (isCauldron(data.getMaterial()) && data instanceof Levelled levelled && levelled.getLevel() != levelled.getMaximumLevel()) {
            Levelled full = (Levelled) levelled.clone();
            full.setLevel(full.getMaximumLevel());
            return full;
        }
        return data;
    }

    public static boolean isWaterlogged(@Nullable BlockData data) {
        if (data != null && WATER_PLANTS.contains(data.getMaterial())) return true;
        return data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    @Nullable
    public static Material extraBucket(@Nullable BlockData data) {
        if (data == null) return null;
        return switch (data.getMaterial()) {
            case WATER_CAULDRON -> Material.WATER_BUCKET;
            case LAVA_CAULDRON -> Material.LAVA_BUCKET;
            case POWDER_SNOW_CAULDRON -> Material.POWDER_SNOW_BUCKET;
            default -> isWaterlogged(data) ? Material.WATER_BUCKET : null;
        };
    }

    public static boolean isCauldron(Material material) {
        return material == Material.CAULDRON || material == Material.WATER_CAULDRON
            || material == Material.LAVA_CAULDRON || material == Material.POWDER_SNOW_CAULDRON;
    }

    private static BlockPos minOf(Map<BlockPos, ?> raw) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos blockPos : raw.keySet()) {
            minX = Math.min(minX, blockPos.x());
            minY = Math.min(minY, blockPos.y());
            minZ = Math.min(minZ, blockPos.z());
        }
        return new BlockPos(minX, minY, minZ);
    }

    private static <T> Map<BlockPos, T> shift(Map<BlockPos, T> raw, BlockPos min) {
        Map<BlockPos, T> shifted = new LinkedHashMap<>();
        raw.forEach((pos, value) -> shifted.put(pos.subtract(min), value));
        return shifted;
    }

    public CopycatBuild rotated(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) {
            return this;
        }
        Matrix3d rotation = new Matrix3d().rotationY(Math.toRadians(90.0 * turns));
        Map<BlockPos, BlockData> rotated = new HashMap<>();
        Map<BlockPos, TileDetail> rotatedDetails = new HashMap<>();
        blocks.forEach((pos, data) -> {
            BlockPos target = rotate(pos, turns);
            rotated.put(target, rotateSixteenths(BlockUtil.transformBlockData(data, rotation), turns));
            TileDetail detail = details.get(pos);
            if (detail != null) {
                rotatedDetails.put(target, detail);
            }
        });
        BlockPos min = minOf(rotated);
        return new CopycatBuild(name, shift(rotated, min), shift(rotatedDetails, min), settings);
    }

    private static BlockPos rotate(BlockPos pos, int turns) {
        int x = pos.x();
        int z = pos.z();
        for (int i = 0; i < turns; i++) {
            int rotatedX = z;
            z = -x;
            x = rotatedX;
        }
        return new BlockPos(x, pos.y(), z);
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

    public int removableCount() {
        return removableUnits.stream().mapToInt(List::size).sum();
    }

    public static ItemStack itemFor(BlockData data) {
        return itemFor(data, null);
    }

    public static ItemStack itemFor(BlockData data, @Nullable TileDetail detail) {
        ItemStack item = new ItemStack(placementItem(data), Math.max(1, itemCount(data)));
        if (detail != null) {
            detail.applyTo(item);
        }
        return item;
    }

    public static Material placementItem(BlockData data) {
        return switch (data.getMaterial()) {
            case WATER -> Material.WATER_BUCKET;
            case LAVA -> Material.LAVA_BUCKET;
            case WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON -> Material.CAULDRON;
            default -> {
                if (isPotted(data.getMaterial())) yield Material.FLOWER_POT;
                Material head = PLANT_HEADS.get(data.getMaterial());
                yield head != null ? head.createBlockData().getPlacementMaterial() : data.getPlacementMaterial();
            }
        };
    }

    public static boolean isStemPlant(Material material) {
        return PLANT_HEADS.containsKey(material) || PLANT_HEADS.containsValue(material);
    }

    public static Material plantHead(Material material) {
        return PLANT_HEADS.getOrDefault(material, material);
    }

    public static boolean isPotted(Material material) {
        return material.name().startsWith(POTTED_PREFIX);
    }

    @Nullable
    public static Material pottedPlant(@Nullable BlockData data) {
        if (data == null || !isPotted(data.getMaterial())) return null;
        String plant = data.getMaterial().name().substring(POTTED_PREFIX.length());
        Material material = Material.getMaterial(plant);
        if ((material == null || !material.isItem()) && plant.endsWith("_BUSH")) {
            material = Material.getMaterial(plant.substring(0, plant.length() - "_BUSH".length()));
        }
        return material != null && material.isItem() ? material : null;
    }

    public static boolean isFluid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    // a double slab looks the same as its full block
    private static BlockData unslab(BlockData data) {
        if (!(data instanceof Slab slab) || slab.getType() != Slab.Type.DOUBLE) return data;
        Material full = SLAB_BLOCKS.get(data.getMaterial());
        return full != null ? full.createBlockData() : data;
    }

    private static Map<Material, Material> slabBlocks() {
        Map<Material, Material> slabBlocks = new EnumMap<>(Material.class);
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.startsWith("LEGACY_") || !name.endsWith("_SLAB")) continue;
            String base = name.substring(0, name.length() - "_SLAB".length());
            // planks first so bamboo slabs pair with bamboo planks rather than the bamboo plant
            for (String candidate : List.of(base + "_PLANKS", base, base + "S", base + "_BLOCK")) {
                Material full = Material.getMaterial(candidate);
                if (full != null && full.isBlock() && !full.isAir()) {
                    slabBlocks.put(material, full);
                    break;
                }
            }
        }
        slabBlocks.put(Material.PETRIFIED_OAK_SLAB, Material.OAK_PLANKS);
        slabBlocks.remove(Material.SMOOTH_STONE_SLAB);
        return slabBlocks;
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

    public static boolean matches(Placed target, Placed actual) {
        return matches(target.data(), actual.data()) && TileDetail.same(target.detail(), actual.detail());
    }

    public static boolean matches(BlockData target, BlockData actual) {
        if (target.getMaterial() != actual.getMaterial()) {
            return sameBlock(unslab(target), unslab(actual));
        }
        return sameBlock(target, actual);
    }

    private static boolean sameBlock(BlockData target, BlockData actual) {
        if (plantHead(target.getMaterial()) != plantHead(actual.getMaterial())) return false;
        if (itemCount(target) != itemCount(actual)) return false;
        if (isWaterlogged(target) != isWaterlogged(actual)) return false;
        //if (target instanceof Rotatable rotatable && actual instanceof Rotatable other && rotatable.getRotation() != other.getRotation()) return false;
        return switch (target) {
            /** turns out a lot of blocks have rotations that aren't visible through their texture, plus some schematics have blocks facing air and builders don't really have support blocks
            case
                Directional directional when actual instanceof Directional other && directional.getFacing() != other.getFacing() ->
                false;
            case
                Orientable orientable when actual instanceof Orientable other && orientable.getAxis() != other.getAxis() ->
                false;
             **/
            // an open trapdoor looks the same hinged at the top or the bottom
            case TrapDoor trapDoor when trapDoor.isOpen() -> true;
            case Bisected bisected when actual instanceof Bisected other && bisected.getHalf() != other.getHalf() ->
                false;
            default ->
                !(target instanceof Slab slab) || !(actual instanceof Slab other) || Objects.equals(slab.getType(), other.getType());
        };
    }
}
