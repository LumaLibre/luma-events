package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
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
        permits Envoys, NonActiveMinigame, Paintball {

    protected final List<EventPlayer> participants = new ArrayList<>();

    protected Audience audience;
    private final String name;
    private final String description;
    private final long duration;
    private final long tickInterval;


    protected long startTime = -1;
    protected boolean open = false;
    protected boolean active = false;

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
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
        HandlerList.unregisterAll(this);
        this.cancel();
        this.active = false;
        this.open = false; // Should be false by now anyway :P
        return true;
    }

    public boolean addParticipant(EventPlayer player) {
        if (!this.active || !this.open) {
            return false;
        }
        this.participants.add(player);
        try {
            this.handleParticipantJoin(player);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return true;
    }

    private void openQueue() {
        CountdownBossBar.builder()
                .title(name + " Starting in: %s")
                .seconds(30)
                .color(BossBar.Color.BLUE)
                .callback(() -> {
                    Bukkit.getPluginManager().registerEvents(this, EventMain.getInstance());
                    this.open = false;
                    this.startTime = System.currentTimeMillis();
                    this.runTaskTimer(EventMain.getInstance(), 0, this.tickInterval);
                    this.audience = Audience.audience(participants.stream().map(EventPlayer::getPlayer).toList());
                    try {
                        this.handleStart();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .audience(Audience.audience(Bukkit.getOnlinePlayers()))
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

    // Minigame starts, returns true if successful
    protected abstract void handleStart();

    protected abstract void onRunnable(long timeLeft);
    // Minigame stops, returns true if successful
    protected abstract void handleStop();

    protected abstract void handleParticipantJoin(EventPlayer player);
}
