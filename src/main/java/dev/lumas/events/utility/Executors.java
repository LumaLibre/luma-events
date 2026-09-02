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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Executors {

    private static final EventMain instance = EventMain.getInstance();
    private static final int TELEPORTS_PER_TICK = 5;
    private static final long TELEPORT_CHAIN_TIMEOUT_MILLIS = 10_000L;

    private static final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> PENDING_TELEPORTS = new ConcurrentHashMap<>();

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

    // Synchronous

    @Nullable
    public static ScheduledTask delayedSync(Entity entity, long delay, Runnable runnable) {
        ScheduledTask task = entity.getScheduler().runDelayed(instance, t -> runnable.run(), null, delay);
        if (task == null) {
            instance.getLogger().warning("Dropped a delayed task for retired entity " + entity.getUniqueId());
        }
        return task;
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

    @Nullable
    public static ScheduledTask sync(Entity entity, Runnable runnable) {
        ScheduledTask task = entity.getScheduler().run(instance, t -> runnable.run(), null);
        if (task == null) {
            instance.getLogger().warning("Dropped a task for retired entity " + entity.getUniqueId());
        }
        return task;
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

    public static ScheduledTask repeatingGlobal(long delay, long period, Consumer<ScheduledTask> consumer) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(instance, consumer, delay, period);
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

    public static CompletableFuture<Boolean> teleportSafely(Entity entity, @Nullable Location location, @Nullable String site) {
        if (location == null) {
            return CompletableFuture.completedFuture(false);
        }

        UUID uuid = entity.getUniqueId();
        JoinTrace.teleportRequest(uuid, "Entity#teleportAsync", entity instanceof Player player ? player : null);
        String callSite = site != null ? site : callerSite();

        return serialized(uuid, future -> beginTeleport(entity, uuid, location, callSite, future));
    }

    public static CompletableFuture<Boolean> teleportSafely(Entity entity, @Nullable Location location) {
        return teleportSafely(entity, location, null);
    }

    private static void beginTeleport(Entity entity, UUID uuid, Location location, String site, CompletableFuture<Boolean> future) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            forward(report(uuid, entity.teleportAsync(location)), future);
            return;
        }

        JoinTrace.offRegionTeleport(entity, site);
        scheduleTeleport(entity, uuid, location, 1L, future);
    }

    private static void scheduleTeleport(Entity entity, UUID uuid, Location location, long delay, CompletableFuture<Boolean> future) {
        boolean scheduled = entity.getScheduler().execute(
                instance,
                () -> {
                    Entity target = entity instanceof Player ? Bukkit.getPlayer(uuid) : entity;
                    if (target == null || !target.isValid()) {
                        future.complete(false);
                        return;
                    }
                    forward(report(uuid, target.teleportAsync(location)), future);
                },
                () -> future.complete(false),
                delay
        );

        if (!scheduled) {
            future.complete(false);
        }
    }

    private static CompletableFuture<Boolean> serialized(UUID uuid, Consumer<CompletableFuture<Boolean>> teleport) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture<Boolean> previous = PENDING_TELEPORTS.put(uuid, future);
        future.whenComplete((success, throwable) -> PENDING_TELEPORTS.remove(uuid, future));

        AtomicBoolean started = new AtomicBoolean();
        Runnable start = () -> {
            if (!started.compareAndSet(false, true)) return;
            try {
                teleport.accept(future);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        };

        if (previous == null || previous.isDone()) {
            start.run();
            return future;
        }

        previous.whenComplete((success, throwable) -> start.run());
        runDelayedAsync(TimeUnit.MILLISECONDS, TELEPORT_CHAIN_TIMEOUT_MILLIS, task -> start.run());
        return future;
    }

    private static void forward(CompletableFuture<Boolean> upstream, CompletableFuture<Boolean> downstream) {
        upstream.whenComplete((success, throwable) -> {
            if (throwable != null) downstream.completeExceptionally(throwable);
            else downstream.complete(Boolean.TRUE.equals(success));
        });
    }

    private static final Set<String> WRAPPER_CLASSES = Set.of(
            Executors.class.getName(),
            "dev.lumas.events.model.EventPlayer",
            "dev.lumas.events.games.interfaces.models.MinigameRole"
    );

    private static String callerSite() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .filter(frame -> !WRAPPER_CLASSES.contains(frame.getClassName()))
                        .filter(frame -> frame.getClassName().startsWith("dev.lumas."))
                        .findFirst()
                        .map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1)
                                + "#" + frame.getMethodName() + ":" + frame.getLineNumber()))
                .orElse("unknown");
    }

    private static CompletableFuture<Boolean> report(UUID uuid, CompletableFuture<Boolean> upstream) {
        CompletableFuture<Boolean> downstream = new CompletableFuture<>();
        upstream.whenComplete((success, throwable) -> {
            JoinTrace.teleportComplete(uuid, success, throwable);
            if (throwable != null) downstream.completeExceptionally(throwable);
            else downstream.complete(Boolean.TRUE.equals(success));
        });
        return downstream;
    }

    public static Collection<CompletableFuture<Boolean>> teleportGroupAsync(List<EventPlayer> eventPlayers, Location location) {
        List<Entity> players = new ArrayList<>(eventPlayers.size());
        for (EventPlayer eventPlayer : eventPlayers) {
            Player player = eventPlayer.getPlayer();
            if (player != null) {
                players.add(player);
            }
        }
        return teleportGroupAsync(players, location);
    }

    public static Collection<CompletableFuture<Boolean>> teleportGroupAsync(Collection<Entity> entities, Location location) {
        List<Entity> entityList = new ArrayList<>(entities);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(entityList.size());

        for (int i = 0; i < entityList.size(); i++) {
            Entity entity = entityList.get(i);
            UUID uuid = entity.getUniqueId();
            long delay = Math.max(1L, i / TELEPORTS_PER_TICK);

            futures.add(serialized(uuid, future -> scheduleTeleport(entity, uuid, location, delay, future)));
        }

        return futures;
    }
}
