package dev.lumas.events.utility;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.model.EventPlayer;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Executors {

    private static final EventMain instance = EventMain.getInstance();

    public static ScheduledTask runRepeatingAsync(TimeUnit timeUnit, long delay, long period, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(instance, consumer, delay, period, timeUnit);
    }

    public static ScheduledTask runDelayedAsync(TimeUnit timeUnit, long delay, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runDelayed(instance, consumer, delay, timeUnit);
    }

    public static ScheduledTask runRepeatingAsync(TimeUnit timeUnit, long period, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(instance, consumer, 0, period, timeUnit);
    }

    public static ScheduledTask runAsync(Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runNow(instance, consumer);
    }

    public static ScheduledTask runAsync(Runnable runnable) {
        return Bukkit.getAsyncScheduler().runNow(instance, t -> runnable.run());
    }

    // TODO: Log if entity was retired
    // Synchronous

    @Nullable
    public static ScheduledTask delayedSync(Entity entity, long delay, Runnable runnable) {
        return entity.getScheduler().runDelayed(instance, t -> runnable.run(), null, delay);
    }

    @Nullable
    public static ScheduledTask delayedSync(EventPlayer eventPlayer, long delay, Runnable runnable) {
        Player player = eventPlayer.getPlayer();
        if (player == null) return null;
        return delayedSync(player, delay, runnable);
    }

    public static ScheduledTask delayedSync(Location location, long delay, Runnable runnable) {
        return Bukkit.getRegionScheduler().runDelayed(instance, location, t -> runnable.run(), delay);
    }

    public static ScheduledTask repeatingSync(Entity entity, long period, Consumer<ScheduledTask> consumer) {
        return entity.getScheduler().runAtFixedRate(instance, consumer, null,1, period);
    }

    public static ScheduledTask repeatingSync(Location location, long period, Consumer<ScheduledTask> consumer) {
        return Bukkit.getRegionScheduler().runAtFixedRate(instance, location, consumer, 1, period);
    }

    public static ScheduledTask sync(Entity entity, Runnable runnable) {
        return entity.getScheduler().run(instance, t -> runnable.run(), null);
    }

    public static ScheduledTask sync(Location location, Runnable runnable) {
        return Bukkit.getRegionScheduler().run(instance, location, t -> runnable.run());
    }

    public static ScheduledTask sync(World world, int chunkX, int chunkZ, Runnable runnable) {
        return Bukkit.getRegionScheduler().run(instance, world, chunkX, chunkZ, t -> runnable.run());
    }

    public static ScheduledTask global(Runnable runnable) {
        return Bukkit.getGlobalRegionScheduler().run(instance, t -> runnable.run());
    }

    public static ScheduledTask delayedGlobal(long delay, Runnable runnable) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(instance, t -> runnable.run(), delay);
    }

    public static ScheduledTask repeatingGlobal(long period, Consumer<ScheduledTask> consumer) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(instance, consumer, 1, period);
    }

    public static void runSync(Entity entity, Runnable runnable) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            runnable.run();
        } else {
            sync(entity, runnable);
        }
    }

    public static void runSync(EventPlayer eventPlayer, Runnable runnable) {
        Entity entity = eventPlayer.getPlayer();
        if (entity != null) {
            runSync(entity, runnable);
        }
    }

    public static void runSync(MinigameRole role, Runnable runnable) {
        Entity entity = role.getEventPlayer().getPlayer();
        if (entity != null) {
            runSync(entity, runnable);
        }
    }

    public static void runSync(Location location, Runnable runnable) {
        if (Bukkit.isOwnedByCurrentRegion(location)) {
            runnable.run();
        } else {
            sync(location, runnable);
        }
    }

    public static void runSync(World world, int chunkX, int chunkZ, Runnable runnable) {
        if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            runnable.run();
        } else {
            sync(world, chunkX, chunkZ, runnable);
        }
    }

    public static void runGlobal(Runnable runnable) {
        if (Bukkit.isGlobalTickThread()) {
            runnable.run();
        } else {
            global(runnable);
        }
    }
}
