package dev.jsinco.luma.lumaevents.utility;

import dev.jsinco.luma.lumaevents.EventMain;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Executors {

    private static final EventMain instance = EventMain.getInstance();

    public static ScheduledTask runRepeatingAsync(long delay, long period, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(instance, consumer, delay, period, timeUnit);
    }

    public static ScheduledTask runDelayedAsync(long delay, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runDelayed(instance, consumer, delay, timeUnit);
    }

    public static ScheduledTask runRepeatingAsync(long period, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(instance, consumer, 0, period, timeUnit);
    }

    public static ScheduledTask runAsync(Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runNow(instance, consumer);
    }

    public static BukkitTask runAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(instance, runnable);
    }


    // Synchronous

    public static BukkitTask delayedSync(long delay, Runnable runnable) {
        return Bukkit.getScheduler().runTaskLater(instance, runnable, delay);
    }

    public static BukkitTask repeatingSync(long period, Runnable runnable) {
        return Bukkit.getScheduler().runTaskTimer(instance, runnable, 0, period);
    }

    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(instance, runnable);
    }

    public static void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            sync(runnable);
        }
    }

}
