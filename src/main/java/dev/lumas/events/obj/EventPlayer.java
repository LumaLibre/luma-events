package dev.lumas.events.obj;

import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
@Setter
@AllArgsConstructor
public class EventPlayer implements Serializable, Scorer {

    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    private boolean claimedCharm;
    private long secondsPlayed;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new HashMap<>(), false, 0L);
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
            return null;
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
            Executors.runSync(() -> consumer.accept(player));
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

}
