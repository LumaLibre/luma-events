package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.events.MinigameExitPreventionListener;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public sealed abstract class Minigame
        extends BukkitRunnable
        implements Listener
        permits BoatRace, Envoys, NonActiveMinigame, Paintball {

    protected final List<EventPlayer> participants = new ArrayList<>();
    private final MinigameExitPreventionListener exitPrevention;

    private final String name;
    private final String description;
    private final long duration;
    private final long tickInterval;
    private final boolean async;


    protected long startTime = -1;
    protected boolean open = false;
    protected boolean active = false;
    protected Audience audience;
    protected WorldTiedBoundingBox boundingBox;

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        this.exitPrevention = new MinigameExitPreventionListener(this);
    }

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        if (preventExit) {
            this.exitPrevention = new MinigameExitPreventionListener(this);
        } else {
            this.exitPrevention = null;
        }
    }


    public boolean start() {
        if (this.active) {
            return false;
        }
        this.active = true;
        this.open = true;
        this.openQueue();
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
        unregisterEvents(this);
        if (this.exitPrevention != null) {
            unregisterEvents(this.exitPrevention);
        }
        this.cancel();
        this.active = false;
        this.open = false; // Should be false by now anyway :P
        return true;
    }

    public boolean addParticipant(EventPlayer player) {
        if (!this.active || !this.open) {
            return false;
        } else if (!this.participants.contains(player)) {
            this.participants.add(player);
        }

        player.sendTitle(
                "<yellow>" + this.name,
                "<red>" + this.description
        );
        try {
            this.handleParticipantJoin(player);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return true;
    }

    private void openQueue() {
        CountdownBossBar.builder()
                .title("<aqua><b>" + name + " Starting in</b><gray>:</gray> <b>%s</b></aqua>")
                .seconds(30)
                .color(BossBar.Color.BLUE)
                .callback(() -> {
                    registerEvents(this);
                    this.audience = Audience.audience(participants.stream().map(EventPlayer::getPlayer).toList());
                    this.open = false;
                    this.startTime = System.currentTimeMillis();
                    if (this.exitPrevention != null) {
                        registerEvents(this.exitPrevention);
                    }

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

    protected void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, EventMain.getInstance());
    }

    protected void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    // Minigame starts, returns true if successful
    protected abstract void handleStart();

    protected abstract void onRunnable(long timeLeft);
    // Minigame stops, returns true if successful
    protected abstract void handleStop();

    protected abstract void handleParticipantJoin(EventPlayer player);
}
