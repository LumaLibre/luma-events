package dev.lumas.events.model;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.ContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMileRegistry;
import dev.lumas.events.explorer.order.ActiveExplorerOrder;
import dev.lumas.events.explorer.order.ExplorerOrder;
import dev.lumas.events.explorer.order.ExplorerOrderRegistry;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.hooks.BetterRTPService;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.constant.PersistentInventoryState;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@NullMarked
public class EventPlayer implements Serializable, Scorer {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();
    private static final Config CONFIG = EventMain.getOkaeriConfig();

    public static final long SUSPEND_COOLDOWN_MS = 60000;

    @Nullable
    private transient volatile Object LOCK;
    @Nullable
    private transient Optional<EventTeam> lazyTeam;
    private transient boolean sortedExplorerOrders;
    private transient long suspendCooldown;

    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    private final List<ActiveExplorerMile> activeExplorerMiles;
    @Setter
    private boolean claimedCharm;
    private long secondsPlayed;
    private volatile boolean suspended;
    private PersistentInventoryState storedInventoryState;
    private int suspendedLevels;
    private float suspendedExperience;
    private @Nullable ItemStack @Nullable[] suspendedInventory;
    private final List<ActiveExplorerOrder> activeExplorerOrders;
    @Setter
    private int souls;
    @Setter
    private int lives;
    @Setter
    private boolean initialSpawn;
    private int deaths; // legacy key
    @Setter
    private int paleSide$deaths;
    @Nullable
    private Location paleSide$lastLocation;
    @Setter
    private boolean paleSide$reclaimed;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(
                uuid,
                new HashMap<>(),
                new ArrayList<>(),
                false,
                0L,
                false,
                PersistentInventoryState.TRANSIENT_INVENTORY,
                0f,
                null,
                new ArrayList<>(),
                0,
                0,
                false,
                null
        );
    }

    public EventPlayer(UUID uuid, Map<MinigameConstant, Integer> scores, List<ActiveExplorerMile> activeExplorerMiles, boolean claimedCharm, long secondsPlayed, boolean suspended, PersistentInventoryState storedInventoryState, float suspendedExperience, @Nullable ItemStack @Nullable[] suspendedInventory, List<ActiveExplorerOrder> activeExplorerOrders, int souls, int lives, boolean initialSpawn, @Nullable Location paleSide$lastLocation) {
        this.uuid = uuid;
        this.scores = scores;
        this.activeExplorerMiles = activeExplorerMiles;
        this.claimedCharm = claimedCharm;
        this.secondsPlayed = secondsPlayed;
        this.suspended = suspended;
        this.storedInventoryState = storedInventoryState;
        this.suspendedExperience = suspendedExperience;
        this.suspendedInventory = suspendedInventory;
        this.activeExplorerOrders = activeExplorerOrders;
        this.souls = souls;
        this.lives = lives;
        this.initialSpawn = initialSpawn;
        this.paleSide$lastLocation = paleSide$lastLocation;
    }

    private Object getLock() {
        if (LOCK == null) {
            synchronized (this) {
                if (LOCK == null) {
                    LOCK = new Object();
                }
            }
        }
        return Objects.requireNonNull(LOCK);
    }


    public void sendMessage(String m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        Util.sendMsg(player, m);
    }

    public void sendNoPrefixedMessage(String m) {
        this.sendNoPrefixedMessage(Util.color(m));
    }

    public void sendNoPrefixedMessage(Component m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(m.colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR)));
    }

    public void sendActionBar(String m) {
        this.sendActionBar(Util.color(m));
    }

    public void sendActionBar(Component m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendActionBar(m);
    }

    public void sendTitle(String title, String subtitle) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.showTitle(Title.title(Util.color(title), Util.color(subtitle)));
    }

    public CompletableFuture<Boolean> teleportAsync(Location location) {
        Player player = this.getPlayer();
        if (player == null) {
            return CompletableFuture.completedFuture(false);
        }
        return player.teleportAsync(location);
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public void operatePlayer(Consumer<Player> consumer) {
        Player player = this.getPlayer();
        if (player != null) {
            Executors.runSync(player, () -> consumer.accept(player));
        }
    }

    public void operatePlayerSafely(Consumer<Player> consumer) {
        Player player = this.getPlayer();
        if (player != null) {
            if (Bukkit.isOwnedByCurrentRegion(player)) {
                Executors.runSync(player, () -> consumer.accept(player));
            } else {
                consumer.accept(player);
            }
        }
    }

    public void addBossBar(BossBar bossBar) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        bossBar.addViewer(player);
    }

    public void removeBossBar(BossBar bossBar) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        bossBar.removeViewer(player);
    }

    public boolean isOnline() {
        Player player = this.getPlayer();
        if (player == null) {
            return false;
        }
        return player.isOnline();
    }

    @Override
    @Nullable
    public String getName() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.uuid);
        return offlinePlayer.getName();
    }

    public void addPermanentScore(MinigameConstant minigame, int score) {
        this.scores.put(minigame, this.scores.getOrDefault(minigame, 0) + score);
    }

    public void setPermanentScore(MinigameConstant minigame, int score) {
        this.scores.put(minigame, score);
    }

    public int getPermanentScore(MinigameConstant minigame) {
        return this.scores.getOrDefault(minigame, 0);
    }

    public Map<MinigameConstant, Integer> getPermanentScores() {
        return Map.copyOf(this.scores);
    }

    public void addSecondsPlayed(long seconds) {
        this.secondsPlayed += seconds;
    }


    public <T> boolean hasUnlockedExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : this.activeExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return true;
            }
        }
        return false;
    }

    public <T> boolean unlockExplorerMile(ExplorerMile<T> explorerMile) {
        return this.activeExplorerMiles.add(new ActiveExplorerMile(explorerMile));
    }

    @Nullable
    public <T> ActiveExplorerMile getActiveExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : activeExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile;
            }
        }
        return null;
    }

    // TODO: Move this implementation to somewhere else
    public void fireForExplorerMiles(Object event) {
        if (!CONFIG.getExplorer().isExplorerMiles()) {
            return;
        }

        synchronized (this.getLock()) {
            Set<ActiveExplorerMile> testableExplorerMiles = new HashSet<>(this.activeExplorerMiles);

            for (ExplorerMile<?> explorerMile : ExplorerMileRegistry.jvmUnifiedMap().values()) {
                if (explorerMile.getEventClass() == event.getClass() && !hasUnlockedExplorerMile(explorerMile)) {
                    //Util.log("Testing an ExplorerMile for which a player does not have any data for: " + explorerMile);
                    testableExplorerMiles.add(new ActiveExplorerMile(explorerMile));
                }
            }


            for (ActiveExplorerMile activeExplorerMile : testableExplorerMiles) {
                if (activeExplorerMile == null) {
                    continue;
                }
                if (activeExplorerMile.getMile().getEventClass() == event.getClass()) {
                    activeExplorerMile.apply(event, this);
                }
            }

            testableExplorerMiles.forEach(activeExplorerMile -> {
                if (!hasUnlockedExplorerMile(activeExplorerMile.getMile())) {
                    if (activeExplorerMile.hasProgress()) {
                        this.activeExplorerMiles.add(activeExplorerMile);
                        activeExplorerMile.playMilesUnlockEffect(this, null);
                    }
                }
            });
        }
    }

    public boolean hasStartedExplorerOrder(ExplorerOrder<?> explorerOrder) {
        for (ActiveExplorerOrder activeOrder : this.activeExplorerOrders) {
            if (activeOrder == null) continue;
            if (Objects.equals(activeOrder.getExplorerOrder().getFIELD_NAME(), explorerOrder.getFIELD_NAME())) {
                return true;
            }
        }
        return false;
    }

    public void resortExplorerOrders() {
        this.sortedExplorerOrders = true;
        this.activeExplorerOrders.sort((o1, o2) ->
                Long.compare(o2.getCompletedAt(), o1.getCompletedAt()));
    }

    @Nullable
    public ActiveExplorerOrder getLastExplorerOrder(Predicate<ActiveExplorerOrder> filter) {
        if (!this.sortedExplorerOrders) {
            // sort by completion time with latest time coming first
            resortExplorerOrders();
        }

        for (ActiveExplorerOrder activeOrder : this.activeExplorerOrders) {
            if (filter.test(activeOrder)) {
                return activeOrder;
            }
        }
        return null;
    }

    @Nullable
    public <T> ActiveExplorerOrder getActiveExplorerOrder(ExplorerOrder<T> explorerOrder) {
        for (ActiveExplorerOrder activeExplorerMile : activeExplorerOrders) {
            if (Objects.equals(activeExplorerMile.getExplorerOrder().getFIELD_NAME(), explorerOrder.getFIELD_NAME())) {
                return activeExplorerMile;
            }
        }
        return null;
    }

    public void fireForExplorerOrders(World world, Object event) {
        if (!CONFIG.getExplorer().isExplorerOrders()) {
            return;
        }

        synchronized (this.getLock()) {
            for (ExplorerOrder<?> nonActiveExplorerOrder : ExplorerOrderRegistry.jvmUnifiedMap().values()) {
                if (nonActiveExplorerOrder.getEventClass() == event.getClass() && !hasStartedExplorerOrder(nonActiveExplorerOrder)) {
                    activeExplorerOrders.add(new ActiveExplorerOrder(nonActiveExplorerOrder, 0, false, -1));
                }
            }

            // copying to prevent downstream mutation problems
            for (ActiveExplorerOrder activeOrder : new ArrayList<>(this.activeExplorerOrders)) {
                if (activeOrder != null && activeOrder.getExplorerOrder().getEventClass() == event.getClass()) {
                    activeOrder.apply(world, event, this);
                }
            }
        }
    }

    private synchronized void switchInventory() {
        this.operatePlayerSafely(player -> {
            PlayerInventory inv = player.getInventory();
            int level = player.getLevel();
            float experience = player.getExp();
            @Nullable ItemStack[] contentsClone = inv.getContents().clone();

            if (this.suspendedInventory != null) {
                inv.setContents(this.suspendedInventory);
            } else {
                inv.clear();
            }

            if (this.suspendedExperience > 0) {
                player.setLevel(this.suspendedLevels);
                player.setExp(this.suspendedExperience);
            } else {
                player.setLevel(0);
                player.setExp(0);
            }

            this.suspendedLevels = level;
            this.suspendedExperience = experience;
            this.suspendedInventory = contentsClone;
            this.storedInventoryState = this.storedInventoryState.opposite();
        });
    }

    public CompletableFuture<Boolean> suspend(boolean rtp) {
        if (!CONFIG.getExplorer().isExplorerOrders()) {
            return CompletableFuture.completedFuture(false);
        }

        if (System.currentTimeMillis() < this.suspendCooldown) {
            sendMessage("You cannot suspend again so soon. Please wait a bit before trying again.");
            return CompletableFuture.completedFuture(false);
        }

        this.suspendCooldown = System.currentTimeMillis() + SUSPEND_COOLDOWN_MS;

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        this.operatePlayerSafely(player ->  {
            List<String> worldNames = EventMain.getOkaeriConfig().getExplorer().getSuspendedWorlds();
            Preconditions.checkState(!worldNames.isEmpty(), "No suspended worlds are configured");
            World world = Preconditions.checkNotNull(Bukkit.getWorld(worldNames.getFirst()));


            Location worldSpawn = world.getSpawnLocation();

            Preconditions.checkState(!this.suspended, "Player is already suspended");
            this.suspended = true;

            if (this.storedInventoryState != PersistentInventoryState.TRANSIENT_INVENTORY) {
                this.suspended = false;
                future.completeExceptionally(new IllegalStateException("Player inventory is not in transient state"));
                return;
            }

            this.switchInventory();
            player.clearActivePotionEffects();
            //this.invincible = System.currentTimeMillis() + INVINCIBLE_TIME_MS; // TODO



            if (rtp || this.paleSide$lastLocation == null) {
                BetterRTPService.getInstance().ifPresentOrElse(service -> {
                    service.rtp(player, world);
                }, () -> {
                    LOGGER.warning("BetterRTP is not enabled, can't RTP player.");
                    player.teleportAsync(worldSpawn).thenAccept(success -> {
                        if (!success) {
                            this.suspended = false;
                            LOGGER.warning("Failed to teleport player to suspended world spawn");
                            future.complete(false);
                        } else {
                            future.complete(true);
                        }
                    }).exceptionally(throwable -> {
                        future.completeExceptionally(throwable);
                        return null;
                    });
                });
            } else {
                player.teleportAsync(this.paleSide$lastLocation).thenAccept(success -> {
                    if (!success) {
                        this.suspended = false;
                        LOGGER.warning("Failed to teleport player to last Pale Side location");
                        future.complete(false);
                    }
                });
                this.sendMessage("You have been teleported to your last Pale Side location.");
            }
        });
        return future;
    }


    public void unsuspend() {
        unsuspend(false);
    }

    public void unsuspend(boolean removeLastLocation) {
        this.operatePlayerSafely(player -> {
            Preconditions.checkState(this.suspended, "Player is not suspended");

            this.paleSide$lastLocation = removeLastLocation ? null : player.getLocation();

            if (this.storedInventoryState == PersistentInventoryState.MAIN_INVENTORY) {
                this.switchInventory();
            } else {
                LOGGER.warning("Unsuspending " + player.getName() + " with TRANSIENT inventory state, save likely occurred before suspend completed.");
            }
            this.suspended = false;

            Location dropOffLocation = EventMain.getOkaeriConfig().getExplorer().getUnsuspendDropOffLocation();
            World unsuspendedWorld = EventMain.getOkaeriConfig().getUnsuspendWorld();

            player.clearActivePotionEffects();

            if (dropOffLocation != null) {
                player.teleportAsync(dropOffLocation).thenAccept(success -> {
                    if (!success) {
                        LOGGER.warning("Failed to teleport player to unsuspend world spawn after unsuspending.");
                    }
                });

            } else if (unsuspendedWorld != null) {
                player.teleportAsync(unsuspendedWorld.getSpawnLocation()).thenAccept(success -> {
                    if (!success) {
                        LOGGER.warning("Failed to teleport player to unsuspend world spawn after unsuspending.");
                    }
                });
            } else {
                {
                    LOGGER.warning("No unsuspend world available.");
                }
            }
        });
    }


    /**
     * Prefer {@link EventTeamManager#getByMember(EventPlayer)}.
     * This method should only be used for repetitive lookups.
     * @return the {@link EventTeam} this player is in, or null if they are not in a team.
     */
    @Nullable
    public EventTeam getLazyTeam() {
        if (this.lazyTeam == null) {
            EventTeam team = EventTeamManager.getByMember(this);
            this.lazyTeam = Optional.ofNullable(team);
        }
        return this.lazyTeam.orElse(null);
    }

    public void invalidateLazyTeam() {
        this.lazyTeam = null; // forces re-lookup on next call
    }

    public void updateLazyTeam(@Nullable EventTeam team) {
        this.lazyTeam = Optional.ofNullable(team);
    }
}
