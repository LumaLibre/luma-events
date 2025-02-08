package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

// TODO:
//  Right now, bossbars will only show to players which have been on the server when
//  the countdown bosbar was created. Should probably fix
public class CountdownBossBar extends BukkitRunnable {


    private final BossBar bossBar;
    private final String title;
    private final float seconds;
    private final Runnable callback;
    private float secondsRemaining;
    private Audience audience;


    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience, Runnable callback) {
        this.bossBar = BossBar.bossBar(Util.color(title), 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = seconds;
        this.secondsRemaining = seconds;
        this.callback = callback;
        this.audience = audience;
        bossBar.addViewer(audience);
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience) {
        this(title, barColor, seconds, audience, null);
    }


    public void start() {
        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, 2);
    }


    public void stop(boolean callback) {
        bossBar.removeViewer(audience);
        this.cancel();
        if (callback) {
            if (this.callback != null) this.callback.run();
        }
    }


    @Override
    public void run() {
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
            this.seconds = (float) miliseconds / 1000;
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
