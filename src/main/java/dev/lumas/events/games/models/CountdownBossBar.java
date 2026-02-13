package dev.lumas.events.games.models;

import dev.lumas.events.EventMain;
import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.utility.Logging;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final boolean countUp;
    
    private final Audience audience;
    private final Set<Audience> extraViewers = ConcurrentHashMap.newKeySet();

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience, Runnable callback, boolean countUp) {
        this.bossBar = BossBar.bossBar(Util.color(title), countUp ? 0.0f : 1.0f, barColor, BossBar.Overlay.NOTCHED_12);
        this.title = title;
        this.seconds = Math.max(0.001f, seconds);
        this.countUp = countUp;
        this.callback = callback;
        this.audience = audience;
        this.secondsRemaining = countUp ? 0.0f : this.seconds;
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience, Runnable callback) {
        this(title, barColor, seconds, audience, callback, false);
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Runnable callback) {
        this(title, barColor, seconds, null, callback, false);
    }

    public CountdownBossBar(String title, BossBar.Color barColor, float seconds, Audience audience) {
        this(title, barColor, seconds, audience, null, false);
    }


    public CountdownBossBar start() {
        if (EventMain.STOPPING) {
            if (this.callback != null) this.callback.run();
            Logging.errorLog("Cannot start CountdownBossBar, the server is stopping. This method shouldn't be called at this time.");
            return this;
        }

        if (audience != null) bossBar.addViewer(audience);
        applyState();

        activeCountdowns.add(this);
        this.runTaskTimerAsynchronously(EventMain.getInstance(), 0, 2);
        return this;
    }


    public void stop(boolean callback) {
        activeCountdowns.remove(this);

        if (audience != null) bossBar.removeViewer(audience);
        for (Audience a : extraViewers) bossBar.removeViewer(a);
        extraViewers.clear();

        this.cancel();
        if (callback && this.callback != null) this.callback.run();
    }

    public void addViewer(Audience newViewer) {
        if (newViewer == null) return;
        bossBar.addViewer(newViewer);
        extraViewers.add(newViewer);
        applyState();
    }
    public void addViewer(Player player) {
        if (player == null) return;
        addViewer((Audience) player);
    }

    public String secondsRemaining() {
        return String.format("%.0f", secondsRemaining);
    }

    /**
     * Adds seconds to the total duration
     * Countdown: it also increases the remaining time
     * Countup: keeps elapsed as-is, just extends the cap
     */
    public void addSeconds(float seconds) {
        if (seconds <= 0) return;
        this.seconds += seconds;
        if (!countUp) this.secondsRemaining += seconds;
    }

    @Override
    public void run() {
        if (countUp) {
            secondsRemaining += 0.1f;
        } else {
            secondsRemaining -= 0.1f;
        }
        applyState();

        boolean finished = countUp ? (secondsRemaining >= seconds) : (secondsRemaining <= 0);
        if (finished) {
            secondsRemaining = countUp ? seconds : 0.0f;
            applyState();

            if (audience != null) bossBar.removeViewer(audience);
            for (Audience a : extraViewers) bossBar.removeViewer(a);
            extraViewers.clear();

            this.cancel();
            if (this.callback != null) this.callback.run();
        }
    }

    private void applyState() {
        float progress;
        progress = secondsRemaining / seconds;
        if (progress < 0.0f) progress = 0.0f;
        if (progress > 1.0f) progress = 1.0f;
        bossBar.progress(progress);
        float display = secondsRemaining;
        bossBar.name(Util.color(String.format(title, String.format("%.0f", display))));
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

        private boolean countUp = false;

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

        public Builder countUp(boolean countUp) {
            this.countUp = countUp;
            return this;
        }

        public CountdownBossBar build() {
            return new CountdownBossBar(title, color, seconds, audience, callback, countUp);
        }
    }
}