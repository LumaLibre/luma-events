package dev.lumas.events.obj;

import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMile;
import dev.lumas.events.explorer.mile.ExplorerMileRegistry;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
@Setter
public class EventPlayer implements Serializable, Scorer {

    private transient volatile Object LOCK;

    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    private final Set<ActiveExplorerMile> unlockedExplorerMiles;
    private boolean claimedCharm;
    private long secondsPlayed;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new HashMap<>(), new HashSet<>(), false, 0L);
    }

    public EventPlayer(UUID uuid, Map<MinigameConstant, Integer> scores, Set<ActiveExplorerMile> unlockedExplorerMiles, boolean claimedCharm, long secondsPlayed) {
        this.uuid = uuid;
        this.scores = scores;
        this.unlockedExplorerMiles = unlockedExplorerMiles;
        this.claimedCharm = claimedCharm;
        this.secondsPlayed = secondsPlayed;
    }

    private Object getLock() {
        if (LOCK == null) {
            synchronized (this) {
                if (LOCK == null) {
                    LOCK = new Object();
                }
            }
        }
        return LOCK;
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

    @NotNull
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
        for (ActiveExplorerMile activeExplorerMile : this.unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return true;
            }
        }
        return false;
    }

    public <T> boolean unlockExplorerMile(ExplorerMile<T> explorerMile) {
        return this.unlockedExplorerMiles.add(new ActiveExplorerMile(explorerMile));
    }

    public <T> ActiveExplorerMile getActiveExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile;
            }
        }
        return null;
    }

    public <T> int getCurrentQuantity(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile.getCurrentQuantity();
            }
        }
        return 0;
    }

    // TODO: Move this implementation to somewhere else
    public void fireForExplorerMiles(Object event) {
        synchronized (this.getLock()) {
            Set<ActiveExplorerMile> testableExplorerMiles = new HashSet<>(this.unlockedExplorerMiles);

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
                        this.unlockedExplorerMiles.add(activeExplorerMile);
                        activeExplorerMile.playMilesUnlockEffect(this, null);
                    }
                }
            });
        }
    }

}
