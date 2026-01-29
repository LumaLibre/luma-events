package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MineBattleDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRole;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRoleMap;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.WorldEditStructure;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.lumas.lumacore.utility.Logging;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class MineBattle extends InventoryUnifiedMinigame {

    private final long timeLimitMillis;
    private final long maxDistanceLOSsquared;
    private final boolean doPeriodicReveal;
    private final boolean useWorldBorder;
    private final Location lobbyLocation;
    private final Location arenaOrigin;
    private final int arenaHeight;
    private final int minRadius;
    private final int maxRadius;
    private final String innerPattern;
    private final String shellPattern;
    private final String outerPattern;
    private final double minPocketSpacing;
    private final int wallPadding;

    private volatile boolean arenaReady = false;
    private CountdownBossBar gameTimerBossBar;
    private ArenaRegions arenaRegions;
    private List<Location> pocketCenters = List.of();
    private final Scoreboard<EventPlayer> scoreboard;

    private final MinigameRoleMap<AbstractMineBattlePlayer> roleMap =
            new MinigameRoleMap<>(AbstractMineBattlePlayer::cleanup);
    private final Map<UUID, Set<UUID>> hiddenByViewer = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> forceVisibleUntil = new HashMap<>();

    private final File schematicsFolder = EventMain.getInstance().getDataPath().resolve("schematics/minebattle").toFile();
    private final Map<UUID, Location> assignedSpawn = new HashMap<>();
    private final List<Location> structureLocations = new ArrayList<>();
    private WorldBorderSnapshot savedBorder = null;
    private volatile long revealAllUntilMillis = 0L;
    private long lastRevealAtMillis = 0L;
    private int periodicRevealStep = 0;

    // Reveal for 3s at half-time, 5s at 3/4, 10s at 7/8 and permanently at 15/16
    private static final PeriodicRevealStep[] REVEAL_SCHEDULE = {
        new PeriodicRevealStep(1, 2,  3_000),
        new PeriodicRevealStep(2, 3,  0),
        new PeriodicRevealStep(3, 4,  5_000),
        new PeriodicRevealStep(4, 5,  0),
        new PeriodicRevealStep(5, 6,  0),
        new PeriodicRevealStep(6, 7,  0),
        new PeriodicRevealStep(7, 8, 10_000),
        new PeriodicRevealStep(15,16, -1)
    };

    private static final NamespacedKey COPPER_MAX_HEALTH_KEY =
            new NamespacedKey(EventMain.getInstance(), "minebattle_copper_max_health");

    private static final PotionEffect START_SATURATION =
            new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0, false, false);
    private static final PotionEffect START_DARKNESS =
            new PotionEffect(PotionEffectType.DARKNESS, PotionEffect.INFINITE_DURATION, 0, false, false);

    private static final ItemStack START_SWORD_TEMPLATE;
    private static final ItemStack START_PICKAXE_TEMPLATE;

    static {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        sword.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        START_SWORD_TEMPLATE = sword;

        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        pickaxe.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        pickaxe.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 3);
        START_PICKAXE_TEMPLATE = pickaxe;
    }

    public MineBattle(MineBattleDefinition def) {
        super("MineBattle", "Break ores, gear up, and fight!", def.getTimeLimitSeconds() * 1000L, def.getHeartbeatTicks(), true, true, false, false);
        this.timeLimitMillis = Util.secsToMillis(def.getTimeLimitSeconds());
        this.maxDistanceLOSsquared = def.getMaxDistanceLOS() * def.getMaxDistanceLOS();
        this.doPeriodicReveal = def.isDoPeriodicReveal();
        this.useWorldBorder = def.isUseWorldBorder();
        this.lobbyLocation = def.getLobbyLocation();
        this.arenaOrigin = def.getArenaOrigin();
        this.arenaHeight = def.getArenaHeight();
        this.minRadius = def.getMinRadius();
        this.maxRadius = def.getMaxRadius();
        this.innerPattern = buildWeightedPattern(def.getInnerPattern());
        this.shellPattern = buildWeightedPattern(def.getShellPattern());
        this.outerPattern = buildWeightedPattern(def.getOuterPattern());
        this.minPocketSpacing = def.getMinPocketSpacing();
        this.wallPadding = def.getWallPadding();
        this.scoreboard = new Scoreboard<>();
        this.boundingBox = computeBoundingBox(arenaOrigin, maxRadius, arenaHeight);
    }

    private WorldTiedBoundingBox computeBoundingBox(Location origin, int maxRadius, int height) {
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        int r = maxRadius + 2; // include outer walls
        Location outerMin = new Location(origin.getWorld(), ox - r, oy - 2, oz - r);
        Location outerMax = new Location(origin.getWorld(), ox + r, oy + height - 1 + 2, oz + r);
        return WorldTiedBoundingBox.of(outerMin, outerMax);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    public static String buildWeightedPattern(Map<String, Double> weights) {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String key = entry.getKey();
            double weight = entry.getValue();
            if (weight <= 0.0) continue;
            Material material;
            try { material = Material.valueOf(key.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { continue; }
            joiner.add(weight + "%" + material.getKey().getKey());
        }
        return joiner.toString();
    }

    @Override
    protected void handleStart() {
        this.arenaReady = false;
        this.roleMap.clear();
        this.hiddenByViewer.clear();
        this.forceVisibleUntil.clear();
        this.revealAllUntilMillis = 0L;
        this.lastRevealAtMillis = 0L;
        this.periodicRevealStep = 0;
        for (EventPlayer ep : this.participants) {
            this.roleMap.put(new ActiveMineBattlePlayer(ep, this));
        }
        int playerCount = Math.max(1, this.participants.size());
        int radius = computeRadiusForPlayers(playerCount);
        Logging.log("Generating arena for " + playerCount + " players (r=" + radius + ")...");
        rollStructuresAndAssignSpawns(radius);
        this.arenaRegions = ArenaRegions.of(this.arenaOrigin, radius, this.arenaHeight);
        Executors.runAsync(() -> {
            try {
                buildArenaFAWE(this.arenaRegions);
                carvePocketsFAWE(this.arenaRegions.world(), this.pocketCenters);
                for (Location loc : this.structureLocations) {
                    File file = pickRandomSchematicFile();
                    if (file == null) {
                        Logging.errorLog("No schematics found in " + schematicsFolder.getPath());
                        break;
                    }
                    WorldEditStructure structure =
                            new WorldEditStructure(loc, "minebattle/" + file.getName());
                    structure.paste();
                }
            } catch (Throwable t) {
                Logging.errorLog(t.getMessage(), t);
            }

            Executors.runSync(() -> {
                if (this.isCancelled()) return;
                CountdownBossBar.builder()
                        .title("<yellow>Generating Arena...") // <gray>%ss</gray>
                        .color(BossBar.Color.YELLOW)
                        .seconds(5)
                        .audience(this.audience)
                        .countUp(true)
                        .callback(() -> Executors.runSync(() -> {
                            if (this.isCancelled()) return;
                            this.arenaReady = true;

                            if (useWorldBorder) setupWorldBorderSafe(radius);

                            teleportPlayersToAssignedSpawnsThen(() -> {
                                if (useWorldBorder) armAndShrinkWorldBorder();
                                startGameTimerBossBar();
                                this.sendAudienceMessage("<green>MineBattle started!</green>");
                            });
                        }))
                        .build()
                        .start();
            });
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.arenaReady) return;
        Executors.runSync(() -> {
            updateLineOfSightVisibility();
            tickPeriodicReveal(timeLeft);
        });
        if (aliveCount() <= 1) this.stop();
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.lobbyLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        AbstractMineBattlePlayer role = this.roleMap.remove(participant.getUuid());
        if (role != null) role.cleanup();
        UUID uuid = participant.getUuid();
        Executors.runSync(() -> {
            Player leaving = participant.getPlayer();
            if (leaving != null) {
                for (EventPlayer ep : this.participants) {
                    Player viewer = ep.getPlayer();
                    if (viewer != null && !viewer.getUniqueId().equals(uuid)) {
                        viewer.showPlayer(EventMain.getInstance(), leaving);
                    }
                }
            }
            hiddenByViewer.remove(uuid);
            for (Set<UUID> set : hiddenByViewer.values()) set.remove(uuid);
            forceVisibleUntil.remove(uuid);
            for (Map<UUID, Long> m : forceVisibleUntil.values()) m.remove(uuid);
        });
        return super.removeParticipant(participant);
    }

    @Override
    protected void handleStop() {
        unsafe(() -> {
            if (this.gameTimerBossBar != null && !this.gameTimerBossBar.isCancelled()) {
                this.gameTimerBossBar.stop(false);
            }
        });

        Executors.runSync(() -> {
            for (EventPlayer ep : this.participants) {
                ep.teleportAsync(this.lobbyLocation);
                Player p = ep.getPlayer();
                if (p == null) continue;
                ep.operatePlayer(MineBattle::bukkitCleanup);
                for (EventPlayer other : this.participants) {
                    Player o = other.getPlayer();
                    if (o != null) {
                        o.showPlayer(EventMain.getInstance(), p);
                    }
                }
            }
        });
        forceVisibleUntil.clear();
        hiddenByViewer.clear();

        ArenaRegions regions = this.arenaRegions;
        if (regions != null) {
            Executors.runAsync(() -> {
                try {
                    removeArenaFAWE(regions);
                } catch (Throwable t) {
                    Logging.errorLog(t.getMessage(), t);
                }
            });
        }
        Executors.runSync(this::restoreWorldBorder);

        this.scoreboard.handleGameEnd(this.audience, () -> CountdownBossBar.builder()
                .audience(this.audience)
                .color(BossBar.Color.BLUE)
                .title("<aqua><b>Game Over")
                .seconds(10)
                .callback(() -> this.participants.forEach(eventPlayer -> {
                    eventPlayer.teleportAsync(this.getGameDropOffLocation());
                    eventPlayer.sendMessage("This minigame has concluded.");
                }))
                .build()
                .start());
    }

    private void startGameTimerBossBar() {
        this.gameTimerBossBar = CountdownBossBar.builder()
                .title("<green>Time Left: %ss")
                .color(BossBar.Color.GREEN)
                .miliseconds(this.getDuration() - 5_000L)
                .audience(this.audience)
                .callback(() -> {
                    this.sendAudienceMessage("<yellow>Time is up!</yellow>");
                    this.stop();
                })
                .build()
                .start();
    }

    public static abstract class AbstractMineBattlePlayer extends MinigameRole {
        protected final MineBattle context;

        protected AbstractMineBattlePlayer(EventPlayer eventPlayer, MineBattle context) {
            super(eventPlayer);
            this.context = context;
        }

        public abstract void cleanup();
    }

    public static final class ActiveMineBattlePlayer extends AbstractMineBattlePlayer {
        private ActiveMineBattlePlayer(EventPlayer eventPlayer, MineBattle context) {
            super(eventPlayer, context);
        }

        @Override
        public void cleanup() {
            eventPlayer.operatePlayer(MineBattle::bukkitCleanup);
        }

        public void eliminate(@Nullable Player killer) {
            this.context.roleMap.swapRole(this, () -> new MineBattleSpectator(this.eventPlayer, this.context));

            this.eventPlayer.operatePlayer(p -> {
                this.context.dropInventoryAndClear(p);
                bukkitCleanup(p);
                this.context.hideFromOtherPlayers(p);
                p.playSound(p, Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
            });

            this.eventPlayer.sendMessage("<red>You have been eliminated!</red>");

            UUID deadId = this.eventPlayer.getUuid();
            this.context.hiddenByViewer.remove(deadId);
            for (Set<UUID> set : this.context.hiddenByViewer.values()) set.remove(deadId);
            this.context.forceVisibleUntil.remove(deadId);
            for (Map<UUID, Long> m : this.context.forceVisibleUntil.values()) m.remove(deadId);

            if (killer != null) {
                this.context.awardKill(killer.getUniqueId(), deadId);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2, false, false, true));
            }
        }
    }

    private static final class MineBattleSpectator extends AbstractMineBattlePlayer {
        private MineBattleSpectator(EventPlayer eventPlayer, MineBattle context) {
            super(eventPlayer, context);
            this.hide();
        }

        @Override
        public void cleanup() {
            this.show();
            eventPlayer.operatePlayer(MineBattle::bukkitCleanup);
        }

        private void hide() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;
                for (EventPlayer other : this.context.getParticipants()) {
                    if (self.getUniqueId().equals(other.getUuid())) continue;
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;
                    bukkitOther.hidePlayer(EventMain.getInstance(), self);
                }
            });
        }

        private void show() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;
                for (EventPlayer other : this.context.getParticipants()) {
                    if (self.getUniqueId().equals(other.getUuid())) continue;
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;
                    bukkitOther.showPlayer(EventMain.getInstance(), self);
                }
            });
        }
    }

    private static void bukkitCleanup(Player player) {
        player.clearActivePotionEffects();
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) { // Set max health back to 20HP (= 10 hearts)
            AttributeModifier existing = attr.getModifier(COPPER_MAX_HEALTH_KEY);
            if (existing != null) attr.removeModifier(existing);
        }
        player.setHealth(20.0);
        player.setFireTicks(0);
        player.setFoodLevel(20);
    }

    public int computeRadiusForPlayers(int playerCount) {
        playerCount = Math.max(1, playerCount);
        int edgeLoss = 2 * (wallPadding + 1/*to pocket center*/);
        double areaNeededPerPocket = minPocketSpacing * minPocketSpacing;
        double requiredArea = playerCount * areaNeededPerPocket * 1.5/*safety margin*/;
        double usableSideNeeded = Math.sqrt(requiredArea);
        double rNeeded = (usableSideNeeded + edgeLoss - 1.0) / 2.0;
        int radius = (int) Math.ceil(rNeeded);
        radius = Math.max(minRadius, Math.min(maxRadius, radius));

        // Ensure usableSide isn't degenerate if minRadius is tiny
        int usableSide = (2 * radius + 1) - edgeLoss;
        if (usableSide < 3) radius = Math.min(maxRadius, radius + 3);

        return radius;
    }

    private void buildArenaFAWE(ArenaRegions regions) {
        Pattern inner = parsePattern(regions.world(), innerPattern);
        Pattern shell = parsePattern(regions.world(), shellPattern);
        Pattern outer = parsePattern(regions.world(), outerPattern);

        try (EditSession session = WorldEdit.getInstance().newEditSession(regions.world())) {
            session.setBlocks((Region) regions.outer(), outer);
            session.setBlocks((Region) regions.shell(), shell);
            session.setBlocks((Region) regions.inner(), inner);
            session.flushQueue();
        }
    }
    private void carvePocketsFAWE(World weWorld, List<Location> centers) {
        Pattern air = parsePattern(weWorld, "air");

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (Location c : centers) {
                CuboidRegion pocket = pocketRegion(weWorld, c);
                session.setBlocks((Region) pocket, air);
                session.flushQueue();
            }
        }
    }
    private void removeArenaFAWE(ArenaRegions regions) {
        Pattern air = parsePattern(regions.world(), "air");
        try (EditSession session = WorldEdit.getInstance().newEditSession(regions.world())) {
            session.setBlocks((Region) regions.outer(), air);
            session.flushQueue();
        }
    }

    private static Pattern parsePattern(World weWorld, String input) throws InputParseException {
        ParserContext ctx = new ParserContext();
        ctx.setWorld(weWorld);
        ctx.setRestricted(false);

        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, ctx);
    }

    private List<Location> generatePocketCenters(Location origin, int radius, int height, int count) {
        org.bukkit.World bw = origin.getWorld();
        if (bw == null) return List.of();
        int cx = origin.getBlockX();
        int cy = origin.getBlockY();
        int cz = origin.getBlockZ();

        // Ensure the 3x3x3 pocket fits and is away from inner walls
        int xMin = cx - radius + wallPadding + 1/*to 3x3x3*/;
        int xMax = cx + radius - wallPadding - 1/*to 3x3x3*/;
        int zMin = cz - radius + wallPadding + 1/*to 3x3x3*/;
        int zMax = cz + radius - wallPadding - 1/*to 3x3x3*/;

        // Keep pockets within inner vertical range and not too near floor/ceiling
        int yMin = cy + 2 + 1/*to 3x3x3*/;
        int yMax = cy + Math.max(4, height - 3 - 1/*to 3x3x3*/);

        if (xMin > xMax || zMin > zMax || yMin > yMax) { // Arena too small
            return List.of(new Location(bw, cx + 0.5, cy + 2.1, cz + 0.5));
        }

        double minDistSq = minPocketSpacing * minPocketSpacing;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        List<Location> pockets = new ArrayList<>(count);
        int maxAttempts = 8000 + count * 2000;
        int attempts = 0;

        while (pockets.size() < count && attempts++ < maxAttempts) {
            int px = rng.nextInt(xMin, xMax + 1);
            int py = rng.nextInt(yMin, yMax + 1);
            int pz = rng.nextInt(zMin, zMax + 1);

            boolean ok = true;
            for (Location other : pockets) {
                double dx = other.getX() - px;
                double dy = other.getY() - py;
                double dz = other.getZ() - pz;
                if ((dx * dx + dy * dy + dz * dz) < minDistSq) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            pockets.add(new Location(bw, px + 0.5, py + 0.1, pz + 0.5));
        }

        // Fall back to coarse grid if random distribution fails
        if (pockets.size() < count) {
            int step = (int) Math.max(4, Math.floor(minPocketSpacing));
            outer:
            for (int x = xMin; x <= xMax; x += step) {
                for (int z = zMin; z <= zMax; z += step) {
                    int y = (yMin + yMax) / 2;
                    Location candidate = new Location(bw, x + 0.5, y + 0.1, z + 0.5);

                    boolean ok = true;
                    for (Location other : pockets) {
                        double dx = other.getX() - candidate.getX();
                        double dy = other.getY() - candidate.getY();
                        double dz = other.getZ() - candidate.getZ();
                        if ((dx * dx + dy * dy + dz * dz) < minDistSq) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;

                    pockets.add(candidate);
                    if (pockets.size() >= count) break outer;
                }
            }
        }

        // If still short, duplicate last-resort near center-ish but offset
        while (pockets.size() < count) {
            double off = pockets.size() * 2.0;
            pockets.add(new Location(bw, cx + 0.5 + off, cy + 2.1, cz + 0.5));
        }

        return pockets.stream().map(l -> l.add(0.0, -1.0, 0.0)).toList();
    }

    private static CuboidRegion pocketRegion(World weWorld, Location center) {
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        BlockVector3 min = BlockVector3.at(x - 1/*to 3x3x3*/, y - 1/*to 3x3x3*/, z - 1/*to 3x3x3*/);
        BlockVector3 max = BlockVector3.at(x + 1/*to 3x3x3*/, y + 1/*to 3x3x3*/, z + 1/*to 3x3x3*/);
        return new CuboidRegion(weWorld, min, max);
    }

    private void updateLineOfSightVisibility() {
        if (!arenaReady) return;

        List<Player> actives = new ArrayList<>();
        List<Player> spectators = new ArrayList<>();
        for (EventPlayer ep : this.participants) {
            Player p = ep.getPlayer();
            if (p == null) continue;

            if (isActive(p)) actives.add(p);
            else if (isSpectator(p)) spectators.add(p);
        }

        // No one sees spectators
        for (Player viewer : actives) {
            for (Player spec : spectators) {
                setCanSee(viewer, spec, false);
            }
        }

        // Spectators don't see other spectators
        for (Player viewer : spectators) {
            for (Player spec : spectators) {
                if (viewer == spec) continue;
                setCanSee(viewer, spec, false);
            }
        }

        // Active <-> Active (LOS in any direction)
        for (int i = 0; i < actives.size(); i++) {
            Player a = actives.get(i);
            for (int j = i + 1; j < actives.size(); j++) {
                Player b = actives.get(j);

                boolean aForces = forcedVisible(a.getUniqueId(), b.getUniqueId());
                boolean bForces = forcedVisible(b.getUniqueId(), a.getUniqueId());

                if (a.getWorld() != b.getWorld()) {
                    setCanSee(a, b, false);
                    setCanSee(b, a, false);
                    continue;
                }

                double distSq = a.getLocation().distanceSquared(b.getLocation());
                boolean baseVisible = false;
                if (distSq <= maxDistanceLOSsquared) {
                    baseVisible = a.hasLineOfSight(b) || b.hasLineOfSight(a);
                }

                setCanSee(a, b, aForces || baseVisible);
                setCanSee(b, a, bForces || baseVisible);
            }
        }

        // Spectator -> Active (directional LOS)
        for (Player viewer : spectators) {
            UUID viewerId = viewer.getUniqueId();

            for (Player target : actives) {
                boolean force = forcedVisible(viewerId, target.getUniqueId());

                if (viewer.getWorld() != target.getWorld()) {
                    setCanSee(viewer, target, false);
                    continue;
                }

                double distSq = viewer.getLocation().distanceSquared(target.getLocation());
                boolean visible = false;

                if (force) {
                    visible = true;
                } else if (distSq <= maxDistanceLOSsquared) {
                    visible = viewer.hasLineOfSight(target);
                }

                setCanSee(viewer, target, visible);
            }
        }
    }

    private void forceShowFor(UUID viewerId, UUID targetId, long durationMs) {
        long until = System.currentTimeMillis() + durationMs;
        forceVisibleUntil
                .computeIfAbsent(viewerId, k -> new HashMap<>())
                .merge(targetId, until, Math::max);
        Set<UUID> hidden = hiddenByViewer.computeIfAbsent(viewerId, k -> new HashSet<>());
        hidden.remove(targetId);
        Player viewer = Bukkit.getPlayer(viewerId);
        Player target = Bukkit.getPlayer(targetId);
        if (viewer != null && target != null) {
            viewer.showPlayer(EventMain.getInstance(), target);
        }
    }

    private record PeriodicRevealStep(int num, int den, long durationMs) {}
    private void tickPeriodicReveal(long timeLeftMillis) {
        if (!doPeriodicReveal) return;
        if (!arenaReady) return;

        while (periodicRevealStep < REVEAL_SCHEDULE.length) {
            PeriodicRevealStep step = REVEAL_SCHEDULE[periodicRevealStep];
            long threshold = (timeLimitMillis * (step.den - step.num)) / step.den;
            if (timeLeftMillis > threshold) break;
            periodicRevealStep++;

            if (step.durationMs == 0) {
                continue;
            }

            long durationMs;
            if (step.durationMs < 0) {
                this.revealAllUntilMillis = Long.MAX_VALUE; // permanent until stop()
                durationMs = Math.max(0L, timeLeftMillis);
            } else {
                long until = System.currentTimeMillis() + step.durationMs;
                this.revealAllUntilMillis = Math.max(this.revealAllUntilMillis, until);
                durationMs = step.durationMs;
            }

            long now = System.currentTimeMillis();
            if (now - lastRevealAtMillis < 500) return;
            lastRevealAtMillis = now;

            revealAllPlayers(durationMs);
            double durationSecs = durationMs / 1000.0;
            if (step.durationMs < 0) {
                this.sendAudienceMessage("<yellow>All players revealed until the end!</yellow>");
            } else {
                this.sendAudienceMessage("<yellow>All players revealed for " + String.format("%.0f", durationSecs) + " seconds!</yellow>");
            }
        }
    }

    private void revealAllPlayers(long durationMs) {
        int ticks = (int) Math.max(20, durationMs / 50);

        List<Player> actives = new ArrayList<>();
        List<Player> viewers = new ArrayList<>();

        for (EventPlayer ep : this.participants) {
            Player p = ep.getPlayer();
            if (p == null) continue;
            if (isActive(p)) actives.add(p);
            if (isActive(p) || isSpectator(p)) viewers.add(p);
        }

        // Glow all active players
        for (Player p : actives) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, ticks, 0, false, false));
            p.playSound(p, Sound.ENTITY_WITHER_SPAWN, 1, 1);
        }

        // Force visibility (viewers -> actives)
        for (Player viewer : viewers) {
            UUID viewerId = viewer.getUniqueId();
            for (Player target : actives) {
                if (viewer == target) continue;
                forceShowFor(viewerId, target.getUniqueId(), durationMs);
            }
        }
    }

    private void setupWorldBorderSafe(int radius) {
        org.bukkit.World bw = arenaOrigin.getWorld();
        if (bw == null) return;

        WorldBorder border = bw.getWorldBorder();

        savedBorder = new WorldBorderSnapshot(
                border.getCenter(),
                border.getSize(),
                border.getDamageAmount(),
                border.getDamageBuffer(),
                border.getWarningDistance(),
                border.getWarningTime()
        );

        double start = (radius + 1) * 2.0 + 6.0; // diameter

        border.setCenter(arenaOrigin);
        border.setSize(start);

        border.setDamageAmount(0.0); // don't (h)arm yet
        border.setDamageBuffer(1000000.0);

        border.setWarningDistance(5);
        border.setWarningTime(5);
    }

    private void armAndShrinkWorldBorder() {
        org.bukkit.World bw = arenaOrigin.getWorld();
        if (bw == null) return;

        WorldBorder border = bw.getWorldBorder();

        border.setDamageBuffer(0.0);
        border.setDamageAmount(3.5);

        long seconds = Math.max(1, timeLimitMillis / 1000L);
        border.setSize(5.0 /*end diameter*/, seconds);
    }

    private void teleportPlayersToAssignedSpawnsThen(Runnable afterAllTeleports) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (EventPlayer ep : this.participants) {
            ep.operatePlayer(MineBattle::bukkitCleanup);

            Location spawn = assignedSpawn.get(ep.getUuid());
            if (spawn == null) spawn = this.pocketCenters.getFirst();

            CompletableFuture<Boolean> tp = ep.teleportAsync(spawn);
            if (tp == null) {
                Logging.warningLog("[MineBattle] teleportAsync returned null for " + ep.getUuid());
                tp = CompletableFuture.completedFuture(false);
            } else {
                tp = tp.exceptionally(err -> {
                    Logging.errorLog(err.getMessage(), err);
                    return false;
                });
            }

            CompletableFuture<Boolean> finalTp = tp;
            tp.thenAccept(ok -> {
                if (ok) ep.operatePlayer(this::equip);
                else Logging.warningLog("[MineBattle] teleport failed for " + ep.getUuid());
            });

            futures.add(finalTp);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Executors.runSync(() -> Executors.delayedSync(20L, afterAllTeleports)));
    }

    private void equip(Player player) {
        player.getInventory().addItem(START_SWORD_TEMPLATE.clone());
        player.getInventory().addItem(START_PICKAXE_TEMPLATE.clone());
        player.addPotionEffect(START_SATURATION);
        player.addPotionEffect(START_DARKNESS);
    }

    private void restoreWorldBorder() {
        if (!useWorldBorder) return;
        if (savedBorder == null) return;

        org.bukkit.World bw = arenaOrigin.getWorld();
        if (bw == null) return;

        WorldBorder border = bw.getWorldBorder();

        border.setCenter(savedBorder.center());
        border.setSize(savedBorder.size());
        border.setDamageAmount(savedBorder.damageAmount());
        border.setDamageBuffer(savedBorder.damageBuffer());
        border.setWarningDistance(savedBorder.warningDistance());
        border.setWarningTime(savedBorder.warningTime());

        savedBorder = null;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamagedByEntityEvent(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isParticipant(victim) || !isParticipant(damager)) return;
        if (isActive(damager)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isParticipant(player)) return;
        if (isActive(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isParticipant(player)) return;
        if (isActive(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityDropItemEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isParticipant(player)) return;
        if (isActive(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStartBreaking(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!isParticipant(event.getPlayer())) return;
        if (isActive(event.getPlayer())) return;
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        if (isActive(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getEntity())) return;

        event.setCancelled(true);
        event.getDrops().clear();
        event.deathMessage(null);

        Player dead = event.getEntity();
        ActiveMineBattlePlayer role = asActive(dead.getUniqueId());
        if (role == null) return;

        Player killer = null;
        try {
            if (event.getDamageSource().getCausingEntity() instanceof Player p) killer = p;
        } catch (Throwable ignored) {}
        if (killer == null) killer = dead.getKiller();

        Player finalKiller = killer;
        Executors.runSync(() -> role.eliminate(finalKiller));
    }

    private int aliveCount() {
        return this.roleMap.getMatching(ActiveMineBattlePlayer.class).size();
    }

    private @Nullable ActiveMineBattlePlayer asActive(UUID uuid) {
        return this.roleMap.as(uuid, ActiveMineBattlePlayer.class);
    }

    private boolean isActive(Player p) {
        return asActive(p.getUniqueId()) != null;
    }

    private boolean isSpectator(Player p) {
        return this.roleMap.as(p.getUniqueId(), MineBattleSpectator.class) != null;
    }

    private void setCanSee(Player viewer, Player target, boolean shouldSee) {
        UUID viewerId = viewer.getUniqueId();
        UUID targetId = target.getUniqueId();

        Set<UUID> hidden = hiddenByViewer.computeIfAbsent(viewerId, k -> new HashSet<>());

        if (shouldSee) {
            if (hidden.remove(targetId)) viewer.showPlayer(EventMain.getInstance(), target);
        } else {
            if (hidden.add(targetId)) viewer.hidePlayer(EventMain.getInstance(), target);
        }
    }

    private boolean forcedVisible(UUID viewerId, UUID targetId) {
        Map<UUID, Long> map = forceVisibleUntil.get(viewerId);
        if (map == null) return false;
        Long until = map.get(targetId);
        if (until == null) return false;

        long now = System.currentTimeMillis();
        if (until <= now) {
            map.remove(targetId);
            if (map.isEmpty()) forceVisibleUntil.remove(viewerId);
            return false;
        }
        return true;
    }

    private EventPlayer getParticipant(UUID uuid) {
        return this.participants.stream()
                .filter(p -> p.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    private void hideFromOtherPlayers(Player dead) {
        for (EventPlayer ep : this.participants) {
            if (ep.getUuid().equals(dead.getUniqueId())) continue;
            Player other = ep.getPlayer();
            if (other != null) {
                other.hidePlayer(EventMain.getInstance(), dead);
            }
        }
    }

    private void dropInventoryAndClear(Player dead) {
        org.bukkit.World w = dead.getWorld();
        Location loc = dead.getLocation();
        for (ItemStack item : dead.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) continue;
            w.dropItemNaturally(loc, item.clone());
        }
        for (ItemStack item : dead.getInventory().getArmorContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) continue;
            w.dropItemNaturally(loc, item.clone());
        }
        ItemStack off = dead.getInventory().getItemInOffHand();
        if (!off.getType().isAir() && off.getEnchantmentLevel(Enchantment.VANISHING_CURSE) <= 0) {
            w.dropItemNaturally(loc, off.clone());
        }

        dead.getInventory().clear();
    }

    private void awardKill(UUID killer, UUID victim) {
        if (killer.equals(victim)) return;
        EventPlayer killerEp = getParticipant(killer);
        if (killerEp == null) return;
        scoreboard.addScore(killerEp, 1);
        killerEp.sendMessage("<green>+1 kill</green> <gray>(" + scoreboard.getScore(killerEp) + " total)</gray>");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        this.ensureNotIllegal();
        if (event.getEntityType() != EntityType.TNT) return;
        if (!boundingBox.contains(event.getEntity())) return;
        event.setYield(0.0f); // prevent block drops
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        this.ensureNotIllegal();
        if (!boundingBox.contains(event.getLocation())) return;
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExp(BlockExpEvent event) {
        this.ensureNotIllegal();
        if (!boundingBox.contains(event.getBlock().getLocation())) return;
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        if (!isActive(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        if (blockType == Material.TNT) {
            Location location = event.getBlock().getLocation();
            event.setCancelled(true);
            player.getInventory().removeItem(new ItemStack(Material.TNT, 1));
            TNTPrimed tnt = location.getWorld().spawn(location.add(0.5, 0.5, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(40);
            tnt.setSource(player);
            Executors.delayedSync(120L, tnt::remove);
        }

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        if (!isActive(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        Random random = RANDOM;
        event.setDropItems(false);
        event.setExpToDrop(0);

        switch (blockType) {
            case COBBLED_DEEPSLATE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(Material.GRAY_STAINED_GLASS));
            }
            case DEEPSLATE_COAL_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(switch (random.nextInt(4)) {
                    case 0 -> Material.CHAINMAIL_HELMET;
                    case 1 -> Material.CHAINMAIL_CHESTPLATE;
                    case 2 -> Material.CHAINMAIL_LEGGINGS;
                    default -> Material.CHAINMAIL_BOOTS;
                }));
            }
            case DEEPSLATE_IRON_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.IRON_HELMET;
                    case 1 -> Material.IRON_CHESTPLATE;
                    case 2 -> Material.IRON_LEGGINGS;
                    case 3 -> Material.IRON_BOOTS;
                    default -> Material.IRON_SWORD;
                }));
            }
            case DEEPSLATE_REDSTONE_ORE -> {
                player.playSound(player, Sound.ENTITY_CREEPER_DEATH, 1, 1);
                Util.giveItem(player, new ItemStack(Material.TNT));
            }
            case DEEPSLATE_COPPER_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
                handleCopper(player);
            }
            case DEEPSLATE_GOLD_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
                if (isRevealAllActive()) return;
                this.getParticipants().stream()
                        .filter(ep -> !Objects.equals(ep.getUuid(), player.getUniqueId()))
                        .forEach(ep -> ep.operatePlayer(p -> {
                            if (p.getLocation().distanceSquared(player.getLocation()) > 25*25) return;
                            forceShowFor(player.getUniqueId(), p.getUniqueId(), 3_000); // (= 60 ticks)
                            if (p.hasPotionEffect(PotionEffectType.GLOWING)) p.removePotionEffect(PotionEffectType.GLOWING);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                            p.playSound(p, Sound.ENTITY_WITHER_SPAWN, 1, 1);
                        }));
            }
            case DEEPSLATE_LAPIS_ORE -> {
                player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
                ItemStack reward = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta rewardMeta = (EnchantmentStorageMeta) reward.getItemMeta();
                switch (random.nextInt(14)) {
                    case 0  -> rewardMeta.addStoredEnchant(Enchantment.SHARPNESS, 1, true);
                    case 1  -> rewardMeta.addStoredEnchant(Enchantment.SHARPNESS, 2, true);
                    case 2  -> rewardMeta.addStoredEnchant(Enchantment.FEATHER_FALLING, 3, true);
                    case 3  -> rewardMeta.addStoredEnchant(Enchantment.PROTECTION, 1, true);
                    case 4  -> rewardMeta.addStoredEnchant(Enchantment.PROTECTION, 2, true);
                    case 5  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 1, true);
                    case 6  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 2, true);
                    case 7  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 3, true);
                    case 8  -> rewardMeta.addStoredEnchant(Enchantment.KNOCKBACK, 1, true);
                    case 9  -> rewardMeta.addStoredEnchant(Enchantment.KNOCKBACK, 2, true);
                    case 10 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 1, true);
                    case 11 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 2, true);
                    case 12 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 3, true);
                    default -> rewardMeta.addStoredEnchant(Enchantment.VANISHING_CURSE, 1, true);
                }
                reward.setItemMeta(rewardMeta);
                Util.giveItem(player, reward);
            }
            case DEEPSLATE_EMERALD_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1);
                handleEmerald(player);
            }
            case DEEPSLATE_DIAMOND_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.DIAMOND_HELMET;
                    case 1 -> Material.DIAMOND_CHESTPLATE;
                    case 2 -> Material.DIAMOND_LEGGINGS;
                    case 3 -> Material.DIAMOND_BOOTS;
                    default -> Material.DIAMOND_SWORD;
                }));
            }
            case ANCIENT_DEBRIS -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.NETHERITE_HELMET;
                    case 1 -> Material.NETHERITE_CHESTPLATE;
                    case 2 -> Material.NETHERITE_LEGGINGS;
                    case 3 -> Material.NETHERITE_BOOTS;
                    default -> Material.NETHERITE_SWORD;
                }));
            }
            case RAW_GOLD_BLOCK -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                Util.giveItem(player, new ItemStack(Material.GOLDEN_APPLE));
            }
            default -> {}
        }

    }

    private boolean isRevealAllActive() {
        long until = revealAllUntilMillis;
        if (until == 0L) return false;
        if (until == Long.MAX_VALUE) return true;
        long now = System.currentTimeMillis();
        if (now <= until) return true;
        revealAllUntilMillis = 0L;
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSlotChange(InventoryClickEvent event) {
        this.ensureNotIllegal();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current == null) return;
        if (cursor.getType() != Material.ENCHANTED_BOOK) return;
        if (!isSword(current) && !isArmor(current)) return;

        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player player)) return;
        if (!isParticipant(player)) return;

        handleEnchantment(event, player, Enchantment.SHARPNESS, 5);
        handleEnchantment(event, player, Enchantment.KNOCKBACK, 2);
        handleEnchantment(event, player, Enchantment.FEATHER_FALLING, 4);
        handleEnchantment(event, player, Enchantment.PROTECTION, 4);
        handleEnchantment(event, player, Enchantment.BLAST_PROTECTION, 4);
        handleEnchantment(event, player, Enchantment.THORNS, 3);
        handleEnchantment(event, player, Enchantment.VANISHING_CURSE, 1);

        event.setCancelled(true);
    }

    private static void handleCopper(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        NamespacedKey key = COPPER_MAX_HEALTH_KEY;

        AttributeModifier existing = attr.getModifier(key);
        double currentDelta = existing != null ? existing.getAmount() : 0.0;

        double step = ThreadLocalRandom.current().nextBoolean() ? 2.0 : -2.0;
        double newDelta = currentDelta + step;

        if (existing != null) attr.removeModifier(existing);
        attr.addTransientModifier(new AttributeModifier(key, newDelta, AttributeModifier.Operation.ADD_NUMBER));

        double maxNow = attr.getValue();
        if (player.getHealth() > maxNow) {
            player.setHealth(maxNow);
        }
    }

    private static void handleEmerald(Player player) {

        // Define effects and their max amplifier (0-based, so 2 = level 3)
        Map<PotionEffectType, Integer> effectLimits = new HashMap<>();
        effectLimits.put(PotionEffectType.ABSORPTION, 4);
        effectLimits.put(PotionEffectType.HASTE, 2);
        effectLimits.put(PotionEffectType.RESISTANCE, 1);
        effectLimits.put(PotionEffectType.STRENGTH, 0);
        effectLimits.put(PotionEffectType.FIRE_RESISTANCE, 0);
        effectLimits.put(PotionEffectType.INVISIBILITY, 0);
        effectLimits.put(PotionEffectType.JUMP_BOOST, 0);
        effectLimits.put(PotionEffectType.SPEED, 0);

        boolean hasAllEffectsAtMax = effectLimits.entrySet().stream().allMatch(entry -> {
            PotionEffectType type = entry.getKey();
            int maxAmp = entry.getValue();
            return player.getActivePotionEffects().stream()
                    .anyMatch(effect -> effect.getType().equals(type) && effect.getAmplifier() >= maxAmp);
        });

        if (!hasAllEffectsAtMax) {
            Random random = new Random();
            List<PotionEffectType> keys = new ArrayList<>(effectLimits.keySet());

            while (true) {
                PotionEffectType randomEffect = keys.get(random.nextInt(keys.size()));
                int maxAmp = effectLimits.get(randomEffect);

                PotionEffect current = player.getPotionEffect(randomEffect);
                if (current == null) {
                    player.addPotionEffect(new PotionEffect(randomEffect, -1, 0, false, false)); // Level 1
                    break;
                } else if (current.getAmplifier() < maxAmp) {
                    player.addPotionEffect(new PotionEffect(randomEffect, -1, current.getAmplifier() + 1, false, false));
                    break;
                }
            }

            hasAllEffectsAtMax = effectLimits.entrySet().stream().allMatch(entry -> {
                PotionEffectType type = entry.getKey();
                int maxAmp = entry.getValue();
                return player.getActivePotionEffects().stream()
                        .anyMatch(effect -> effect.getType().equals(type) && effect.getAmplifier() >= maxAmp);
            });

            if (hasAllEffectsAtMax) {
                Util.sendMsg(player, "<red><bold>Caution!</bold></red> <red>Your body can't handle so many effects. If you get one more, you'll die</red>");
            }

        } else {
            player.setHealth(0.0);
            Util.sendMsg(player, "<red>Better listen to our warnings in the future");
        }
    }

    private void handleEnchantment(InventoryClickEvent event, Player player, Enchantment enchantment, int maxLevel) {
        EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) event.getCursor().getItemMeta();
        if (enchantmentStorageMeta == null) return;
        if (!enchantmentStorageMeta.hasStoredEnchant(enchantment)) return;
        if (event.getCurrentItem() == null) return;

        // preconditions
        if (List.of(Enchantment.SHARPNESS, Enchantment.KNOCKBACK).contains(enchantment) && !isSword(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to swords!").color(NamedTextColor.RED)));
            return;
        }
        if (Objects.equals(Enchantment.FEATHER_FALLING, enchantment) && !isBoots(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to boots!").color(NamedTextColor.RED)));
            return;
        }
        if (List.of(Enchantment.PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.THORNS).contains(enchantment) && !isArmor(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to armor!").color(NamedTextColor.RED)));
            return;
        }

        // compute target level
        int bookLevel = enchantmentStorageMeta.getStoredEnchantLevel(enchantment);
        int itemLevel = event.getCurrentItem().getEnchantmentLevel(enchantment);
        int resultLevel = Math.max(bookLevel, itemLevel);
        if (bookLevel > 0 && bookLevel == itemLevel) {
            resultLevel++; // equal levels combine to +1 (Luma exclusive as a replacement for crafting them together)
        }
        resultLevel = Math.min(maxLevel, resultLevel);

        // don't enchant if the level wouldn't increase
        if (event.getCurrentItem().getEnchantmentLevel(enchantment) >= resultLevel) {
            player.sendActionBar(Component.text("This item is already enchanted with a higher level!").color(NamedTextColor.RED));
            return;
        }

        // enchant
        event.getCurrentItem().addUnsafeEnchantment(enchantment, resultLevel);
        event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
        player.playSound (player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
    }

    private boolean isSword(ItemStack item) {
        return Tag.ITEMS_SWORDS.isTagged(item.getType());
    }

    private boolean isArmor(ItemStack item) {
        return Tag.ITEMS_ENCHANTABLE_ARMOR.isTagged(item.getType());
    }

    private boolean isBoots(ItemStack item) {
        return Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(item.getType());
    }

    private void rollStructuresAndAssignSpawns(int radius) {
        structureLocations.clear();
        assignedSpawn.clear();

        int players = this.participants.size();
        List<EventPlayer> structurePlayers = new ArrayList<>();
        boolean structuresEnabled = pickRandomSchematicFile() != null;
        for (EventPlayer ep : this.participants) {
            if (RANDOM.nextBoolean() && structuresEnabled) structurePlayers.add(ep);
        }

        int structureCount = structurePlayers.size();
        this.pocketCenters = generatePocketCenters(this.arenaOrigin, radius, this.arenaHeight, players + structureCount);
        int extraIdx = 0;

        Set<UUID> structureSet = new HashSet<>();
        for (EventPlayer ep : structurePlayers) structureSet.add(ep.getUuid());

        for (int i = 0; i < players; i++) {
            EventPlayer ep = this.participants.get(i);
            Location normalPocket = pocketCenters.get(i);
            if (!structureSet.contains(ep.getUuid())) {
                assignedSpawn.put(ep.getUuid(), normalPocket);
            } else {
                structureLocations.add(normalPocket);
                Location extraPocket = pocketCenters.get(players + extraIdx);
                extraIdx++;
                assignedSpawn.put(ep.getUuid(), extraPocket);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private @Nullable File pickRandomSchematicFile() {
        if (!schematicsFolder.exists()) schematicsFolder.mkdirs();
        File[] files = schematicsFolder.listFiles(f ->
                f.isFile() && (f.getName().endsWith(".schem") || f.getName().endsWith(".schematic"))
        );
        if (files == null || files.length == 0) return null;
        return files[new Random().nextInt(files.length)];
    }

    private record ArenaRegions(World world, CuboidRegion inner, CuboidRegion shell, CuboidRegion outer) {
        static ArenaRegions of(Location origin, int radius, int height) {
            org.bukkit.World bw = origin.getWorld();
            Preconditions.checkNotNull(bw, "Arena origin world is null");
            World weWorld = BukkitAdapter.adapt(bw);
            int cx = origin.getBlockX();
            int cy = origin.getBlockY();
            int cz = origin.getBlockZ();
            BlockVector3 innerMin = BlockVector3.at(cx - radius, cy, cz - radius);
            BlockVector3 innerMax = BlockVector3.at(cx + radius, cy + height - 1, cz + radius);
            BlockVector3 shellMin = BlockVector3.at(cx - (radius + 1), cy - 1, cz - (radius + 1));
            BlockVector3 shellMax = BlockVector3.at(cx + (radius + 1), cy + height, cz + (radius + 1));
            BlockVector3 outerMin = BlockVector3.at(cx - (radius + 2), cy - 2, cz - (radius + 2));
            BlockVector3 outerMax = BlockVector3.at(cx + (radius + 2), cy + height + 1, cz + (radius + 2));
            return new ArenaRegions(weWorld,
                new CuboidRegion(weWorld, innerMin, innerMax),
                new CuboidRegion(weWorld, shellMin, shellMax),
                new CuboidRegion(weWorld, outerMin, outerMax)
            );
        }
    }

    private record WorldBorderSnapshot(
            Location center,
            double size,
            double damageAmount,
            double damageBuffer,
            int warningDistance,
            int warningTime
    ) {}
}
