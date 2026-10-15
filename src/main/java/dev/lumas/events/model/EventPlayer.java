package dev.lumas.events.model;

import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Externals;
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
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
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

    private static final boolean USE_FLOODGATE = Externals.classExists("org.geysermc.floodgate.api.FloodgateApi");


    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    @Setter
    private boolean claimedCharm;
    private long secondsPlayed;
    private boolean townInviteRewardClaimed;
    private boolean minigameDirty;
    @Setter
    private boolean initiallySpawned;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(
            uuid,
            new HashMap<>(),
            false,
            0L,
            false,
            false
        );
    }

    public EventPlayer(UUID uuid, Map<MinigameConstant, Integer> scores, boolean claimedCharm, long secondsPlayed, boolean minigameDirty, boolean initiallySpawned) {
        this.uuid = uuid;
        this.scores = scores;
        this.claimedCharm = claimedCharm;
        this.secondsPlayed = secondsPlayed;
        this.minigameDirty = minigameDirty;
        this.initiallySpawned = initiallySpawned;
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
        player.sendMessage(m.colorIfAbsent(Util.TEXT_COLOR));
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
        return Executors.teleportSafely(player, location);
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public CompletableFuture<Void> operatePlayer(Consumer<Player> consumer) {
        Player player = this.getPlayer();
        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }
        return Executors.runSyncFuture(player, () -> consumer.accept(player));
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

    public boolean isBedrock() {
        if (!USE_FLOODGATE) return false;
        return FloodgateApi.getInstance().isFloodgatePlayer(this.uuid);
    }

    public boolean isDisadvantagedPlatform() {
        if (!USE_FLOODGATE || !isBedrock()) return false;
        FloodgatePlayer floodgatePlayer = FloodgateApi.getInstance().getPlayer(this.uuid);
        if (floodgatePlayer == null) return false;
        DeviceOs os = floodgatePlayer.getDeviceOs();
        // everything else is basically unfair to kbm, listing out the ones that use kbm
        return os != DeviceOs.UWP && // windows
            os != DeviceOs.WIN32 && // windows x86
            os != DeviceOs.OSX && // mac, education edition?
            os != DeviceOs.UNKNOWN;
    }

    public boolean isDisadvantagedInput() {
        if (!USE_FLOODGATE || !isBedrock()) return false;
        FloodgatePlayer fp = FloodgateApi.getInstance().getPlayer(this.uuid);
        if (fp == null) return false;
        InputMode input = fp.getInputMode();
        return input == InputMode.TOUCH
            || input == InputMode.CONTROLLER
            || input == InputMode.VR;
        // KEYBOARD_MOUSE and UNKNOWN -> not disadvantaged
    }

    // TODO: remove?
    public synchronized boolean claimTownInviteReward() {
        if (this.townInviteRewardClaimed) {
            return false;
        }
        this.townInviteRewardClaimed = true;
        return true;
    }


    public synchronized boolean minigameDirty() {
        return this.minigameDirty;
    }

    public synchronized void minigameDirty(boolean dirty) {
        this.minigameDirty = dirty;
        EventPlayerManager.saveAsync(this);
    }

    // TODO: collapse these methods
    public synchronized void disconnectedFromMinigame() {
        this.minigameDirty = true;
    }

    public synchronized boolean wasDisconnectedFromMinigame() {
        return this.minigameDirty;
    }

    public synchronized void resetDisconnectedFromMinigame() {
        this.minigameDirty = false;
    }
}
