package dev.jsinco.luma.lumaevents.games.obj;

import dev.lumas.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CountdownBossBar extends BukkitRunnable {

    public static final ConcurrentLinkedQueue<CountdownBossBar> activeCountdowns = new ConcurrentLinkedQueue<>();

    @Getter
    private final BossBar bossBar;
    private final String title;
    private final Runnable callback;

    private float seconds;
    @Getter
    private float secondsRemaining;

    private Audience audience;


    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience, Runnable callback) {
        this.bossBar = BossBar.bossBar(Util.color(title), 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = seconds;
        this.secondsRemaining = seconds;
        this.callback = callback;
        this.audience = audience;
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Runnable callback) {
        this.bossBar = BossBar.bossBar(Util.color(title), 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = seconds;
        this.secondsRemaining = seconds;
        this.callback = callback;
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience) {
        this(title, barColor, seconds, audience, null);
    }


    public CountdownBossBar start() {
        if (EventMain.STOPPING) {
            if (this.callback != null) this.callback.run();
            Logging.errorLog("Cannot start CountdownBossBar, the server is stopping. This method shouldn't be called at this time.");
            return this;
        }
        bossBar.addViewer(audience);
        activeCountdowns.add(this);
        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, 2);
        return this;
    }


    public void stop(boolean callback) {
        activeCountdowns.remove(this);
        bossBar.removeViewer(audience);
        this.cancel();
        if (callback) {
            if (this.callback != null) this.callback.run();
        }
    }

    public String secondsRemaining() {
        return String.format("%.0f", secondsRemaining);
    }

    public void addSeconds(float seconds) {
        this.seconds += seconds;
        this.secondsRemaining += seconds;
    }

    @Override
    public void run() {
        float newProgress = secondsRemaining / seconds;
        if (newProgress  < 1.0 && newProgress > 0.0) {
            bossBar.progress(newProgress);
        }

        //String.format("%.1f", secondsRemaining)
        bossBar.name(Util.color(String.format(title, secondsRemaining())));
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


        public CountdownBossBar build() {
            return new CountdownBossBar(title, color, seconds, audience, callback);
        }
    }
}
