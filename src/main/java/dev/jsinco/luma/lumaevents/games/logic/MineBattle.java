package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MineBattleDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Logger;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MineBattle extends InventoryUnifiedMinigame {

    private static final long HEARTBEAT = 10; // tick every 0.5 seconds

    private final long maxDurationMillis;
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
    private Scoreboard<EventPlayer> scoreboard;
    private final Set<UUID> eliminated = new HashSet<>();

    public MineBattle(MineBattleDefinition def) {
        super("MineBattle", "Break ores, gear up, and fight!", def.getMaxDurationMillis(), HEARTBEAT, true, true, false);
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
        this.maxDurationMillis = def.getMaxDurationMillis();
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
        int playerCount = Math.max(1, this.participants.size());
        this.sendAudienceMessage("<gray>Generating arena for " + playerCount + " players...</gray>");
        int radius = computeRadiusForPlayers(playerCount);
        this.pocketCenters = generatePocketCenters(this.arenaOrigin, radius, this.arenaHeight, playerCount);
        this.arenaRegions = ArenaRegions.of(this.arenaOrigin, radius, this.arenaHeight);
        Executors.runAsync(() -> {
            try {
                buildArenaFAWE(this.arenaRegions);
                carvePocketsFAWE(this.arenaRegions.world(), this.pocketCenters);
            } catch (Throwable t) {
                Logger.logErr(t);
            }
            Executors.runSync(() -> {
                if (this.isCancelled()) return;
                this.arenaReady = true;
                teleportPlayersToPockets();
                Logger.log("Duration remaining when arena ready: " + this.getDuration());
                startGameTimerBossBar();
                this.sendAudienceMessage("<green>MineBattle started!</green>");
            });
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.arenaReady) return;
        if (aliveCount() <= 1) this.stop();
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.lobbyLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
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
                cleanPlayer(p);
                for (EventPlayer other : this.participants) {
                    Player o = other.getPlayer();
                    if (o != null) {
                        o.showPlayer(EventMain.getInstance(), p);
                    }
                }
            }
        });

        ArenaRegions regions = this.arenaRegions;
        if (regions != null) {
            Executors.runAsync(() -> {
                try {
                    removeArenaFAWE(regions);
                } catch (Throwable t) {
                    Logger.logErr(t);
                }
            });
        }

        this.scoreboard.handleGameEnd(this.audience, () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<blue><b>Game Over")
                    .seconds(10)
                    .callback(() -> {
                        this.participants.forEach(eventPlayer -> {
                            eventPlayer.teleportAsync(this.getGameDropOffLocation());
                            eventPlayer.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    private void startGameTimerBossBar() {
        this.gameTimerBossBar = CountdownBossBar.builder()
                .title("<green>Time Left: %ss")
                .color(BossBar.Color.GREEN)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .callback(() -> {
                    this.sendAudienceMessage("<yellow>Time is up!</yellow>");
                    this.stop();
                })
                .build()
                .start();
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
            int changedOuter = session.setBlocks((Region) regions.outer(), outer);
            int changedShell = session.setBlocks((Region) regions.shell(), shell);
            int changedInner = session.setBlocks((Region) regions.inner(), inner);
            session.flushQueue();
        }
    }
    private void carvePocketsFAWE(World weWorld, List<Location> centers) {
        Pattern air = parsePattern(weWorld, "air");

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (Location c : centers) {
                CuboidRegion pocket = pocketRegion(weWorld, c);
                int changedBlocks = session.setBlocks((Region) pocket, air);
                session.flushQueue();
            }
        }
    }
    private void removeArenaFAWE(ArenaRegions regions) {
        Pattern air = parsePattern(regions.world(), "air");
        try (EditSession session = WorldEdit.getInstance().newEditSession(regions.world())) {
            int changedBlocks = session.setBlocks((Region) regions.outer(), air);
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

        return pockets;
    }

    private static CuboidRegion pocketRegion(World weWorld, Location center) {
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        BlockVector3 min = BlockVector3.at(x - 1/*to 3x3x3*/, y - 1/*to 3x3x3*/, z - 1/*to 3x3x3*/);
        BlockVector3 max = BlockVector3.at(x + 1/*to 3x3x3*/, y + 1/*to 3x3x3*/, z + 1/*to 3x3x3*/);
        return new CuboidRegion(weWorld, min, max);
    }

    private void teleportPlayersToPockets() {
        int n = Math.min(this.participants.size(), this.pocketCenters.size());
        for (int i = 0; i < n; i++) {
            this.participants.get(i).operatePlayer(this::cleanPlayer);
            this.participants.get(i).teleportAsync(this.pocketCenters.get(i));
            this.participants.get(i).sendMessage("<green>You have been placed into your mining pocket!</green>");
        }
    }

    private void cleanPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.clearActivePotionEffects();
        player.setHealth(20.0);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setExp(0.0f);
        player.setLevel(0);
        player.getInventory().clear();
        player.updateInventory();
    }

    private void equip(Player player) {

    }

    @EventHandler
    public void onEntityDamagedByEntityEvent(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (eliminated.contains(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player dead = event.getEntity();
        UUID deadId = dead.getUniqueId();
        if (!isParticipant(deadId)) return;
        if (eliminated.contains(deadId)) {
            event.setCancelled(true);
            event.getDrops().clear();
            return;
        }
        event.setCancelled(true);
        event.getDrops().clear();
        event.deathMessage(null);

        Player killer = null;
        try {
            if (event.getDamageSource().getCausingEntity() instanceof Player p) killer = p;
        } catch (Throwable ignored) {}
        if (killer == null) killer = dead.getKiller();

        Player finalKiller = killer;
        Executors.runSync(() -> {
            dropInventoryAndClear(dead);
            eliminated.add(deadId);
            if (finalKiller != null) {
                awardKill(finalKiller.getUniqueId(), deadId);
            }

            cleanPlayer(dead);
            hideFromOtherPlayers(dead);
            dead.setGameMode(GameMode.ADVENTURE);
            dead.getWorld().playSound(dead.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
            Util.sendMsg(dead, "<red>You have been eliminated!");
        });
    }

    private int aliveCount() {
        int alive = 0;
        for (EventPlayer p : this.participants) {
            if (!eliminated.contains(p.getUuid())) {
                alive++;
            }
        }
        return alive;
    }

    private boolean isParticipant(UUID uuid) {
        return this.participants.stream().anyMatch(p -> p.getUuid().equals(uuid));
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
            w.dropItemNaturally(loc, item.clone());
        }
        /*
        for (ItemStack item : dead.getInventory().getArmorContents()) {
            if (item == null || item.getType().isAir()) continue;
            w.dropItemNaturally(loc, item.clone());
        }
        ItemStack off = dead.getInventory().getItemInOffHand();
        if (off != null && !off.getType().isAir()) {
            w.dropItemNaturally(loc, off.clone());
        }
        */

        dead.getInventory().clear();
        dead.updateInventory();
    }

    private void awardKill(UUID killer, UUID victim) {
        if (killer.equals(victim)) return;
        if (!isParticipant(killer)) return;
        if (eliminated.contains(killer)) return;
        EventPlayer killerEp = getParticipant(killer);
        if (killerEp != null) {
            scoreboard.addScore(killerEp, 1);
            killerEp.sendMessage("<green>+1 kill</green> <gray>(" + scoreboard.getScore(killerEp) + " total)</gray>");
        }
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
}
