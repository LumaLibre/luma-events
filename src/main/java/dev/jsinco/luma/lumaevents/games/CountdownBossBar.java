package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: cleanup
public class CountdownBossBar extends BukkitRunnable {

    private static final ConcurrentLinkedQueue<CountdownBossBar> activeCountdowns = new ConcurrentLinkedQueue<>();

    private final BossBar bossBar;
    private final String title;
    private final float seconds;
    private final Runnable callback;
    private final boolean global;

    private float secondsRemaining;
    private Audience audience;


    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience, Runnable callback) {
        this.bossBar = BossBar.bossBar(Util.color(title), 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = seconds;
        this.secondsRemaining = seconds;
        this.callback = callback;
        this.audience = audience;
        this.global = false;
        bossBar.addViewer(audience);
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, boolean global, Runnable callback) {
        this.bossBar = BossBar.bossBar(Util.color(title), 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = seconds;
        this.secondsRemaining = seconds;
        this.callback = callback;
        if (global) this.audience = Audience.audience(Bukkit.getOnlinePlayers());
        this.global = global;
        bossBar.addViewer(audience);
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience) {
        this(title, barColor, seconds, audience, null);
    }


    public void start() {
        activeCountdowns.add(this);
        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, 2);
    }


    public void stop(boolean callback) {
        activeCountdowns.remove(this);
        bossBar.removeViewer(audience);
        this.cancel();
        if (callback) {
            if (this.callback != null) this.callback.run();
        }
    }


    @Override
    public void run() {
        if (global) {
            Audience newAudience = Audience.audience(Bukkit.getOnlinePlayers());
            if (!audience.equals(newAudience)) {
                bossBar.addViewer(newAudience);
                this.audience = newAudience;
            }
        }

        float newProgress = secondsRemaining / seconds;
        if (newProgress  < 1.0 && newProgress > 0.0) {
            bossBar.progress(newProgress);
        }

        bossBar.name(Util.color(String.format(title, String.format("%.1f", secondsRemaining))));
        secondsRemaining -= 0.1f;

        // when done:
        if (secondsRemaining <= 0) {
            bossBar.removeViewer(audience);
            this.cancel();
            if (this.callback != null) this.callback.run();
        }
    }

    public static void stopAll(boolean callback) {
        activeCountdowns.forEach(countdown -> countdown.stop(callback));
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String title;
        private BossBar.Color color;
        private float seconds;
        private Runnable callback = null;
        private Audience audience;
        private boolean global;


        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder color(BossBar.Color color) {
            this.color = color;
            return this;
        }

        public Builder seconds(float seconds) {
            this.seconds = seconds;
            return this;
        }

        public Builder miliseconds(long miliseconds) {
            this.seconds = (float) Util.millisToSecs(miliseconds);
            return this;
        }

        public Builder callback(Runnable callback) {
            this.callback = callback;
            return this;
        }

        public Builder audience(Audience audience) {
            this.audience = audience;
            return this;
        }

        public Builder global(boolean global) {
            this.global = global;
            return this;
        }

        public CountdownBossBar build() {
            if (global) return new CountdownBossBar(title, color, seconds, true, callback);
            return new CountdownBossBar(title, color, seconds, audience, callback);
        }
    }
}
