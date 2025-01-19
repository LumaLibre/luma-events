package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import lombok.Setter;
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

    private final String name;
    private final String description;
    private final long duration;
    private final long tickInterval;


    protected long startTime = -1;
    protected boolean open = false;
    protected boolean active = false;

    protected Minigame(String name, String description, long duration, long tickInterval) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
    }


    protected boolean start() {
        if (this.active) {
            return false;
        }
        this.active = true;
        this.open = true;
        this.openQueue();
        return true;
    }

    protected boolean stop() {
        if (!this.active) {
            return false;
        }
        this.handleStop();
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
        return true;
    }

    private void openQueue() {
        CountdownBossBar.builder()
                .title(name + " Starting in: %s")
                .seconds(30)
                .color(BossBar.Color.BLUE)
                .callback(() -> {
                    this.handleStart();
                    Bukkit.getPluginManager().registerEvents(this, EventMain.getInstance());
                    this.open = false;
                    this.startTime = System.currentTimeMillis();
                })
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
        this.onRunnable(timeLeft);
    }

    // Minigame starts, returns true if successful
    protected abstract void handleStart();

    protected abstract void onRunnable(long timeLeft);
    // Minigame stops, returns true if successful
    protected abstract void handleStop();
}
