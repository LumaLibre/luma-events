package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.Manor2Definition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import dev.lumas.events.utility.latency.LatencyTracker;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Manor2 extends InventoryUnifiedMinigame {
    private static final String[] SPLASH = {
            "Are you afraid of ghosts?",
            "Boooooooooooo!!!!",
            "Ghost Busters!",
            "Who you gonna call?",
            "House of Dracula!",
            "Trick or treat?",
            "Here's Johny!!!",
            "Put these foolish ambitions to rest",
            "Go eat candy instead :(",
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
        Bukkit.getRegionScheduler().runAtFixedRate(
                EventMain.getInstance(),
                huntersStartLocations.getFirst(),
                scheduledTask -> {
                    if (stopping) {
                        scheduledTask.cancel();
                        return;
                    }
                    blockCache.showSync();
                }, 1L, 1L);

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
        return super.removeParticipant(player, doTeleport);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.handleStop();
        }
        tickRecorders();
        for (Manor2Role role : roleMap) {
            role.tick();
            role.operatePlayer(player -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 0, false, false, false));
            });
        }
    }

    @EventHandler
    public void onTridentThrow(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof Trident)) return;
        if (!Flashlight.isFlashlight(event.getItemStack())) return;
        event.setCancelled(true);
    }

    @Override
    protected void handleStop() {
        if (timer != null) timer.stop(false);

        Bukkit.getRegionScheduler().run(
                EventMain.getInstance(),
                huntersStartLocations.getFirst(),
                ignore -> blockCache.shutdown()
        );
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(role.retire());
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .join();

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
                completed.complete(null);
            });
            return completed;
        }

        public abstract void tick();

        public abstract void die();

        public abstract CompletableFuture<Void> initiatePlay();
    }

    private class Ghost extends Manor2Role {

        private int revealedTime = 0;
        private volatile boolean revealed = false;

        protected Ghost(EventPlayer eventPlayer) {
            super(eventPlayer);
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
        }

        public void reveal() {
            eventPlayer.operatePlayer(player -> player.setInvisible(false));
            revealed = true;
        }
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
        @org.jetbrains.annotations.Nullable
        private Snapshot at(long when) {
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

        private int ghostConsumedTime = 0;

        private Flashlight flashlight;

        protected GhostHunter(EventPlayer eventPlayer) {
            super(eventPlayer);
            this.flashlight = new Flashlight(eventPlayer.getPlayer());
        }

        @Override
        public void tick() {
            eventPlayer.operatePlayer(player -> {
                flashlight.tick();

            });
        }

        @Override
        public void die() {

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
        private static final float FLICKER_START = 25f;
        private static final float HEAVY_FLICKER_START = 10f;
        private static final float MAX_BATTERY_CHARGE = 100f;
        private static final float BATTERY_DRAIN_PER_TICK = 100f / (20f * 40f);
        private static final float BATTERY_SELF_CHARGE_PER_TICK = 100f / (20f * 20f);
        private static final int LIGHT_BLOCK_LIFETIME = 4;
        private static final int FLASHLIGHT_DISTANCE = 8;

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

        public void tick() {
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

            int illuminance = getFlickeringIlluminance();
            if (illuminance <= 0) {
                return;
            }
            triggerLightCheck(player, FLASHLIGHT_DISTANCE, illuminance, targetsOf(rewindOf(player), Ghost.class::isInstance));
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

        private int getFlickeringIlluminance() {
            int base = getBaseIlluminance();
            if (base <= 0) return 0;

            float chargePercent = batteryCharge / MAX_BATTERY_CHARGE * 100f;
            if (chargePercent > FLICKER_START) {
                flickerTicksRemaining = 0;
                flickerOff = false;
                return base;
            }

            if (flickerTicksRemaining > 0) {
                flickerTicksRemaining--;
                if (flickerOff) return 0;
                return Math.max(base - 3, MIN_ILLUMINANCE);
            }

            double severity = 1.0 - chargePercent / FLICKER_START;
            double startChance = 0.01 + severity * 0.1;

            if (Math.random() < startChance) {
                flickerTicksRemaining = 1 + (int) Math.round(severity * 5);

                if (chargePercent <= HEAVY_FLICKER_START) {
                    flickerOff = Math.random() < 0.6;
                } else {
                    flickerOff = Math.random() < 0.2;
                }

                if (flickerOff) return 0;
                return Math.max(base - 2, MIN_ILLUMINANCE);
            }

            return base;
        }

        private void illuminate(int illuminance) {
            Location location = player.getLocation();
            Location head = location.toCenterLocation().add(0, 1, 0);
            Set<Location> alreadyPlaced = new HashSet<>();

            int firstHit = 0;

            for (int i = 0; i < FLASHLIGHT_DISTANCE * 2; i++) {
                Location posToCheck = head.clone().add(location.getDirection().clone().multiply((double) i / 2));
                if (blocksBeam(posToCheck.getBlock())) break;
                firstHit = i;
            }

            int maxSteps = FLASHLIGHT_DISTANCE * 2;
            int offset = maxSteps - 1 - firstHit;

            for (int i = 0; i <= firstHit; i++) {
                int lightLevel = Math.floorDiv((i + 1 + offset) * illuminance, maxSteps);
                if (lightLevel <= 0) continue;

                Location lightLoc = head.clone().add(location.getDirection().clone().multiply((double) i / 2)).toBlockLocation();
                Block block = lightLoc.getBlock();

                if (block.getType().isAir()) {
                    if (alreadyPlaced.contains(lightLoc)) continue;
                    alreadyPlaced.add(lightLoc);
                    blockCache.setBlock(lightLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
                } else if (!blocksBeam(block) && block.getRelative(BlockFace.UP).getType().isAir()) {
                    Location newLoc = lightLoc.clone().add(0, 1, 0);
                    if (!alreadyPlaced.add(newLoc)) continue;
                    blockCache.setBlock(newLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
                }
            }
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

            List<Player> hitPlayers = new ArrayList<>();
            double tanHalfAngle = maxLight / lightDistance;
            double cosHalfAngle = lightDistance / Math.sqrt(maxLight * maxLight + lightDistance * lightDistance);
            Vector apex = muzzle.toVector();

            for (Target target : targets) {
                BoundingBox hitbox = target.hitbox();
                double distance = coneHitDistance(apex, direction, range, tanHalfAngle, cosHalfAngle, hitbox);
                if (distance < 0) {
                    continue;
                }

                // Occlusion has to be checked towards the target, not down the middle of the cone / Mitality
                Vector toTarget = hitbox.getCenter().subtract(apex);
                double length = toTarget.length();
                if (length > 1.0E-4 && !hasClearShot(muzzle, toTarget.multiply(1.0 / length), length)) {
                    continue;
                }
                hitPlayers.add((Player) target.entity);
            }
            if (!hitPlayers.isEmpty()) {
                shooter.playSound(shooter, Sound.BLOCK_BELL_USE, 1f, 1f); //TODO add actual damage
            }
        }

        private static double coneHitDistance(Vector apex, Vector axis, double range, double tanHalfAngle, double cosHalfAngle, BoundingBox hitbox) {
            double height = hitbox.getHeight();
            double radius = Math.max(Math.max(hitbox.getWidthX(), hitbox.getWidthZ()) / 2.0, height / 6.0);

            // Nothing beyond this can reach the cone
            double maxReach = range + (height / 2.0) + radius;
            if (apex.distanceSquared(hitbox.getCenter()) > maxReach * maxReach) return -1;

            double best = -1;
            for (double fraction : BODY_SAMPLE_FRACTIONS) {
                double dx = hitbox.getCenterX() - apex.getX();
                double dy = (hitbox.getMinY() + (height * fraction)) - apex.getY();
                double dz = hitbox.getCenterZ() - apex.getZ();

                double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
                if (distance <= radius) return 0; // muzzle is inside target

                double along = (dx * axis.getX()) + (dy * axis.getY()) + (dz * axis.getZ());
                if (along < 0 || along - radius > range) continue;

                double offX = dx - (axis.getX() * along);
                double offY = dy - (axis.getY() * along);
                double offZ = dz - (axis.getZ() * along);
                double fromAxis = Math.sqrt((offX * offX) + (offY * offY) + (offZ * offZ));

                // The cone widens with distance, and a sphere of <radius> widens what counts as touching it
                if (fromAxis > (along * tanHalfAngle) + (radius / cosHalfAngle)) continue;

                double toSurface = Math.max(0, distance - radius);
                if (toSurface <= range && (best < 0 || toSurface < best)) best = toSurface;
            }
            return best;
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
}
