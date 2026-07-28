package dev.lumas.events.model;

import dev.lumas.core.util.ContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
@NullMarked
public class EventPlayer implements Serializable, Scorer {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();
    private static final Config CONFIG = EventMain.getOkaeriConfig();


    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    @Setter
    private boolean claimedCharm;
    private long secondsPlayed;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(
                uuid,
                new HashMap<>(),
                false,
                0L
        );
    }

    public EventPlayer(UUID uuid, Map<MinigameConstant, Integer> scores, boolean claimedCharm, long secondsPlayed) {
        this.uuid = uuid;
        this.scores = scores;
        this.claimedCharm = claimedCharm;
        this.secondsPlayed = secondsPlayed;
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

    public CompletableFuture<Boolean> teleportAsync(@Nullable Location location) {
        if (location == null) {
            return CompletableFuture.completedFuture(false);
        }
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
}
