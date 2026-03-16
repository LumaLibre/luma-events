package dev.lumas.events.obj;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMileRegistry;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.hooks.BetterRTPService;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.constant.PersistentInventoryState;
import dev.lumas.lumacore.utility.ContextLogger;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
@NullMarked
public class EventPlayer implements Serializable, Scorer {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();
    @Nullable
    private transient volatile Object LOCK;

    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    private final Set<ActiveExplorerMile> activeExplorerMiles;
    @Setter
    private boolean claimedCharm;
    private long secondsPlayed;
    private volatile boolean suspended;
    private PersistentInventoryState storedInventoryState;
    private float suspendedExperience;
    private @Nullable ItemStack @Nullable[] suspendedInventory;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new HashMap<>(), new HashSet<>(), false, 0L, false,PersistentInventoryState.TRANSIENT_INVENTORY, 0f, null);
    }

    public EventPlayer(UUID uuid, Map<MinigameConstant, Integer> scores, Set<ActiveExplorerMile> activeExplorerMiles, boolean claimedCharm, long secondsPlayed, boolean suspended, PersistentInventoryState storedInventoryState, float suspendedExperience, @Nullable ItemStack @Nullable[] suspendedInventory) {
        this.uuid = uuid;
        this.scores = scores;
        this.activeExplorerMiles = activeExplorerMiles;
        this.claimedCharm = claimedCharm;
        this.secondsPlayed = secondsPlayed;
        this.suspended = suspended;
        this.storedInventoryState = storedInventoryState;
        this.suspendedExperience = suspendedExperience;
        this.suspendedInventory = suspendedInventory;
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
        player.sendMessage(m);
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

    public <T> int getCurrentQuantity(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : activeExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile.getCurrentQuantity();
            }
        }
        return 0;
    }

    // TODO: Move this implementation to somewhere else
    public void fireForExplorerMiles(Object event) {
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

    private synchronized void switchInventory() {
        this.operatePlayer(player -> {
            PlayerInventory inv = player.getInventory();
            float experience = player.getExp();
            @Nullable ItemStack[] contentsClone = inv.getContents().clone();

            if (this.suspendedInventory != null) {
                inv.setContents(this.suspendedInventory);
            } else {
                inv.clear();
            }

            if (this.suspendedExperience > 0) {
                player.setExp(this.suspendedExperience);
            } else {
                player.setExp(0);
            }

            this.suspendedExperience = experience;
            this.suspendedInventory = contentsClone;
            this.storedInventoryState = this.storedInventoryState.opposite();
        });
    }

    public void suspend() {
        this.operatePlayer(player ->  {
            List<String> worldNames = EventMain.getOkaeriConfig().getSuspendedWorlds();
            Preconditions.checkState(!worldNames.isEmpty(), "No suspended worlds are configured");
            World world = Preconditions.checkNotNull(Bukkit.getWorld(worldNames.getFirst()));


            Location worldSpawn = world.getSpawnLocation();

            Preconditions.checkState(!this.suspended, "Player is already suspended");
            this.suspended = true;

            player.teleportAsync(worldSpawn).thenAccept(success -> {
                if (!success) {
                    this.suspended = false;
                    LOGGER.warning("Failed to teleport player to suspended world spawn");
                    return; // Quietly fail here.
                }
                if (this.storedInventoryState != PersistentInventoryState.TRANSIENT_INVENTORY) {
                    this.suspended = false;
                    throw new IllegalStateException("Player inventory is not in transient state");
                }
                this.switchInventory();


                // Finished preparing and putting the player into this world with their new inventory.
                BetterRTPService.getInstance().ifPresentOrElse(service -> {
                    service.rtp(player, world);
                }, () -> LOGGER.warning("BetterRTP is not enabled, can't RTP player."));
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        });
    }

    // TODO: Use global thread
    public void unsuspend() {
        this.operatePlayer(player -> {
            Preconditions.checkState(this.suspended, "Player is not suspended");
            Preconditions.checkState(this.storedInventoryState == PersistentInventoryState.MAIN_INVENTORY, "Player inventory is not in main state");

            this.switchInventory();
            this.suspended = false;

            World unsuspendedWorld = EventMain.getOkaeriConfig().getUnsuspendWorld();

            if (unsuspendedWorld != null) {
                player.teleportAsync(unsuspendedWorld.getSpawnLocation()).thenAccept(success -> {
                    if (!success) {
                        LOGGER.warning("Failed to teleport player to unsuspend world spawn after unsuspending.");
                    }
                });
            } else {
                LOGGER.warning("No unsuspend world available.");
            }
        });
    }
}
