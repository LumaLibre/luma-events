package dev.lumas.events.games.models;

import dev.lumas.lumacore.utility.Logging;
import dev.lumas.events.EventMain;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

@Builder
public class GlobalCountdownActionBar extends BukkitRunnable {

    public static final ConcurrentLinkedQueue<GlobalCountdownActionBar> activeCountdowns = new ConcurrentLinkedQueue<>();

    private final String message;
    private final Runnable callback;

    @Getter
    private float seconds;


    public GlobalCountdownActionBar(String message, Runnable callback, float seconds) {
        this.message = message;
        this.callback = callback;
        this.seconds = seconds;
    }


    public GlobalCountdownActionBar start() {
        if (EventMain.STOPPING) {
            if (this.callback != null) this.callback.run();
            Logging.errorLog("Cannot start GlobalCountdownActionBar, the server is stopping. This method shouldn't be called at this time.");
            return this;
        }
        activeCountdowns.add(this);
        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, 2);
        return this;
    }


    public void stop(boolean callback) {
        activeCountdowns.remove(this);
        this.cancel();
        if (callback) {
            if (this.callback != null) this.callback.run();
        }
    }

    public String secondsRemaining() {
        return String.format("%.0f", seconds);
    }

    @Override
    public void run() {
        Component msg = Util.color(String.format(message, secondsRemaining()));
        Executors.runSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(msg);
            }
        });
        seconds -= 0.1f;

        // when done:
        if (seconds <= 0) {
            this.cancel();
            if (this.callback != null) this.callback.run();
        }
    }

    public static void stopAll(boolean callback) {
        activeCountdowns.forEach(countdown -> countdown.stop(callback));
    }



}
