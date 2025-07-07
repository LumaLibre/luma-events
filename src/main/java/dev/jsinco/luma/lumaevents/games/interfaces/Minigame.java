package dev.jsinco.luma.lumaevents.games.interfaces;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.events.MinigameExitPreventionListener;
import dev.jsinco.luma.lumaevents.games.events.MinigamePreventInventoryTampering;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.MinigameBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Getter
@Setter
public abstract class Minigame extends BukkitRunnable implements Listener {

    protected static final Random RANDOM = Util.RANDOM;

    protected final List<EventPlayer> participants = new ArrayList<>();
    protected final List<Listener> extraListeners = new ArrayList<>();
    private final MinigamePreventInventoryTampering inventoryTampering;

    private final String name;
    private final String description;
    private final long duration;
    private final long tickInterval;
    private final boolean async;


    protected long startTime = -1;
    protected boolean open = false;
    protected boolean active = false;
    protected Audience audience;
    protected MinigameBoundingBox boundingBox;

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        this.extraListeners.add(new MinigameExitPreventionListener(this));
        this.inventoryTampering = new MinigamePreventInventoryTampering(this);
        registerEvents(this.inventoryTampering);
    }

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit, boolean preventInventoryTampering) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        if (preventExit) {
            this.extraListeners.add(new MinigameExitPreventionListener(this));
        }
        this.inventoryTampering = preventInventoryTampering ? new MinigamePreventInventoryTampering(this) : null;
        if (this.inventoryTampering != null) {
            registerEvents(this.inventoryTampering);
        }
    }


    public boolean start() {
        return this.start(90);
    }

    public boolean start(int seconds) {
        if (this.active) {
            return false;
        }
        this.active = true;
        this.open = true;
        this.openQueue(seconds);
        return true;
    }

    public boolean stop() {
        if (!this.active) {
            return false;
        }
        try {
            this.handleStop();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        try {
            Bukkit.getScheduler().runTaskLater(EventMain.getInstance(), this::onPostStop, 3L);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        unregisterEvents(this);
        extraListeners.stream()
                .filter(Objects::nonNull)
                .forEach(this::unregisterEvents);
        if (this.inventoryTampering != null) {
            unregisterEvents(this.inventoryTampering);
        }
        this.cancel();

        this.active = false;
        this.open = false; // Should be false by now anyway :P
        return true;
    }

    public boolean addParticipant(EventPlayer player) {
        if (!this.active || !this.open) {
            return false;
        }

        try {
            if(!this.handleParticipantJoin(player)) {
                player.sendMessage("This minigame has denied your entry.");
                return false;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            player.sendMessage("An error occurred while trying to join the minigame.");
            return false;
        }


        if (!this.participants.contains(player)) {
            this.participants.add(player);
        }
        player.sendTitle(
                "<yellow>" + this.name,
                "<red>" + this.description
        );
        return true;
    }

    public boolean removeParticipant(EventPlayer player) {
        if (!this.active) {
            return false;
        }
        this.participants.remove(player);
        Location loc = this.getGameDropOffLocation();
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && loc != null) {
            bukkitPlayer.teleportAsync(loc);
            Util.sendMsg(bukkitPlayer, "You have been removed from the active minigame!");
        }
        return true;
    }

    private void openQueue(int seconds) {
        CountdownBossBar.builder()
                .title("<aqua><b>" + name + " Starting in</b><gray>:</gray> <b>%ss</b></aqua>")
                .seconds(seconds)
                .color(BossBar.Color.BLUE)
                .callback(() -> {
                    if (this.participants.size() < this.minimumParticipants()) {
                        // Nothing has happened at this point other than these values
                        // being changed to true, so we can just set them to false and return
                        this.active = false;
                        this.open = false;
                        if (this.inventoryTampering != null) {
                            unregisterEvents(this.inventoryTampering);
                        }
                        Util.broadcast("Not enough players joined to start " + this.name);
                        return;
                    }
                    this.onPreStart();

                    registerEvents(this);
                    this.audience = Audience.audience(participants.stream()
                                    .map(EventPlayer::getPlayer).filter(Objects::nonNull).toList());
                    this.open = false;
                    this.startTime = System.currentTimeMillis();
                    extraListeners.stream()
                            .filter(Objects::nonNull)
                            .forEach(this::registerEvents);

                    try {
                        this.handleStart();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    if (async) {
                        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, this.tickInterval);
                    } else {
                        this.runTaskTimer(EventMain.getInstance(), 0, this.tickInterval);
                    }
                })
                .global(true)
                .build()
                .start();
    }

    @Override
    public void run() {
        long timeLeft = this.duration - (System.currentTimeMillis() - this.startTime);
        if (timeLeft <= 0) {
            this.stop();
            return;
        }
        try {
            this.onRunnable(timeLeft);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void ensureNotIllegal() {
        if (!this.isActive()) {
            throw new GameComponentIllegallyActive("Minigame is not active");
        }
    }

    protected boolean addExtraListener(Listener listener) {
        if (listener == null) {
            return false;
        }
        this.extraListeners.add(listener);
        if (!this.open) {
            registerEvents(listener);
        }
        return true;
    }

    protected void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, EventMain.getInstance());
    }

    protected void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    public Location getGameDropOffLocation() {
        return EventMain.getOkaeriConfig().getGameDropOffLocation();
    }

    public void sendAudienceMessage(String m) {
        if (this.audience == null) {
            return;
        }
        this.audience.sendMessage(Util.color(Util.PREFIX + m, TextColor.fromHexString(Util.TEXT_COLOR)));
    }

    protected boolean isParticipant(EventPlayer... players) {
        for (EventPlayer p : players) {
            if (!this.participants.contains(p)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInBoundingBox(EventPlayer... players) {
        for (EventPlayer p : players) {
            Player bukkitPlayer = p.getPlayer();
            if (bukkitPlayer == null) return false;
            if (!this.boundingBox.contains(bukkitPlayer.getLocation())) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInBoundingBox(Player... players) {
        for (Player p : players) {
            if (!this.boundingBox.contains(p.getLocation())) {
                return false;
            }
        }
        return true;
    }

    protected void onPreStart() {
        // This method can be overridden to perform actions before the minigame starts
        // For example, setting up the environment, clearing inventories, etc.
    }

    protected void onPostStop() {
        // This method can be overridden to perform actions after the minigame stops
        // For example, restoring player inventories, cleaning up resources, etc.
    }

    protected int minimumParticipants() {
        // This method can be overridden to specify the minimum number of participants required to start the minigame
        return 1; // Default is 1 participant
    }

    // Minigame starts, returns true if successful
    protected abstract void handleStart();

    protected abstract void onRunnable(long timeLeft);
    // Minigame stops, returns true if successful
    protected abstract void handleStop();

    protected abstract boolean handleParticipantJoin(EventPlayer player);
}
