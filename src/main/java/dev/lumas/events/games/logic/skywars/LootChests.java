package dev.lumas.events.games.logic.skywars;

import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot.LootPackage;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot.LootPool;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.utility.Executors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

final class LootChests {

    private static final int USE_ALL_DYNAMIC_CHESTS_AT_TEAM_SIZE = 4;
    private static final Material[] DYNAMIC_CHEST_TIERS = {
            Material.WAXED_OXIDIZED_COPPER_CHEST,
            Material.WAXED_WEATHERED_COPPER_CHEST,
            Material.WAXED_EXPOSED_COPPER_CHEST,
            Material.WAXED_COPPER_CHEST
    };

    private final GenericStructure map;
    private final SkyWarsLoot loot;
    private volatile List<Location> chests = List.of();
    private volatile Map<Location, BlockData> dynamicMarkers = Map.of();
    private volatile Map<Location, BlockData> dynamicChests = Map.of();
    private final Map<Location, LootPool> unopened = new ConcurrentHashMap<>();

    LootChests(GenericStructure map, SkyWarsLoot loot) {
        this.map = map;
        this.loot = loot;
    }

    @Nullable
    LootPool poolFor(Material material) {
        return switch (material) {
            case COPPER_CHEST, WAXED_COPPER_CHEST -> loot.getCommon();
            case EXPOSED_COPPER_CHEST, WAXED_EXPOSED_COPPER_CHEST -> loot.getUncommon();
            case WEATHERED_COPPER_CHEST, WAXED_WEATHERED_COPPER_CHEST -> loot.getRare();
            case OXIDIZED_COPPER_CHEST, WAXED_OXIDIZED_COPPER_CHEST -> loot.getEpic();
            default -> null;
        };
    }

    CompletableFuture<Void> find() {
        return map.findBlockDataAsync(data -> poolFor(data.getMaterial()) != null || data.getMaterial() == Material.CHEST)
                .thenAccept(found -> {
                    List<Location> chests = new ArrayList<>();
                    Map<Location, BlockData> markers = new LinkedHashMap<>();
                    found.forEach((location, data) -> {
                        if (data.getMaterial() == Material.CHEST) {
                            markers.put(location, data);
                        } else {
                            chests.add(location);
                        }
                    });
                    this.chests = chests;
                    this.dynamicMarkers = markers;
                });
    }

    void activateDynamic(int teamSize) {
        Map<Location, BlockData> markers = this.dynamicMarkers;
        Location center = map.getOrigin();
        
        List<List<Location>> units = new ArrayList<>();
        Set<Location> grouped = new HashSet<>();
        markers.forEach((location, data) -> {
            if (!grouped.add(location)) return;
            List<Location> unit = new ArrayList<>(List.of(location));
            Location partner = partnerOf(location, data);
            if (partner != null && markers.containsKey(partner) && grouped.add(partner)) {
                unit.add(partner);
            }
            units.add(unit);
        });
        units.sort(Comparator.comparingDouble(unit -> horizontalDistance(unit.getFirst(), center)));

        double share = Math.clamp((teamSize - 1) / (double) (USE_ALL_DYNAMIC_CHESTS_AT_TEAM_SIZE - 1), 0, 1);
        int count = (int) Math.round(units.size() * share);
        Map<Location, BlockData> placements = new HashMap<>();
        if (!units.isEmpty()) {
            double nearest = horizontalDistance(units.getFirst().getFirst(), center);
            double furthest = horizontalDistance(units.getLast().getFirst(), center);
            for (List<Location> unit : units.subList(0, count)) {
                double outwards = furthest == nearest ? 0 : (horizontalDistance(unit.getFirst(), center) - nearest) / (furthest - nearest);
                Material tier = DYNAMIC_CHEST_TIERS[Math.min(DYNAMIC_CHEST_TIERS.length - 1, (int) (outwards * DYNAMIC_CHEST_TIERS.length))];
                for (Location half : unit) {
                    BlockData chest = tier.createBlockData();
                    markers.get(half).copyTo(chest);
                    placements.put(half, chest);
                }
            }
        }
        this.dynamicChests = placements;
        SkyWars.LOGGER.info("Using " + count + " of " + units.size() + " dynamic SkyWars chests for teams of " + teamSize);
    }

    @Nullable
    private static Location partnerOf(Location location, BlockData data) {
        if (!(data instanceof org.bukkit.block.data.type.Chest chest) || chest.getType() == org.bukkit.block.data.type.Chest.Type.SINGLE) {
            return null;
        }
        BlockFace clockwise = switch (chest.getFacing()) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
        BlockFace side = chest.getType() == org.bukkit.block.data.type.Chest.Type.LEFT ? clockwise : clockwise.getOppositeFace();
        return location.clone().add(side.getModX(), 0, side.getModZ());
    }

    private static double horizontalDistance(Location a, Location b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    CompletableFuture<Void> fillAll(Predicate<Location> shouldUpgrade, DoubleConsumer onProgress) {
        Map<Location, BlockData> dynamicChests = this.dynamicChests;
        List<Location> chests = new ArrayList<>(this.chests);
        chests.addAll(dynamicChests.keySet());
        if (chests.isEmpty()) {
            SkyWars.LOGGER.warning("No copper chests found in the SkyWars schematic " + map.getLocalSchemPath());
        }
        AtomicInteger done = new AtomicInteger();
        AtomicInteger filled = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(chests.size());
        for (Location location : chests) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            Executors.runSync(location, () -> {
                try {
                    Block block = location.getBlock();
                    BlockData dynamicChest = dynamicChests.get(location);
                    if (dynamicChest != null) {
                        block.setBlockData(dynamicChest, false);
                    }
                    if (shouldUpgrade.test(location)) {
                        upgrade(block);
                    }
                    LootPool pool = poolFor(block.getType());
                    if (pool != null && block.getState(false) instanceof Chest chest) {
                        fill(pool, chest.getBlockInventory());
                        unopened.put(block.getLocation(), pool);
                        filled.incrementAndGet();
                    } else {
                        SkyWars.LOGGER.warning("Expected a copper chest at " + describe(location) + " but found " + block.getType());
                    }
                    onProgress.accept((double) done.incrementAndGet() / chests.size());
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRun(() -> SkyWars.LOGGER.info("Filled " + filled.get() + " of " + chests.size() + " SkyWars chests"
                        + (chests.isEmpty() ? "" : " (e.g. at " + describe(chests.getFirst()) + ")")));
    }

    private static String describe(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private void upgrade(Block block) {
        Material next = switch (block.getType()) {
            case COPPER_CHEST -> Material.EXPOSED_COPPER_CHEST;
            case EXPOSED_COPPER_CHEST -> Material.WEATHERED_COPPER_CHEST;
            case WEATHERED_COPPER_CHEST -> Material.OXIDIZED_COPPER_CHEST;
            case WAXED_COPPER_CHEST -> Material.WAXED_EXPOSED_COPPER_CHEST;
            case WAXED_EXPOSED_COPPER_CHEST -> Material.WAXED_WEATHERED_COPPER_CHEST;
            case WAXED_WEATHERED_COPPER_CHEST -> Material.WAXED_OXIDIZED_COPPER_CHEST;
            default -> null;
        };
        if (next == null) return;

        BlockData upgraded = next.createBlockData();
        block.getBlockData().copyTo(upgraded);
        block.setBlockData(upgraded, false);
    }

    int claim(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder(false);
        List<InventoryHolder> halves = holder instanceof DoubleChest doubleChest
                ? Arrays.asList(doubleChest.getLeftSide(false), doubleChest.getRightSide(false))
                : Collections.singletonList(holder);

        int points = 0;
        for (InventoryHolder half : halves) {
            LootPool pool = half instanceof Chest chest ? unopened.remove(chest.getLocation()) : null;
            if (pool != null) {
                points += pool.getPoints();
            }
        }
        return points;
    }

    void fill(LootPool pool, Inventory inventory) {
        inventory.clear();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);

        List<ItemStack> items = spread(roll(pool, random), slots.size(), random);
        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), items.get(i));
        }
    }

    private List<ItemStack> roll(LootPool pool, Random random) {
        List<ItemStack> items = new ArrayList<>();
        for (Map<String, LootPackage> category : pool.getGuaranteed().values()) {
            items.addAll(toItemStacks(pick(category.values(), random)));
        }

        int rolls = random.nextInt(pool.getMinRandomRolls(), pool.getMaxRandomRolls() + 1);
        for (int i = 0; i < rolls; i++) {
            items.addAll(toItemStacks(pick(pool.getRandom().values(), random)));
        }
        return items;
    }

    private List<ItemStack> spread(List<ItemStack> rolled, int slotCount, Random random) {
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> splittable = new ArrayList<>();
        for (ItemStack item : rolled) {
            (item.getAmount() > 1 ? splittable : items).add(item);
        }

        while (slotCount - items.size() - splittable.size() > 0 && !splittable.isEmpty()) {
            ItemStack stack = splittable.remove(random.nextInt(splittable.size()));
            int splitAmount = random.nextInt(1, stack.getAmount() / 2 + 1);
            ItemStack split = stack.asQuantity(splitAmount);
            stack.setAmount(stack.getAmount() - splitAmount);

            for (ItemStack part : List.of(stack, split)) {
                (part.getAmount() > 1 && random.nextBoolean() ? splittable : items).add(part);
            }
        }

        items.addAll(splittable);
        Collections.shuffle(items, random);
        return items;
    }

    private LootPackage pick(Collection<LootPackage> packages, Random random) {
        int total = 0;
        for (LootPackage lootPackage : packages) {
            total += lootPackage.getWeight();
        }

        int roll = random.nextInt(total);
        for (LootPackage lootPackage : packages) {
            roll -= lootPackage.getWeight();
            if (roll < 0) {
                return lootPackage;
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private List<ItemStack> toItemStacks(LootPackage lootPackage) {
        return lootPackage.getItems().stream()
            .map(item -> ItemStack.of(item.getMaterial(), item.getCount()))
            .toList();
    }
}
