package dev.lumas.events.utility;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AsynchronousRunnable implements Consumer<ScheduledTask> {

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

    public void runNowAsync() {
        this.task = Executors.runAsync(this);
    }

    public void delayedAsync(long delay) {
        this.task = Executors.runDelayedAsync(TimeUnit.MILLISECONDS, delay * 50, this);
    }

    public void repeatingAsync(long initialDelay, long period) {
        this.task = Executors.runRepeatingAsync(TimeUnit.MILLISECONDS, initialDelay * 50, period * 50, this);
    }

}
