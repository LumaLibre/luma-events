package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.World;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TNTRunDefinition;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class TNTRun extends InventoryUnifiedMinigame {

    private final Location lobbyLocation;
    private final Location arenaOrigin;
    private final int decayDelayTicks;
    private final int eliminationHeight;

    private volatile boolean arenaReady = false;
    private volatile boolean decayArmed = false;

    private CountdownBossBar countdownBossBar;
    private CountdownBossBar gameTimerBossBar;

    private final Scoreboard<EventPlayer> scoreboard = new Scoreboard<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final Map<UUID, Set<UUID>> hiddenByViewer = new HashMap<>();

    private final File mapsFolder =
            new File(EventMain.getInstance().getDataFolder(), "assets/tntrun-maps");
    private CuboidRegion pastedRegion = null;

    private BukkitTask decayTask = null;
    private long decayTick = 0L;

    private record BlockPos(int x, int y, int z) {}
    private final Map<BlockPos, Long> decayQueue = new HashMap<>();

    private BukkitTask waitingForArenaTask = null;

    public TNTRun(TNTRunDefinition def) {
        super("TNT Run", "Don't fall down!", def.getTimeLimitSeconds() * 1000L, def.getHeartbeatTicks(),
                true, true, false, true);

        this.decayDelayTicks = def.getDecayDelayTicks();
        this.lobbyLocation = def.getLobbyLocation();
        this.arenaOrigin = def.getArenaOrigin();
        this.eliminationHeight = def.getEliminationHeight();
    }

    @Override
    protected void onPreStart() {
        super.onPreStart();

        this.arenaReady = false;
        this.pastedRegion = null;
        this.boundingBox = null;

        File map = pickRandomMapFile();
        if (map == null) {
            Logger.logWrn("[TNTRun] No maps found in " + mapsFolder.getPath());
            this.stop();
            return;
        }

        Logger.log("[TNTRun] Pre-generating map: " + map.getName());

        Executors.runAsync(() -> {
            try {
                Clipboard clipboard = loadClipboard(map);

                org.bukkit.World bw = arenaOrigin.getWorld();
                Preconditions.checkNotNull(bw, "arenaOrigin world is null");
                World weWorld = BukkitAdapter.adapt(bw);

                pastedRegion = computePastedRegion(clipboard, arenaOrigin);
                this.boundingBox = regionToBoundingBox(bw, pastedRegion);

                pasteClipboard(weWorld, clipboard, arenaOrigin);

                Executors.runSync(() -> {
                    if (this.isCancelled()) return;
                    this.arenaReady = true;
                    Logger.log("[TNTRun] Arena ready.");
                });

            } catch (Throwable t) {
                Logger.logErr(t);
                Executors.runSync(this::stop);
            }
        });
    }

    @Override
    protected void handleStart() {
        this.decayArmed = false;
        this.eliminated.clear();
        decayQueue.clear();

        this.decayTick = 0L;
        stopDecayTask();
        startDecayTask();

        if (waitingForArenaTask != null) {
            waitingForArenaTask.cancel();
            waitingForArenaTask = null;
        }

        if (arenaReady) {
            Executors.runSync(this::teleportPlayersToArenaThenStartCountdown);
            return;
        }

        waitingForArenaTask = Executors.repeatingSync(1L, () -> {
            if (this.isCancelled()) {
                if (waitingForArenaTask != null) waitingForArenaTask.cancel();
                waitingForArenaTask = null;
                return;
            }

            if (!arenaReady) return;

            if (waitingForArenaTask != null) waitingForArenaTask.cancel();
            waitingForArenaTask = null;

            teleportPlayersToArenaThenStartCountdown();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!arenaReady) return;

        Executors.runSync(() -> {
            for (EventPlayer ep : this.participants) {
                if (eliminated.contains(ep.getUuid())) continue;
                Player p = ep.getPlayer();
                if (p == null) continue;

                if (p.getLocation().getY() < eliminationHeight) {
                    eliminate(ep);
                    continue;
                }

                if (decayArmed && p.getGameMode() == GameMode.SURVIVAL) {
                    scheduleDecayUnderFootprint(p);
                }
            }

            for (EventPlayer ep : this.participants) {
                if (eliminated.contains(ep.getUuid())) continue;
                scoreboard.addScore(ep, 1);
            }

            if (aliveCount() <= 1) this.stop();
        });
    }

    @Override
    protected void handleStop() {
        if (waitingForArenaTask != null) {
            waitingForArenaTask.cancel();
            waitingForArenaTask = null;
        }
        stopDecayTask();
        unsafe(() -> {
            if (countdownBossBar != null && !countdownBossBar.isCancelled()) countdownBossBar.stop(false);
            if (gameTimerBossBar != null && !gameTimerBossBar.isCancelled()) gameTimerBossBar.stop(false);
        });

        this.decayArmed = false;

        Executors.runSync(() -> {
            for (EventPlayer ep : this.participants) {
                Player player = ep.getPlayer();
                ep.teleportAsync(this.lobbyLocation);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    cleanPlayer(player);
                }
            }
            restoreAllVisibility();
        });

        CuboidRegion region = this.pastedRegion;
        if (region != null) {
            Executors.runAsync(() -> {
                try (EditSession session = WorldEdit.getInstance().newEditSession(region.getWorld())) {
                    session.setBlocks((Region) region, com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState());
                    session.flushQueue();
                } catch (Throwable t) {
                    Logger.logErr(t);
                }
            });
        }

        this.pastedRegion = null;
        decayQueue.clear();

        this.scoreboard.handleGameEnd(this.audience, () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
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

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        player.teleportAsync(this.lobbyLocation);
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        UUID uuid = participant.getUuid();
        eliminated.add(uuid);
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
        });
        return super.removeParticipant(participant);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    private void startDecayTask() {
        this.decayTask = Executors.repeatingSync(1L, () -> {
            if (!arenaReady) return;
            decayTick++;
            processDecayQueue();
        });
    }

    private void stopDecayTask() {
        if (this.decayTask != null) {
            this.decayTask.cancel();
            this.decayTask = null;
        }
    }

    private void teleportPlayersToArenaThenStartCountdown() {
        for (EventPlayer ep : this.participants) {
            Player p = ep.getPlayer();
            if (p != null) {
                cleanPlayer(p);
                p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION,
                    0, false, false, false
                ));
            }
            ep.teleportAsync(arenaOrigin);
        }

        this.countdownBossBar = CountdownBossBar.builder()
                .audience(this.audience)
                .title("<yellow><b>Starting in %ss</b>")
                .color(net.kyori.adventure.bossbar.BossBar.Color.YELLOW)
                .seconds(10)
                .callback(() -> {
                    this.sendAudienceMessage("<green>TNTRun started!</green>");
                    this.decayArmed = true;
                    startGameTimerBossBar();
                })
                .build()
                .start();
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

    private static final double PRECISION = 1.0e-4;
    private void scheduleDecayUnderFootprint(Player p) {
        if (!decayArmed) return;

        BoundingBox bb = p.getBoundingBox();
        int minX = (int) Math.floor(bb.getMinX() + PRECISION);
        int maxX = (int) Math.floor(bb.getMaxX() - PRECISION);
        int minZ = (int) Math.floor(bb.getMinZ() + PRECISION);
        int maxZ = (int) Math.floor(bb.getMaxZ() - PRECISION);
        int y0 = (int) Math.floor(bb.getMinY() - PRECISION);

        org.bukkit.World w = p.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int dy = 0; dy <= 1; dy++) {
                    Block block = w.getBlockAt(x, y0 - dy, z);
                    Block above = w.getBlockAt(x, y0 - dy + 1, z);
                    if (block.getType().isAir() || !above.getType().isAir()) continue;
                    if (!block.getType().hasGravity()) continue;
                    scheduleDecay(block);
                }
            }
        }
    }

    private void processDecayQueue() {
        if (!arenaReady || !decayArmed) return;
        if (decayQueue.isEmpty()) return;

        org.bukkit.World w = arenaOrigin.getWorld();
        if (w == null) return;

        long nowTick = decayTick;

        Iterator<Map.Entry<BlockPos, Long>> it = decayQueue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (e.getValue() > nowTick) continue;

            BlockPos pos = e.getKey();
            it.remove();

            Block b = w.getBlockAt(pos.x(), pos.y(), pos.z());
            Material type = b.getType();

            if (type.isAir() || !type.hasGravity()) continue;

            b.setType(Material.AIR, false);

            Block under = b.getRelative(0, -1, 0);
            if (under.getType() == Material.TNT) {
                under.setType(Material.AIR, false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        this.ensureNotIllegal();
        if (!arenaReady) return;
        if (!decayArmed) return;

        Location to = event.getTo();
        Player player = event.getPlayer();
        if (!isParticipant(player.getUniqueId())) return;
        if (eliminated.contains(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        if ( // Only process when moving across block borders
            event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
        ) return;

        Block belowTo = to.getWorld().getBlockAt(to.getBlockX(), to.getBlockY() - 1, to.getBlockZ());
        Material m = belowTo.getType();
        if (m.isAir() || !m.hasGravity()) return;
        scheduleDecay(belowTo);
    }

    private void scheduleDecay(Block block) {
        decayQueue.putIfAbsent(
                new BlockPos(block.getX(), block.getY(), block.getZ()),
                decayTick + decayDelayTicks
        );
    }

    private void eliminate(EventPlayer ep) {
        if (!eliminated.add(ep.getUuid())) return;
        Player player = ep.getPlayer();
        if (player == null) return;

        cleanPlayer(player);
        player.setAllowFlight(true);
        player.setFlying(true);

        ep.teleportAsync(arenaOrigin);
        updateSpectatorVisibilityFor(ep.getUuid());

        player.playSound(player, Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
        Util.sendMsg(player, "<red>You have been eliminated!");
    }

    private int aliveCount() {
        int alive = 0;
        for (EventPlayer p : this.participants) {
            if (!eliminated.contains(p.getUuid())) alive++;
        }
        return alive;
    }

    private boolean isParticipant(UUID uuid) {
        return this.participants.stream().anyMatch(p -> p.getUuid().equals(uuid));
    }

    private void updateSpectatorVisibilityFor(UUID eliminatedId) {
        Player eliminatedPlayer = Bukkit.getPlayer(eliminatedId);
        if (eliminatedPlayer == null) return;

        for (EventPlayer viewerEp : this.participants) {
            Player viewer = viewerEp.getPlayer();
            if (viewer == null) continue;
            if (viewer.getUniqueId().equals(eliminatedId)) continue;

            viewer.hidePlayer(EventMain.getInstance(), eliminatedPlayer);
            hiddenByViewer.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>()).add(eliminatedId);
        }

        for (EventPlayer targetEp : this.participants) {
            UUID targetId = targetEp.getUuid();
            Player target = targetEp.getPlayer();
            if (target == null) continue;
            if (targetId.equals(eliminatedId)) continue;

            if (eliminated.contains(targetId)) {
                eliminatedPlayer.hidePlayer(EventMain.getInstance(), target);
                hiddenByViewer.computeIfAbsent(eliminatedId, k -> new HashSet<>()).add(targetId);
            } else {
                eliminatedPlayer.showPlayer(EventMain.getInstance(), target);
                Set<UUID> set = hiddenByViewer.get(eliminatedId);
                if (set != null) set.remove(targetId);
            }
        }
    }

    private void restoreAllVisibility() {
        for (EventPlayer a : this.participants) {
            Player pa = a.getPlayer();
            if (pa == null) continue;
            for (EventPlayer b : this.participants) {
                Player pb = b.getPlayer();
                if (pb == null) continue;
                pa.showPlayer(EventMain.getInstance(), pb);
            }
        }
        hiddenByViewer.clear();
    }

    private void cleanPlayer(Player player) {
        player.clearActivePotionEffects();
        player.setHealth(20.0);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setExp(0.0f);
        player.setLevel(0);
        player.getInventory().clear();
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player p)) return;
        if (!isParticipant(p.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        this.ensureNotIllegal();
        if (event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(true);
            event.getEntity().remove();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        this.ensureNotIllegal();
        if (event.getEntity() instanceof TNTPrimed) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    private @Nullable File pickRandomMapFile() {
        if (!mapsFolder.exists()) mapsFolder.mkdirs();
        File[] files = mapsFolder.listFiles(f ->
                f.isFile() && (f.getName().endsWith(".schem") || f.getName().endsWith(".schematic"))
        );
        if (files == null || files.length == 0) return null;
        return files[ThreadLocalRandom.current().nextInt(files.length)];
    }

    private static Clipboard loadClipboard(File file) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IllegalArgumentException("Unknown schematic format: " + file.getName());

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    private static void pasteClipboard(World weWorld, Clipboard clipboard, Location pasteAt) throws Exception {
        BlockVector3 to = BlockVector3.at(pasteAt.getBlockX(), pasteAt.getBlockY(), pasteAt.getBlockZ());

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation op = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(to)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(op);
            session.flushQueue();
        }
    }

    private static CuboidRegion computePastedRegion(Clipboard clipboard, Location pasteAt) {
        org.bukkit.World bw = pasteAt.getWorld();
        Preconditions.checkNotNull(bw, "pasteAt world is null");
        World weWorld = BukkitAdapter.adapt(bw);

        BlockVector3 clipMin = clipboard.getRegion().getMinimumPoint();
        BlockVector3 clipMax = clipboard.getRegion().getMaximumPoint();
        BlockVector3 clipOrigin = clipboard.getOrigin();

        BlockVector3 base = BlockVector3.at(pasteAt.getBlockX(), pasteAt.getBlockY(), pasteAt.getBlockZ());

        BlockVector3 worldMin = base.add(clipMin.subtract(clipOrigin));
        BlockVector3 worldMax = base.add(clipMax.subtract(clipOrigin));

        return new CuboidRegion(weWorld, worldMin, worldMax);
    }

    private static WorldTiedBoundingBox regionToBoundingBox(org.bukkit.World bw, CuboidRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        Location lMin = new Location(bw, min.x(), min.y(), min.z());
        Location lMax = new Location(bw, max.x(), max.y(), max.z());
        return WorldTiedBoundingBox.of(lMin, lMax);
    }
}
