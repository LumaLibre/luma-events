package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.Manor2Definition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.packet.PacketGlow;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import dev.lumas.events.utility.latency.LatencyTracker;
import dev.lumas.glowapi.model.GlowColorManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class Manor2 extends InventoryUnifiedMinigame {
    private static final String[] SPLASH = {
            "Are you afraid of ghosts?",
            "Boooooooooooo!!!!",
            "Ghost Busters!",
            "Who you gonna call?",
            "House of Dracula!",
            "Trick or treat?",
            "Here's Johny!!!",
            "Put these foolish ambitions to rest",
            "Wazzzuup!",
            "Did you pray enough?",
            "It's alive! It's alive!",
            "Be afraid. Be very afraid",
            "What is the color of night?",
            "This is not a dream!",
            "Someone must extinguish thy flame."
    };
    private static final long TICK_INTERVAL = 1L;
    private static final int TELEPORTS_PER_TICK = 5;
    private static final long END_DELAY_SECONDS = 10;
    private static final int TRAIL_HISTORY_TICKS = 20;
    private static final double HAUNT_RADIUS = 10.0;
    // Mitality's secret
    private static final double[] BODY_SAMPLE_FRACTIONS = {0.2, 0.5, 0.8};
    private static final double BEAM_STEP = 0.5;
    private static final long MAX_REWIND_NANOS = TimeUnit.MILLISECONDS.toNanos((TRAIL_HISTORY_TICKS - 4) * 50L);
    // End of mitality's secret
    private static final NamespacedKey TAGGED = new NamespacedKey(EventMain.getInstance(), "manor2/item_tag");
    private final Location lobbyLocation;
    private final List<Location> huntersStartLocations;
    private final List<Location> ghostStartLocations;
    private final int playerRatio;
    private final MinigameRoleMap<Manor2Role> roleMap = new MinigameRoleMap<>();
    private final int ghostRevealTime;
    private final int hunterDeathTime;
    private final Manor2Definition.LagCompensationSettings lagCompensation;

    private final ConcurrentHashMap<UUID, Trail> trails = new ConcurrentHashMap<>();
    private final LocalizedFakeBlockCache blockCache = new LocalizedFakeBlockCache(32);
    private final LatencyTracker latency = new LatencyTracker(this::onlineParticipants);
    private final ConcurrentHashMap<UUID, Recorder> recorders = new ConcurrentHashMap<>();
    private final PacketGlow packetGlow = PacketGlow.newInstance();
    private final GhostAttacks ghostAttacks = new GhostAttacks();
    private CountdownBossBar timer;

    public Manor2(Manor2Definition definition) {
        super("Manor 2", Util.getRandom(SPLASH), definition.getDuration(), TICK_INTERVAL, true, true, false, false);
        this.lobbyLocation = definition.getLobby();
        this.huntersStartLocations = definition.getHunterStartLocations();
        this.ghostStartLocations = definition.getGhostStartLocations();
        this.playerRatio = definition.getHunterRatio();
        this.ghostRevealTime = definition.getGhostRevealTime();
        this.hunterDeathTime = definition.getHunterDeathTime();
        this.lagCompensation = definition.getLagCompensation();
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    @Override
    protected void handleStart() {
        this.latency.start(); // No-op if already started
        Collections.shuffle(participants);
        int ghostsToSpecify = Math.ceilDiv(participants.size(), playerRatio);
        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i).getPlayer();
            if (player == null) {
                continue;
            }
            Manor2Role role;
            List<Location> teleportTargets;
            if (ghostsToSpecify-- > 0) {
                role = new Ghost(participants.get(i));
                teleportTargets = ghostStartLocations;
            } else {
                role = new GhostHunter(participants.get(i));
                teleportTargets = huntersStartLocations;
            }
            roleMap.put(role);
            Location target = teleportTargets.get(i % teleportTargets.size());
            Executors.delayedSync(player, Math.floorDiv(i, TELEPORTS_PER_TICK) + 1, () -> {
                        Executors.teleportSafely(player, target);
                        role.initiatePlay();
                    }
            );
        }
        showParticipantsToEachOther();
        packetGlow.start();
        Executors.repeatingSync(huntersStartLocations.getFirst(), 1L, task -> {
            if (stopping) {
                task.cancel();
                return;
            }
            blockCache.showSync();
        });

        timer = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
                .miliseconds(getDuration())
                .audience(audience)
                .build();
        timer.start();
    }

    private void startRecording(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        Recorder running = recorders.get(uuid);
        if (running != null && running.entity() == entity && !running.task().isCancelled()) return;

        if (running != null) running.task().cancel();

        Trail trail = new Trail();
        ScheduledTask task = Executors.repeatingSync(entity, 1L, _ -> trail.record(entity));
        if (task == null) return; // Already gone, the next sweep can try again

        trails.put(uuid, trail);
        recorders.put(uuid, new Recorder(entity, task));
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.stop();
        }
        tickRecorders();
        ghostAttacks.tick();
        for (Manor2Role role : roleMap) {
            role.tick();
            role.operatePlayer(player -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 0, false, false, false));
            });
        }
    }

    @Override
    protected void handleStop() {
        if (timer != null) timer.stop(false);

        packetGlow.stop();
        ghostAttacks.stop();

        Executors.sync(huntersStartLocations.getFirst(), blockCache::shutdown);

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(role.retire());
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .join();

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.latency.start(); // No-op if already started
        player.teleportAsync(lobbyLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer player, boolean doTeleport) {
        if (player.getPlayer() != null && this.timer != null) {
            this.timer.getBossBar().removeViewer(player.getPlayer());
        }
        if (roleMap.get(player.getUuid()) instanceof Ghost ghost) {
            ghost.visibility.stop();
        }
        ghostAttacks.abort(player.getUuid());
        return super.removeParticipant(player, doTeleport);
    }

    @EventHandler
    public void onTridentThrow(PlayerLaunchProjectileEvent event) {
        this.ensureNotIllegal();
        if (!(event.getProjectile() instanceof Trident)) return;
        if (!Flashlight.isFlashlight(event.getItemStack())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onGhostAttack(PrePlayerAttackEntityEvent event) {
        this.ensureNotIllegal();
        if (ghostAttacks.handleAttack(event.getPlayer(), event.getAttacked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        this.ensureNotIllegal();
        if (ghostAttacks.handleSwapHands(event.getPlayer())) {
            event.setCancelled(true);
        }
    }


    private void showParticipantsToEachOther() {
        List<Player> players = onlineParticipants();
        for (Player viewer : players) {
            Executors.runSync(viewer, () -> {
                for (Player other : players) {
                    if (other != viewer) viewer.showPlayer(EventMain.getInstance(), other);
                }
            });
        }
    }

    private List<Player> onlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player != null) players.add(player);
        }
        return players;
    }

    private void tickRecorders() {
        Set<UUID> live = new HashSet<>();

        for (Player player : onlineParticipants()) {
            live.add(player.getUniqueId());
            startRecording(player);
        }

        recorders.entrySet().removeIf(entry -> {
            if (live.contains(entry.getKey())) return false;
            entry.getValue().task().cancel();
            return true;
        });
        trails.keySet().removeIf(uuid -> !live.contains(uuid));
    }

    private record Recorder(LivingEntity entity, ScheduledTask task) {
    }

    private abstract class Manor2Role extends MinigameRole {

        protected Manor2Role(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        public CompletableFuture<Void> retire() {
            CompletableFuture<Void> completed = new CompletableFuture<>();
            this.operatePlayer(player -> {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) instanceof ItemStack itemStack && Util.hasPersistentKey(itemStack, TAGGED)) {
                        inventory.setItem(i, null);
                    }
                }
                player.clearActivePotionEffects();
                player.setFreezeTicks(0);
                GhostAttacks.NiceRestraint.release(player); // In case a grab never got to free them
                GlowColorManager.getInstance().update(player);
                packetGlow.removeTarget(player);
                packetGlow.removeViewer(player);
                completed.complete(null);
            });
            return completed;
        }

        public abstract void tick();

        public abstract void die();

        public abstract CompletableFuture<Void> initiatePlay();
    }

    private class Ghost extends Manor2Role {

        private static final int WHIRL_INTERVAL_TICKS = 80;
        private static final float WHIRL_VOLUME = 0.6f;

        private final GhostVisibility visibility;
        private int revealedTime = 0;
        private int ticksUntilWhirl = 0;

        protected Ghost(EventPlayer eventPlayer) {
            super(eventPlayer);
            this.visibility = new GhostVisibility(eventPlayer);
            eventPlayer.operatePlayer(player -> {
                GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.DARK_GRAY);
                packetGlow.addViewer(player);
            });
        }

        @Override
        public void die() {

        }

        private void showRevealedResult() {
            eventPlayer.operatePlayer(player -> {
                AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    double newHealth = maxHealth.getValue() * (1 - (double) revealedTime / ghostRevealTime);
                    player.setHealth(newHealth);
                } else {
                    EventMain.getInstance().getLogger().warning("Player has no max health attribute - can not take damage");
                }
            });
        }

        @Override
        public CompletableFuture<Void> initiatePlay() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            eventPlayer.operatePlayer(player -> {
                future.complete(null);
            });
            return future;
        }

        public void tick() {
            visibility.tick();
            whirl();

            if (visibility.getState().isRevealed()) revealedTime++;
            else revealedTime = 0; // <- TODO: intended?
        }

        private void whirl() {
            if (--ticksUntilWhirl > 0 || !participants.contains(eventPlayer)) return;
            ticksUntilWhirl = WHIRL_INTERVAL_TICKS;

            float pitch = 0.8f + (ThreadLocalRandom.current().nextFloat() * 0.4f);
            operatePlayer(player -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WHIRL, SoundCategory.HOSTILE, WHIRL_VOLUME, pitch));
        }

        public void light(LightHit hit, UUID playerUuid) {
            visibility.light(hit, playerUuid);
            ghostAttacks.onLit(this, hit, playerUuid);
        }

        @Override
        public CompletableFuture<Void> retire() {
            visibility.stop();
            return super.retire();
        }
    }

    private double hauntCloseness(Player player) {
        Vector center = player.getBoundingBox().getCenter();
        double closest = HAUNT_RADIUS;
        for (Target ghost : targetsOf(System.nanoTime(), role -> role instanceof Ghost && participants.contains(role.getEventPlayer()))) {
            if (ghost.entity() == player || !ghost.entity().getWorld().equals(player.getWorld())) continue;
            closest = Math.min(closest, ghost.hitbox().getCenter().distance(center));
        }
        return 1 - (closest / HAUNT_RADIUS);
    }

    private List<Target> targetsOf(long atTimestamp, Predicate<Manor2Role> rolePredicate) {
        List<Target> targets = new ArrayList<>();

        for (Manor2Role participant : this.roleMap) {
            if (!rolePredicate.test(participant)) {
                continue;
            }
            Player player = participant.getEventPlayer().getPlayer();
            if (player == null) {
                continue;
            }

            Target target = targetAt(player, atTimestamp);
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    private long rewindOf(Player shooter) {
        if (!lagCompensation.isEnabled()) return 0L;

        long cap = Math.min(MAX_REWIND_NANOS,
                TimeUnit.MILLISECONDS.toNanos(Math.max(0, lagCompensation.getMaxRewindMillis())));
        long asked = TimeUnit.MILLISECONDS.toNanos(
                latency.pingMillis(shooter) + Math.max(0, lagCompensation.getInterpolationMillis()));

        return Math.clamp(asked, 0L, cap);
    }

    private Target targetAt(LivingEntity entity, long at) {
        Trail trail = trails.get(entity.getUniqueId());
        Snapshot snapshot = trail == null ? null : trail.at(at);
        if (snapshot != null) return new Target(entity, snapshot.hitbox(), snapshot.eyeHeight());

        return Bukkit.isOwnedByCurrentRegion(entity)
                ? new Target(entity, entity.getBoundingBox(), entity.getEyeHeight())
                : null;
    }

    private static final class Trail {

        private final Snapshot[] snapshots = new Snapshot[TRAIL_HISTORY_TICKS];
        private volatile int written;

        private void record(LivingEntity entity) {
            int index = this.written;
            snapshots[Math.floorMod(index, TRAIL_HISTORY_TICKS)] =
                    new Snapshot(System.nanoTime(), entity.getBoundingBox(), entity.getEyeHeight());
            this.written = index + 1;
        }

        private void clear() {
            Arrays.fill(snapshots, null);
            this.written = 0;
        }

        // The hitbox this entity had at <when>, interpolated between the ticks either side of it
        private @Nullable Snapshot at(long when) {
            int count = this.written; // Reading this first "publishes" every snapshot below it

            Snapshot newer = null;
            for (int age = 0; age < Math.min(count, TRAIL_HISTORY_TICKS); age++) {
                Snapshot snapshot = snapshots[Math.floorMod(count - 1 - age, TRAIL_HISTORY_TICKS)];
                if (snapshot == null) break;
                if (snapshot.takenAt() <= when) {
                    return newer == null ? snapshot : between(snapshot, newer, when);
                }
                newer = snapshot;
            }
            return newer;
        }

        private static Snapshot between(Snapshot older, Snapshot newer, long when) {
            long span = newer.takenAt() - older.takenAt();
            if (span <= 0) return newer;

            double progress = Math.clamp((double) (when - older.takenAt()) / span, 0.0, 1.0);
            BoundingBox from = older.hitbox();
            BoundingBox to = newer.hitbox();

            return new Snapshot(when, new BoundingBox(
                    lerp(from.getMinX(), to.getMinX(), progress),
                    lerp(from.getMinY(), to.getMinY(), progress),
                    lerp(from.getMinZ(), to.getMinZ(), progress),
                    lerp(from.getMaxX(), to.getMaxX(), progress),
                    lerp(from.getMaxY(), to.getMaxY(), progress),
                    lerp(from.getMaxZ(), to.getMaxZ(), progress)),
                    lerp(older.eyeHeight(), newer.eyeHeight(), progress));
        }

        private static double lerp(double from, double to, double progress) {
            return from + ((to - from) * progress);
        }
    }

    private record Snapshot(long takenAt, BoundingBox hitbox, double eyeHeight) {
    }

    private class GhostHunter extends Manor2Role {

        private static final int FROST_PER_TICK = 4;

        private int ghostConsumedTime = 0;

        private Flashlight flashlight;

        protected GhostHunter(EventPlayer eventPlayer) {
            super(eventPlayer);
            this.flashlight = new Flashlight(eventPlayer.getPlayer());
            eventPlayer.operatePlayer(player -> {
                GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.WHITE);
                packetGlow.addTarget(player);
            });
        }

        @Override
        public void tick() {
            eventPlayer.operatePlayer(player -> {
                double ghostCloseness = hauntCloseness(player);
                flashlight.tick(ghostCloseness);
                freeze(player, ghostCloseness);
            });
        }

        private void freeze(Player player, double ghostCloseness) {
            int target = (int) Math.round((player.getMaxFreezeTicks() - 1) * ghostCloseness);
            int current = player.getFreezeTicks();
            if (current < target) {
                player.setFreezeTicks(Math.min(target, current + FROST_PER_TICK));
            }
        }

        @Override
        public void die() {
            retire(); // TODO: extract parts of this
            roleMap.swapRole(this, () -> new Spectator(eventPlayer));
        }

        @Override
        public CompletableFuture<Void> initiatePlay() {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            eventPlayer.operatePlayer(player -> {
                player.getInventory().setItem(0, flashlight.createItem());
                completableFuture.complete(null);
            });
            return completableFuture;
        }

        public void consumeTick(Ghost attacker) {

        }
    }

    private class Spectator extends Manor2Role {

        protected Spectator(EventPlayer eventPlayer) {
            super(eventPlayer);
            eventPlayer.operatePlayer(player -> {
                GlowColorManager.getInstance().update(player);
                packetGlow.removeTarget(player);
                packetGlow.addViewer(player);
            });
        }

        @Override
        public void tick() {

        }

        @Override
        public void die() {

        }

        @Override
        public CompletableFuture<Void> initiatePlay() {
            return null;
        }
    }

    private class Flashlight {

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
        private static final BlockFace[] LIGHT_FALLBACK_FACES = {
                BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN
        };

        private final Player player;

        @Getter
        private float batteryCharge = 100f;
        @Getter
        private boolean active = false;

        private int flickerTicksRemaining = 0;
        private boolean flickerOff = false;

        public Flashlight(Player player) {
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
            // rewindOf is how far back to look, the trails want the moment itself
            triggerLightCheck(player, FLASHLIGHT_DISTANCE, illuminance, targetsOf(System.nanoTime() - rewindOf(player), Ghost.class::isInstance));
            illuminate(illuminance);
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
            if (base <= 0) return 0;

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
                blockCache.setBlock(spot.getLocation(), blockData, LIGHT_BLOCK_LIFETIME);
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
                    .build());
            item.editPersistentDataContainer(pdc -> {
                pdc.set(TAGGED, PersistentDataType.STRING, "flashlight");
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
            String tag = item.getPersistentDataContainer().get(TAGGED, PersistentDataType.STRING);
            return "flashlight".equals(tag);
        }

        private void triggerLightCheck(Player shooter, double lightDistance, double maxLight, Collection<Target> targets) {
            Location muzzle = shooter.getEyeLocation();
            Vector direction = muzzle.getDirection().normalize();
            double range = Math.max(0.5, lightDistance);

            Map<Ghost, LightHit> hit = new HashMap<>();
            double hitWidth = maxLight / 2;
            double tanHalfAngle = hitWidth / lightDistance;
            double cosHalfAngle = lightDistance / Math.sqrt(hitWidth * hitWidth + lightDistance * lightDistance);
            Vector apex = muzzle.toVector();

            for (Target target : targets) {
                BoundingBox hitbox = target.hitbox();
                LightHit lightHit = coneHit(apex, direction, range, maxLight, tanHalfAngle, cosHalfAngle, hitbox);
                if (lightHit == null) {
                    continue;
                }

                // Occlusion has to be checked towards the target, not down the middle of the cone / Mitality
                Vector toTarget = hitbox.getCenter().subtract(apex);
                double length = toTarget.length();
                if (length > 1.0E-4 && !hasClearShot(muzzle, toTarget.multiply(1.0 / length), length)) {
                    continue;
                }

                if (target.entity instanceof Player hitPlayer) {
                    Ghost ghost = roleMap.as(hitPlayer.getUniqueId(), Ghost.class);
                    if (ghost != null) {
                        hit.put(ghost, lightHit);
                    }
                }
            }

            hit.forEach((ghost, lightHit) -> ghost.light(lightHit, shooter.getUniqueId()));
        }

        // Where the cone catches the hitbox: the closest distance to it and how near the centre of the beam it gets
        private static @Nullable LightHit coneHit(Vector apex, Vector axis, double range, double maxLight, double tanHalfAngle, double cosHalfAngle, BoundingBox hitbox) {
            double height = hitbox.getHeight();
            double radius = Math.max(Math.max(hitbox.getWidthX(), hitbox.getWidthZ()) / 2.0, height / 6.0);

            // Nothing beyond this can reach the cone
            double maxReach = range + (height / 2.0) + radius;
            if (apex.distanceSquared(hitbox.getCenter()) > maxReach * maxReach) return null;

            double best = -1;
            double bestCenteredness = 0;
            for (double fraction : BODY_SAMPLE_FRACTIONS) {
                double dx = hitbox.getCenterX() - apex.getX();
                double dy = (hitbox.getMinY() + (height * fraction)) - apex.getY();
                double dz = hitbox.getCenterZ() - apex.getZ();

                double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
                if (distance <= radius) return new LightHit(0, 1, (int) maxLight); // muzzle is inside target

                double along = (dx * axis.getX()) + (dy * axis.getY()) + (dz * axis.getZ());
                if (along < 0 || along - radius > range) continue;

                double offX = dx - (axis.getX() * along);
                double offY = dy - (axis.getY() * along);
                double offZ = dz - (axis.getZ() * along);
                double fromAxis = Math.sqrt((offX * offX) + (offY * offY) + (offZ * offZ));

                // The cone widens with distance, and a sphere of <radius> widens what counts as touching it
                double coneRadius = (along * tanHalfAngle) + (radius / cosHalfAngle);
                if (fromAxis > coneRadius) continue;

                double toSurface = Math.max(0, distance - radius);
                if (toSurface > range) continue;
                if (best < 0 || toSurface < best) best = toSurface;
                bestCenteredness = Math.max(bestCenteredness, 1 - (fromAxis / coneRadius));
            }
            if (best < 0) return null;

            int lightLevel = (int) Math.floor(maxLight * (1 - best / range));
            return new LightHit(best, bestCenteredness, lightLevel);
        }

        private static boolean hasClearShot(Location from, Vector direction, double distance) {
            World world = from.getWorld();
            Vector origin = from.toVector();
            int lastX = Integer.MIN_VALUE;
            int lastY = Integer.MIN_VALUE;
            int lastZ = Integer.MIN_VALUE;

            for (double travelled = BEAM_STEP; travelled < distance; travelled += BEAM_STEP) {
                int blockX = (int) Math.floor(from.getX() + (direction.getX() * travelled));
                int blockY = (int) Math.floor(from.getY() + (direction.getY() * travelled));
                int blockZ = (int) Math.floor(from.getZ() + (direction.getZ() * travelled));

                if (blockX == lastX && blockY == lastY && blockZ == lastZ) continue; // Same block as the last step
                lastX = blockX;
                lastY = blockY;
                lastZ = blockZ;

                if (!Bukkit.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) return true;
                if (solidHitDistance(world.getBlockAt(blockX, blockY, blockZ), origin, direction, distance) >= 0) {
                    return false;
                }
            }
            return true;
        }

        private static double solidHitDistance(Block block, Vector origin, Vector direction, double maxDistance) {
            if (!couldStopShots(block)) return -1;

            BoundingBox bounds = block.getBoundingBox();
            if (bounds.getVolume() <= 0) return -1;
            if (bounds.contains(origin)) return 0; // Fired from inside the block itself

            // <direction> is normalized, so this is the distance along the ray
            RayTraceResult hit = bounds.rayTrace(origin, direction, maxDistance);
            return hit == null ? -1 : hit.getHitPosition().distance(origin);
        }

        private static boolean couldStopShots(Block block) {
            return !block.isPassable() && block.getType() != Material.BARRIER;
        }


    }

    private record Target(LivingEntity entity, BoundingBox hitbox, double eyeHeight) {

        private BoundingBox expandedHitbox(double radius) {
            return hitbox.clone().expand(radius);
        }

        private boolean containsWithin(double radius, double x, double y, double z) {
            return x >= hitbox.getMinX() - radius && x < hitbox.getMaxX() + radius
                    && y >= hitbox.getMinY() - radius && y < hitbox.getMaxY() + radius
                    && z >= hitbox.getMinZ() - radius && z < hitbox.getMaxZ() + radius;
        }
    }

    private enum RevealState {
        HIDDEN, // vanished + invisibility
        SILHOUETTE, // glow + invisibility
        EXPOSED; // glow

        public boolean isRevealed() {
            return this != HIDDEN;
        }
    }

    private record LightHit(double distance, double centeredness, int lightLevel) {
    }

    private record LightHitRecord(LightHit hit, UUID shooter) {
    }

    private final class GhostVisibility {

        private static final int HIDE_AFTER_UNLIT_TICKS = 10;
        private static final double EXPOSED_MIN_CENTEREDNESS = 0.75;
        private static final double SILHOUETTE_MIN_CENTEREDNESS = 0.5;
        private static final int EXPOSED_MIN_LIGHT_LEVEL = 7;
        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, true, false);
        private static final PotionEffect GLOW = new PotionEffect(PotionEffectType.GLOWING, 3, 0, true, true, false);

        private final EventPlayer ghost;
        private final AtomicReference<@Nullable LightHitRecord> pendingLight = new AtomicReference<>(); // strongest light from any hunter
        @Getter
        private volatile RevealState state = RevealState.HIDDEN;
        private int unlitTicks = 0;

        private final Set<UUID> hiddenFrom = ConcurrentHashMap.newKeySet();
        private final Object vanishLock = new Object(); // TODO: probably a better way to do this, i didnt want to deal with the race conditions - Jsinco
        private volatile boolean stopping = false;
        private final AtomicInteger forcedExposedTicks = new AtomicInteger();

        private GhostVisibility(EventPlayer ghost) {
            this.ghost = ghost;
        }

        public void exposeFor(int ticks) {
            forcedExposedTicks.accumulateAndGet(ticks, Math::max);
        }

        public void light(LightHit hit, UUID playerUuid) {
            if (stopping) return;
            LightHitRecord entry = new LightHitRecord(hit, playerUuid);
            pendingLight.accumulateAndGet(entry, (current, next) -> current == null || isStronger(next.hit(), current.hit()) ? next : current);
        }

        public void tick() {
            if (stopping) return;
            LightHitRecord hitRecord = pendingLight.getAndSet(null);
            LightHit hit = hitRecord == null ? null : hitRecord.hit();
            RevealState previous = state;
            if (hit != null) {
                unlitTicks = 0;
                state = stateOf(hit);
            } else if (++unlitTicks >= HIDE_AFTER_UNLIT_TICKS) {
                state = RevealState.HIDDEN;
            }
            if (forcedExposedTicks.getAndUpdate(ticks -> Math.max(0, ticks - 1)) > 0) {
                unlitTicks = 0;
                state = RevealState.EXPOSED;
            }
            boolean changed = state != previous;

//            Bukkit.broadcastMessage("Ghost visibility tick (" + ghost.getName() + "): " + state + ", unlitTicks: " + unlitTicks + (hit == null
//                    ? ", not lit"
//                    : String.format(", distance: %.2f, centeredness: %.2f, light level: %d", hit.distance(), hit.centeredness(), hit.lightLevel())));

            final RevealState current = state;
            ghost.operatePlayer(player -> {
                 // ghost.tickDamage(hitRecord.shooter());
                if (stopping) return; // Released while this was queued
                if (current == RevealState.EXPOSED) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    if (changed) {
                        Location playerLocation = player.getLocation();
                        playerLocation.getWorld().spawnParticle(Particle.DUST_PILLAR, playerLocation, 10, BlockType.RED_CONCRETE.createBlockData());
                    }
                } else {
                    player.addPotionEffect(INVISIBILITY);
                }

                if (current.isRevealed()) {
                    player.addPotionEffect(GLOW);
                    Location playerLocation = player.getLocation();
                    playerLocation.getWorld().spawnParticle(
                        Particle.DUST,
                        playerLocation.clone().add(
                            ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                            ThreadLocalRandom.current().nextDouble(0, 2),
                            ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                        ),
                        1,
                        new Particle.DustOptions(Color.GRAY, 1f)
                    );
                } else {
                    player.removePotionEffect(PotionEffectType.GLOWING);
                }

                setVanished(player, current == RevealState.HIDDEN);
            });
        }

        public void stop() {
            List<UUID> toShow;
            synchronized (vanishLock) {
                if (stopping) return;
                stopping = true;
                toShow = List.copyOf(hiddenFrom);
            }
            pendingLight.set(null);
            state = RevealState.EXPOSED;

            ghost.operatePlayer(player -> {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.removePotionEffect(PotionEffectType.GLOWING);
            });

            Player self = ghost.getPlayer();
            if (self == null) return;
            for (UUID viewerId : toShow) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null) {
                    hiddenFrom.remove(viewerId);
                    continue;
                }

                Executors.runSync(viewer, () -> showTo(viewer, self));
            }
        }

        private static RevealState stateOf(LightHit hit) {
            if (hit.centeredness() < SILHOUETTE_MIN_CENTEREDNESS) return RevealState.HIDDEN;
            boolean exposed = hit.centeredness() >= EXPOSED_MIN_CENTEREDNESS || hit.lightLevel() >= EXPOSED_MIN_LIGHT_LEVEL;
            return exposed ? RevealState.EXPOSED : RevealState.SILHOUETTE;
        }

        private static boolean isStronger(LightHit hit, LightHit than) {
            int byState = stateOf(hit).compareTo(stateOf(than));
            return byState != 0 ? byState > 0 : hit.lightLevel() > than.lightLevel();
        }

        private void setVanished(Player self, boolean vanished) {
            for (Manor2Role role : roleMap) {
                if (role instanceof Ghost) continue; // TODO: ghosts can always see each other?
                Player viewer = role.getEventPlayer().getPlayer();
                if (viewer == null || viewer == self) continue;

                boolean hide = vanished && participants.contains(role.getEventPlayer());
                if (viewer.canSee(self) != hide) continue;

                role.operatePlayer(target -> {
                    if (hide) hideFrom(target, self);
                    else showTo(target, self);
                });
            }
        }

        private void hideFrom(Player viewer, Player self) {
            synchronized (vanishLock) {
                if (stopping) return;
                viewer.hidePlayer(EventMain.getInstance(), self);
                hiddenFrom.add(viewer.getUniqueId());
            }
        }

        private void showTo(Player viewer, Player self) {
            viewer.showPlayer(EventMain.getInstance(), self);
            hiddenFrom.remove(viewer.getUniqueId());
        }
    }


    // Basic idea:
    // - hold both the hunter and ghost in place for 2-3s
    // - allow other hunters to 'free' the attacked player by fighting off the ghost
    // - either the guardian animation or totem animation for jumpscaring
    // - allow the ghost to cancel the attack if they're taking too much damage
    private final class GhostAttacks {

        private static final int GRAB_TICKS = 50;
        private static final int STAGGER_TILL_BREAK = 150;
        private static final double MIN_STAGGER_CENTEREDNESS = 0.5;
        private static final int HEARTBEAT_INTERVAL_TICKS = 12;
        private static final int KILLED_COOLDOWN_TICKS = 100;
        private static final int BROKEN_COOLDOWN_TICKS = 200;
        private static final int LET_GO_COOLDOWN_TICKS = 100;
        private static final int BROKEN_EXPOSE_TICKS = 60;
        private static final int LET_GO_EXPOSE_TICKS = 20;
        private static final int VICTIM_GRACE_TICKS = 60;
        private static final double BROKEN_KNOCKBACK = 1.2;
        private static final String GHOST_FACE_TAG = "ghost_face";

        private final Map<UUID, Grab> grabMap = new ConcurrentHashMap<>();
        private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
        private volatile long currentTick = 0;

        public boolean handleAttack(Player attacker, Entity attacked) {
            Ghost ghost = roleMap.as(attacker.getUniqueId(), Ghost.class);
            if (ghost == null || !(attacked instanceof Player victimPlayer)) return false;
            GhostHunter victim = roleMap.as(victimPlayer.getUniqueId(), GhostHunter.class);
            if (victim == null) return false;

            tryGrab(ghost, attacker, victim, victimPlayer);
            return true;
        }

        public boolean handleSwapHands(Player player) {
            Grab grab = grabMap.get(player.getUniqueId());
            if (grab == null) return false;

            if (grab.ghost.getUuid().equals(player.getUniqueId())) {
                grab.backout();
            }
            return true;
        }

        public void onLit(Ghost ghost, LightHit hit, UUID shooter) {
            Grab grab = grabMap.get(ghost.getUuid());
            if (grab != null && grab.ghost == ghost) {
                grab.light(hit, shooter);
            }
        }

        public void tick() {
            currentTick++;
            for (Grab grab : Set.copyOf(grabMap.values())) {
                grab.tick();
            }
        }

        public void abort(UUID player) {
            Grab grab = grabMap.get(player);
            if (grab != null) {
                grab.abort();
            }
        }

        public void stop() {
            Set.copyOf(grabMap.values()).forEach(Grab::abort);
        }

        private synchronized void tryGrab(Ghost ghost, Player ghostPlayer, GhostHunter victim, Player victimPlayer) {
            if (stopping || !isAvailable(ghost) || !isAvailable(victim)) return;

            Grab grab = new Grab(ghost, victim, ghostPlayer.getEyeLocation(), victimPlayer.getLocation());
            grabMap.put(ghost.getUuid(), grab);
            grabMap.put(victim.getUuid(), grab);
            grab.begin(ghostPlayer);
        }

        private boolean isAvailable(Manor2Role role) {
            return participants.contains(role.getEventPlayer())
                    && !grabMap.containsKey(role.getUuid())
                    && cooldown.getOrDefault(role.getUuid(), 0L) <= currentTick;
        }

        private void cooldown(Manor2Role role, int ticks) {
            cooldown.merge(role.getUuid(), currentTick + ticks, Math::max);
        }

        private final class Grab {

            private final Ghost ghost;
            private final GhostHunter victim;
            private final Location ghostEyes;
            private final Location victimLocation;
            private final AtomicInteger stagger = new AtomicInteger();
            private final AtomicBoolean ended = new AtomicBoolean();
            private int ticksLeft = GRAB_TICKS; // not making this volatile because only touched by `tick()`

            private Grab(Ghost ghost, GhostHunter victim, Location ghostEyes, Location victimLocation) {
                this.ghost = ghost;
                this.victim = victim;
                this.ghostEyes = ghostEyes;
                this.victimLocation = victimLocation;
            }

            private void begin(Player ghostPlayer) {
                NiceRestraint.apply(ghostPlayer);
                ghost.visibility.exposeFor(2);
                victim.operatePlayer(victimPlayer -> {
                    if (ended.get()) return;
                    NiceRestraint.apply(victimPlayer);
                    playEffects(victimPlayer, ghostPlayer);

                    victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1.5f, 1f);
                });
            }

            @SuppressWarnings("UnstableApiUsage")
            private void tick() {
                if (ended.get()) return;
                if (ghost.getEventPlayer().getPlayer() == null || victim.getEventPlayer().getPlayer() == null
                        || !participants.contains(ghost.getEventPlayer()) || !participants.contains(victim.getEventPlayer())) {
                    abort();
                    return;
                }
                if (stagger.get() >= STAGGER_TILL_BREAK) {
                    breakFree();
                    return;
                }
                if (--ticksLeft <= 0) {
                    kill();
                    return;
                }

                ghost.visibility.exposeFor(2);

                boolean heartbeat = ticksLeft % HEARTBEAT_INTERVAL_TICKS == 0;
                victim.operatePlayer(player -> {
                    if (ended.get()) return;
                    player.lookAt(ghostEyes, LookAnchor.EYES);
                    player.setFreezeTicks(player.getMaxFreezeTicks() - 1);
                    if (heartbeat) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.5f, 1f);
                    }
                });
            }

            private void light(LightHit hit, UUID shooter) {
                // ignoring the victim's light
                if (shooter.equals(victim.getUuid()) || hit.centeredness() < MIN_STAGGER_CENTEREDNESS) {
                    return;
                }
                stagger.addAndGet(Math.max(0, hit.lightLevel()));
            }

            private void kill() {
                if (!finish()) {
                    return;
                }
                cooldown(ghost, KILLED_COOLDOWN_TICKS);
                victim.operatePlayer(player -> {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.5f, 1f);
                    player.sendHurtAnimation(0);
                });
                victim.die();
            }

            private void breakFree() {
                if (!finish()) {
                    return;
                }
                cooldown(ghost, BROKEN_COOLDOWN_TICKS);
                cooldown(victim, VICTIM_GRACE_TICKS);
                ghost.visibility.exposeFor(BROKEN_EXPOSE_TICKS);
                ghost.operatePlayer(player -> {
                    Vector away = player.getLocation().toVector().subtract(victimLocation.toVector()).setY(0);
                    if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 0);
                    player.setVelocity(away.normalize().multiply(BROKEN_KNOCKBACK).setY(0.4));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.5f, 0.6f);
                });
            }

            private void backout() {
                if (!finish()) {
                    return;
                }
                cooldown(ghost, LET_GO_COOLDOWN_TICKS);
                cooldown(victim, VICTIM_GRACE_TICKS);
                ghost.visibility.exposeFor(LET_GO_EXPOSE_TICKS);
            }

            private void abort() {
                finish();
            }

            private boolean finish() {
                if (!ended.compareAndSet(false, true)) return false;
                grabMap.remove(ghost.getUuid(), this);
                grabMap.remove(victim.getUuid(), this);
                ghost.operatePlayer(NiceRestraint::release);
                victim.operatePlayer(NiceRestraint::release);
                return true;
            }

            private void playEffects(Player victimPlayer, Player ghostPlayer) { // TODO: Looking for a better way to do this?
                ItemStack face = ItemStack.of(Material.PLAYER_HEAD);
                face.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(ghostPlayer.getPlayerProfile()));
                face.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection(List.of()));
                face.editPersistentDataContainer(pdc -> pdc.set(TAGGED, PersistentDataType.STRING, GHOST_FACE_TAG)); // retire() cleans it up if all else fails

                ItemStack offhand = victimPlayer.getInventory().getItemInOffHand();
                victimPlayer.getInventory().setItemInOffHand(face);
                victimPlayer.updateInventory(); // TODO
                Executors.delayedSync(victimPlayer, 1, () -> {
                    victimPlayer.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
                    Executors.delayedSync(victimPlayer, 1, () -> {
                        ItemStack held = victimPlayer.getInventory().getItemInOffHand();
                        String tag = held.getPersistentDataContainer().get(TAGGED, PersistentDataType.STRING);
                        if (GHOST_FACE_TAG.equals(tag)) {
                            victimPlayer.getInventory().setItemInOffHand(offhand);
                        }
                    });
                });
            }
        }

        /**
         * A restraint that is 'nice' to clients compared to other methods such as cancelling
         * the player move event.
         */
        private static final class NiceRestraint {

            private static final NamespacedKey KEY = new NamespacedKey(EventMain.getInstance(), "manor2/restraint");
            private static final AttributeModifier STOPPED = new AttributeModifier(KEY, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1); // x0, whatever else applies
            private static final AttributeModifier UNMOVABLE = new AttributeModifier(KEY, 1, AttributeModifier.Operation.ADD_NUMBER);
            private static final Map<Attribute, AttributeModifier> MODIFIERS = Map.of(
                    Attribute.MOVEMENT_SPEED, STOPPED,
                    Attribute.SNEAKING_SPEED, STOPPED,
                    Attribute.JUMP_STRENGTH, STOPPED,
                    Attribute.KNOCKBACK_RESISTANCE, UNMOVABLE,
                    Attribute.EXPLOSION_KNOCKBACK_RESISTANCE, UNMOVABLE
            );

            private static void apply(Player player) {
                MODIFIERS.forEach((attribute, modifier) -> {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance == null || instance.getModifier(KEY) != null) return;
                    instance.addTransientModifier(modifier);
                });
                player.setSprinting(false);
                player.setVelocity(new Vector());
            }

            private static void release(Player player) {
                for (Attribute attribute : MODIFIERS.keySet()) {
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance != null) {
                        instance.removeModifier(KEY);
                    }
                }
            }
        }
    }
}
