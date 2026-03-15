package dev.lumas.events.utility.scheduler;

import dev.lumas.events.EventMain;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.function.Consumer;

public abstract class RegionRunnable implements Consumer<ScheduledTask> {

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

    public void now(Location location) {
        this.task = Bukkit.getRegionScheduler().run(EventMain.getInstance(), location, this);
    }

    public void now(Entity entity) {
        this.task = entity.getScheduler().run(EventMain.getInstance(), this, null);
    }

    public void delayed(long delay, Location location) {
        this.task = Bukkit.getRegionScheduler().runDelayed(EventMain.getInstance(), location, this, delay);
    }

    public void delayed(long delay, Entity entity) {
        this.task = entity.getScheduler().runDelayed(EventMain.getInstance(), this, null, delay);
    }

    public void repeating(long initialDelay, long period, Location location) {
        this.task = Bukkit.getRegionScheduler().runAtFixedRate(EventMain.getInstance(), location, this, 1, period);
    }

    public void repeating(long initialDelay, long period, Entity entity) {
        this.task = entity.getScheduler().runAtFixedRate(EventMain.getInstance(), this, null, 1, period);
    }

}
