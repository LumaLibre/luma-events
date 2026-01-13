package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TNTRunDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRole;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRoleMap;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.WorldEditStructure;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.lumas.lumacore.utility.Logging;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// TODO: finish cleanup & test
public final class TNTRun extends InventoryUnifiedMinigame {

    private static final double DECAY_PRECISION = 1.0e-4;

    // TODO: Cleanup -> unnecessary fields

    private final WorldEditStructure worldEditStructure;
    private final Location lobbyLocation;
    private final Location arenaOrigin;
    private final int decayDelayTicks;
    private final int eliminationHeight;

    private volatile boolean arenaReady = false; // TODO: maybe unnecessary volatile?
    private volatile boolean decayArmed = false; // TODO: maybe unnecessary volatile?
    private boolean playersTeleported = false;

    private CountdownBossBar countdownBossBar;
    private CountdownBossBar gameTimerBossBar;

    private final Scoreboard<EventPlayer> scoreboard = new Scoreboard<>();
    private final MinigameRoleMap<AbstractTNTRunPlayer> roleMap = new MinigameRoleMap<>(AbstractTNTRunPlayer::cleanup);
    private final Map<BlockPos, Long> decayQueue = new HashMap<>();

    private long decayTick = 0L;
    private BukkitTask decayTask = null;
    private BukkitTask waitingForArenaTask = null;

    public TNTRun(TNTRunDefinition def) {
        super("TNT Run", "Don't fall down!", def.getTimeLimitSeconds() * 1000L, def.getHeartbeatTicks(),
                false, true, false, true);

        this.decayDelayTicks = def.getDecayDelayTicks();
        this.lobbyLocation = def.getLobbyLocation();
        this.arenaOrigin = def.getArenaOrigin();
        this.eliminationHeight = def.getEliminationHeight();
        this.worldEditStructure = new WorldEditStructure(arenaOrigin, def.getMapSchematic());
        this.boundingBox = worldEditStructure.getBoundingBox();
    }


    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: Implement
    }


    @Override
    protected void handleStart() {

        worldEditStructure.pasteAsync().whenComplete((vo, thr) -> {
            Executors.runSync(() -> {
                if (this.isCancelled()) return;
                this.arenaReady = true;
                Logging.log("[TNTRun] Arena ready.");
            });
        });

        // TODO: callbacks for this probably
        // Executors.runSync(this::stop);


        for (EventPlayer eventPlayer : this.participants) {
            ActiveTNTRunPlayer role = new ActiveTNTRunPlayer(eventPlayer, this);
            this.roleMap.put(role);
        }


        this.startDecayTask();

        if (this.arenaReady) {
            Executors.runSync(this::teleportPlayersToArenaThenStartCountdown);
            return;
        }

        this.waitingForArenaTask = Executors.repeatingSync(1L, () -> {
            if (this.isCancelled()) {
                if (waitingForArenaTask != null) waitingForArenaTask.cancel();
                waitingForArenaTask = null;
                return;
            }

            if (!this.arenaReady) return;

            if (waitingForArenaTask != null) waitingForArenaTask.cancel();
            waitingForArenaTask = null;

            teleportPlayersToArenaThenStartCountdown();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.arenaReady) return;

        if (!this.playersTeleported) {
            this.playersTeleported = true;
            this.teleportPlayersToArenaThenStartCountdown();
        }


        for (AbstractTNTRunPlayer tntRunPlayer : this.roleMap) {
            tntRunPlayer.tick();
        }

        if (this.roleMap.getMatching(ActiveTNTRunPlayer.class).size() <= 1) {
            this.stop();
        }
    }

    @Override
    protected void handleStop() {
        this.decayArmed = false;

        unsafe(() -> {
            if (waitingForArenaTask != null) {
                waitingForArenaTask.cancel();
                waitingForArenaTask = null;
            }
        });

        unsafe(this::stopDecayTask);

        unsafe(() -> {

            if (countdownBossBar != null && !countdownBossBar.isCancelled()) {
                countdownBossBar.stop(false);
            }
            if (gameTimerBossBar != null && !gameTimerBossBar.isCancelled()) {
                gameTimerBossBar.stop(false);
            }
        });

        for (AbstractTNTRunPlayer tntRunPlayer : this.roleMap) {
            tntRunPlayer.cleanup();
            tntRunPlayer.getEventPlayer().teleportAsync(this.lobbyLocation);
        }

        this.worldEditStructure.remove();
        this.decayQueue.clear();

        // TODO: fixup
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
    protected boolean handleParticipantJoin(EventPlayer eventPlayer) {
        eventPlayer.teleportAsync(this.lobbyLocation);
        return super.handleParticipantJoin(eventPlayer);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        AbstractTNTRunPlayer tntRunPlayer = this.roleMap.remove(participant.getUuid());
        tntRunPlayer.cleanup();
        return super.removeParticipant(participant);
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
        for (EventPlayer eventPlayer : this.participants) {
            eventPlayer.teleportAsync(this.arenaOrigin);
        }

        this.countdownBossBar = CountdownBossBar.builder()
                .audience(this.audience)
                .title("<yellow><b>Starting in %ss</b>")
                .color(net.kyori.adventure.bossbar.BossBar.Color.YELLOW)
                .seconds(10)
                .callback(() -> {
                    this.sendAudienceMessage("<green>TNT Run started!</green>");
                    this.decayArmed = true;
                    this.startGameTimerBossBar();
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

    private void scheduleDecayUnderFootprint(Player player) {
        if (!this.decayArmed) {
            return;
        }

        BoundingBox bb = player.getBoundingBox();
        int minX = (int) Math.floor(bb.getMinX() + DECAY_PRECISION);
        int maxX = (int) Math.floor(bb.getMaxX() - DECAY_PRECISION);
        int minZ = (int) Math.floor(bb.getMinZ() + DECAY_PRECISION);
        int maxZ = (int) Math.floor(bb.getMaxZ() - DECAY_PRECISION);
        int y0 = (int) Math.floor(bb.getMinY() - DECAY_PRECISION);

        org.bukkit.World w = player.getWorld();
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
        if (!this.arenaReady || !this.decayArmed || this.decayQueue.isEmpty()) return;

        World world = arenaOrigin.getWorld();
        if (world == null) return;

        long nowTick = this.decayTick;

        Iterator<Map.Entry<BlockPos, Long>> it = decayQueue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (e.getValue() > nowTick) continue;

            BlockPos pos = e.getKey();
            it.remove();

            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            Material type = block.getType();

            if (type.isAir() || !type.hasGravity()) continue;

            block.setType(Material.AIR, false);

            Block under = block.getRelative(0, -1, 0);
            if (under.getType() == Material.TNT) {
                under.setType(Material.AIR, false);
            }
        }
    }

    private void scheduleDecay(Block block) {
        decayQueue.putIfAbsent(
                new BlockPos(block.getX(), block.getY(), block.getZ()),
                decayTick + decayDelayTicks
        );
    }

    // TODO: Why is priority low
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !this.decayArmed) return;
        this.ensureNotIllegal();

        Player player = event.getPlayer();
        ActiveTNTRunPlayer activeTntRunPlayer = this.roleMap.as(player.getUniqueId(), ActiveTNTRunPlayer.class);

        if (activeTntRunPlayer == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        // Only process when moving across block borders
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Block blockBelow = event.getTo().getBlock().getRelative(BlockFace.DOWN);
        Material type = blockBelow.getType();
        if (type.isAir() || !type.hasGravity()) {
            return;
        }

        this.scheduleDecay(blockBelow);
    }


    @EventHandler(ignoreCancelled = true) // TODO: Probably unnecessary: worldguard
    public void onDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;

        if (this.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true) // TODO: Probably unnecessary: worldguard
    public void onBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();

        if (this.isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // TODO: Unnecessary listener? When would a player be able to place a block?
    // TODO: Probably unnecessary: worldguard
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        if (this.isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }



    public static abstract class AbstractTNTRunPlayer extends MinigameRole {
        protected final TNTRun context;

        protected AbstractTNTRunPlayer(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer);
            this.context = context;
        }

        public abstract void cleanup();

        public abstract void tick();
    }


    public static class ActiveTNTRunPlayer extends AbstractTNTRunPlayer {
        protected ActiveTNTRunPlayer(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer, context);
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void tick() {
            Player player = this.eventPlayer.getPlayer();

            if (player == null) {
                return;
            }

            player.setFoodLevel(20);

            if (player.getLocation().getY() < this.context.eliminationHeight) {
                this.eliminate();
                return;
            }

            if (this.context.decayArmed) {
                this.context.scheduleDecayUnderFootprint(player);
            }

            // add score
            this.context.scoreboard.addScore(this.eventPlayer, 1);
        }


        public void eliminate() {
            // swap to spectator role
            this.context.roleMap.swapRole(this, () -> new TNTRunSpectator(this.eventPlayer, this.context));

            this.eventPlayer.operatePlayer(player -> {
                player.playSound(player, Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
            });
            this.eventPlayer.sendMessage("<red>You have been eliminated!");
        }
    }

    private static class TNTRunSpectator extends AbstractTNTRunPlayer {

        protected TNTRunSpectator(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer, context);
            this.hide();
        }

        @Override
        public void cleanup() {
            this.show();
            this.eventPlayer.operatePlayer(player -> {
                player.setAllowFlight(false);
                player.setFlying(false);
            });
        }

        public void hide() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;

                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;

                    bukkitOther.hidePlayer(EventMain.getInstance(), self);
                }
            });
        }

        public void show() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;

                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;

                    bukkitOther.showPlayer(EventMain.getInstance(), self);
                }
            });
        }

        @Override
        public void tick() {
            this.eventPlayer.operatePlayer(player -> {
                if (!player.getAllowFlight()) { // Other plugins may interfere
                    player.setAllowFlight(true);
                }

                if (!player.isFlying()) { // Some players are not aware that they can fly, force them to fly
                    player.setFlying(true);
                }
            });
        }
    }


    private record BlockPos(int x, int y, int z) {}
}
