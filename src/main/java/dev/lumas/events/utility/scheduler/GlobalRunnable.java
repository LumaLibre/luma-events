package dev.lumas.events.utility.scheduler;

import dev.lumas.events.EventMain;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.function.Consumer;

public abstract class GlobalRunnable implements Consumer<ScheduledTask> {

    @MonotonicNonNull
    private ScheduledTask task;


    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
        }
    }

    public boolean isCancelled() {
        return this.task != null && this.task.isCancelled();
    }

    public void runNowGlobal() {
        this.task = Bukkit.getGlobalRegionScheduler().run(EventMain.getInstance(), this);
    }

    public void delayedGlobal(long delay) {
        this.task = Bukkit.getGlobalRegionScheduler().runDelayed(EventMain.getInstance(), this, delay);
    }

    public void repeatingGlobal(long initialDelay, long period) {
        this.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(EventMain.getInstance(), this, 1, period);
    }
}
