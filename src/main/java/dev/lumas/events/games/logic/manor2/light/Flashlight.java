package dev.lumas.events.games.logic.manor2.light;

import dev.lumas.events.configurable.sectors.tracing.ConeConfig;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.latency.tracing.ConeTraceResult;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.datacomponent.item.UseEffects;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Flashlight {

    private static final int MIN_ILLUMINANCE = 7;
    private static final int MAX_ILLUMINANCE = 12;
    private static final double MAX_FLICKER_START_CHANCE = 0.15;
    private static final int MAX_FLICKER_EXTRA_TICKS = 5;
    private static final double MAX_BLACKOUT_CHANCE = 0.6;
    private static final int FLICKER_DIMMING = 3;
    private static final float MAX_BATTERY_CHARGE = 100f;
    private static final float BATTERY_DRAIN_PER_TICK = 100f / (20f * 40f);
    private static final float BATTERY_SELF_CHARGE_PER_TICK = 100f / (20f * 20f);
    private static final int LIGHT_BLOCK_LIFETIME = 4;
    private static final int FLASHLIGHT_DISTANCE = 8;
    private static final double LIGHT_SAMPLE_STEP = 0.25;
    private static final int DUST_SPAWNS_PER_TICK = 3;
    private static final double DUST_PITCH_MAX_VALUE = 70;
    private static final int DUST_PITCH_DOWN_LOOK_RADIUS = 1;
    private static final BlockFace[] LIGHT_FALLBACK_FACES = {
            BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN
    };


    private final Manor2 manor2;
    private final Player player;

    @Getter
    private float batteryCharge = 100f;
    @Getter
    private boolean active = false;

    private int flickerTicksRemaining = 0;
    private boolean flickerOff = false;
    private final ConeConfig coneConfig = new ConeConfig();

    public Flashlight(Manor2 manor2, Player player) {
        this.manor2 = manor2;
        this.player = player;
    }

    public void tick(double ghostCloseness) {
        if (!isInUse()) {
            batteryCharge = Math.clamp(batteryCharge + BATTERY_SELF_CHARGE_PER_TICK, 0f, 100f);
            updateItem();
            return;
        }

        active = batteryCharge > 0f;
        if (!active) {
            return;
        }

        drainBattery();
        updateItem();

        if (batteryCharge <= 0f) {
            active = false;
            return;
        }

        int illuminance = getIlluminance(ghostCloseness);
        if (illuminance <= 0) {
            return;
        }
        coneConfig.setRange(FLASHLIGHT_DISTANCE);
        coneConfig.setConeDegrees(Math.atan2((double) illuminance / 2, FLASHLIGHT_DISTANCE) * 180 / Math.PI);
        Collection<ConeTraceResult> results = manor2.getTracing().traceCone(
                player,
                livingEntity -> manor2.getRoleMap().as(livingEntity.getUniqueId(), Ghost.class) != null,
                this::compareConeTraceResults,
                coneConfig
        );
        results.stream()
                .map(traceResult -> LightHit.of(traceResult, illuminance, coneConfig.getConeDegrees()))
                .forEach(lightHit -> {
                    if (manor2.getRoleMap().as(lightHit.target().getUniqueId(), Ghost.class) instanceof Ghost ghost) {
                        ghost.light(lightHit, player.getUniqueId());
                    }
                });
        illuminate(illuminance);
        placeDust();
    }

    private void placeDust() {
        Location playerLocation = player.getEyeLocation();
        if (playerLocation.getPitch() < -DUST_PITCH_MAX_VALUE) {
            return;
        }
        Vector direction = playerLocation.getDirection();
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < DUST_SPAWNS_PER_TICK; i++) {
            Location lookAt;
            if (direction.equals(new Vector(0, -1, 0))) {
                lookAt = playerLocation.clone()
                        .add(random.nextInt(-DUST_PITCH_DOWN_LOOK_RADIUS, DUST_PITCH_DOWN_LOOK_RADIUS + 1),
                                0,
                                random.nextInt(-DUST_PITCH_DOWN_LOOK_RADIUS, DUST_PITCH_DOWN_LOOK_RADIUS + 1)
                        );
            } else {
                lookAt = playerLocation.clone().add(direction.clone().rotateAroundY(random.nextDouble(
                                -Math.toRadians(coneConfig.getConeDegrees()), Math.toRadians(coneConfig.getConeDegrees())
                        )).multiply(random.nextDouble((double) FLASHLIGHT_DISTANCE / 2, FLASHLIGHT_DISTANCE))
                );
            }
            Block spot = lookAt.getBlock();
            int dy = 1;
            while (!spot.getRelative(0, -dy, 0).isSolid()) {
                dy++;
            }
            final int dyFinal = dy;
            manor2.getDefinition().getFlashlightGroundParticles()
                    .forEach(particleDefinition -> particleDefinition.showAt(spot.getRelative(0, -dyFinal, 0).getLocation()));
        }
    }

    private int compareConeTraceResults(ConeTraceResult coneTraceResult1, ConeTraceResult coneTraceResult2) {
        if (coneTraceResult1.z() == 0) {
            return 1;
        }
        return Double.compare(LightHit.centeredness(coneTraceResult2, coneConfig.getConeDegrees()), LightHit.centeredness(coneTraceResult1, coneConfig.getConeDegrees())); // less is more for this case
    }

    private void drainBattery() {
        batteryCharge = Math.max(batteryCharge - BATTERY_DRAIN_PER_TICK, 0f);
    }

    private boolean isInUse() {
        return player.hasActiveItem() && isFlashlight(player.getActiveItem());
    }

    private int getBaseIlluminance() {
        if (batteryCharge <= 0f) return 0;
        double percentage = batteryCharge / MAX_BATTERY_CHARGE;
        double brightness = Math.sqrt(percentage);
        return MIN_ILLUMINANCE + (int) Math.round((MAX_ILLUMINANCE - MIN_ILLUMINANCE) * brightness);
    }

    private int getIlluminance(double ghostCloseness) {
        int base = getBaseIlluminance();
        if (base <= 0) {
            return 0;
        }
        if (flickerTicksRemaining > 0) {
            flickerTicksRemaining--;
            return flickerOff ? 0 : Math.max(base - FLICKER_DIMMING, MIN_ILLUMINANCE);
        }
        if (ghostCloseness <= 0 || Math.random() >= ghostCloseness * MAX_FLICKER_START_CHANCE) {
            flickerOff = false;
            return base;
        }

        flickerTicksRemaining = (int) Math.round(ghostCloseness * MAX_FLICKER_EXTRA_TICKS);
        flickerOff = Math.random() < ghostCloseness * MAX_BLACKOUT_CHANCE;
        return flickerOff ? 0 : Math.max(base - FLICKER_DIMMING, MIN_ILLUMINANCE);
    }

    private void illuminate(int illuminance) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        List<Block> beam = new ArrayList<>();
        int maxSteps = (int) Math.ceil(FLASHLIGHT_DISTANCE / LIGHT_SAMPLE_STEP);
        for (int i = 0; i <= maxSteps; i++) {
            Block block = eye.clone().add(direction.clone().multiply(i * LIGHT_SAMPLE_STEP)).getBlock();
            if (blocksBeam(block)) break;
            if (beam.isEmpty() || !beam.getLast().equals(block)) beam.add(block);
        }
        if (beam.isEmpty()) return;

        Location end = beam.getLast().getLocation().toCenterLocation();
        Map<Block, Integer> lights = new HashMap<>();
        for (Block block : beam) {
            double distanceFromEnd = block.getLocation().toCenterLocation().distance(end);
            int lightLevel = (int) Math.floor(illuminance * (1 - distanceFromEnd / FLASHLIGHT_DISTANCE));
            if (lightLevel <= 0) continue;

            Block spot = findLightSpot(block, lightLevel);
            if (spot == null) continue;
            lights.merge(spot, lightLevel, Math::max);
        }

        lights.forEach((spot, lightLevel) -> {
            BlockData blockData = BlockType.LIGHT.createBlockData(light -> {
                light.setLevel(lightLevel);
                light.setWaterlogged(spot.getType() == Material.WATER);
            });
            manor2.getBlockCache().setBlock(spot.getLocation(), blockData, LIGHT_BLOCK_LIFETIME);
        });
    }

    private @Nullable Block findLightSpot(Block block, int lightLevel) {
        if (canHoldLight(block, lightLevel)) return block;
        for (BlockFace face : LIGHT_FALLBACK_FACES) {
            Block relative = block.getRelative(face);
            if (canHoldLight(relative, lightLevel)) return relative;
        }
        return null;
    }

    private boolean canHoldLight(Block block, int lightLevel) {
        BlockData data = block.getBlockData();
        if (data instanceof Light light) return light.getLevel() < lightLevel;
        if (block.getType().isAir()) return true;
        return block.getType() == Material.WATER && data instanceof Levelled water && water.getLevel() == 0;
    }

    private boolean blocksBeam(Block block) {
        if (block.getType().isOccluding()) return true;
        return block.getBlockData() instanceof Slab slab && slab.getType() == Slab.Type.DOUBLE;
    }

    private void updateItem() {
        for (ItemStack itemStack : player.getInventory()) {
            if (!isFlashlight(itemStack)) {
                continue;
            }
            itemStack.setData(DataComponentTypes.DAMAGE, 101 - Math.round(batteryCharge));
        }
    }

    public ItemStack createItem() {
        ItemStack item = ItemStack.of(Material.TRIDENT);
        item.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("spyglass"));
        item.setData(DataComponentTypes.CUSTOM_NAME, Util.color("<gold><bold>Flashlight"));
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                .addHiddenComponents(DataComponentTypes.DAMAGE)
                .build());
        item.editPersistentDataContainer(pdc -> {
            pdc.set(Manor2.ITEM_TAG, PersistentDataType.STRING, "flashlight");
        });
        item.setData(DataComponentTypes.MAX_DAMAGE, 101);
        item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .canSprint(true)
                .speedMultiplier(1.0f)
                .interactVibrations(true)
                .build());
        item.setData(DataComponentTypes.DAMAGE, 101 - Math.round(batteryCharge));
        return item;
    }

    public static boolean isFlashlight(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        String tag = item.getPersistentDataContainer().get(Manor2.ITEM_TAG, PersistentDataType.STRING);
        return "flashlight".equals(tag);
    }


}
