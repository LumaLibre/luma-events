package dev.lumas.events.utility;

import dev.lumas.events.EventMain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class JoinTrace {

    private static volatile boolean verbose = false;
    private static final Set<String> reportedSites = ConcurrentHashMap.newKeySet();

    private JoinTrace() {}

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setVerbose(boolean value) {
        verbose = value;
    }

    private static String where(@Nullable Location location) {
        if (location == null) return "world=<null>";
        return String.format("world=%s xyz=%.2f/%.2f/%.2f chunk=%d,%d",
                location.getWorld() == null ? "<null>" : location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static String actual(@Nullable Player player) {
        if (player == null) return "player=<offline>";
        if (!Bukkit.isOwnedByCurrentRegion(player)) return "player=<not-owned-here>";
        return where(player.getLocation());
    }

    private static String ctx(@Nullable Entity entity) {
        return String.format("thread=%s globalTick=%s ownsEntity=%s",
                Thread.currentThread().getName(),
                Bukkit.isGlobalTickThread(),
                entity == null ? "n/a" : Bukkit.isOwnedByCurrentRegion(entity));
    }

    private static void log(String line) {
        EventMain.getInstance().getLogger().info("[TRACE " + Instant.now() + "] " + line);
    }

    public static void joinStart(Player player, @Nullable Location destination) {
        if (!verbose) return;
        log(String.format("EVENT_JOIN_START name=%s uuid=%s src[%s] dst[%s] %s",
                player.getName(), player.getUniqueId(), actual(player), where(destination), ctx(player)));
    }

    public static void teleportRequest(UUID uuid, String api, @Nullable Player player) {
        if (!verbose) return;
        log(String.format("TELEPORT_REQUEST uuid=%s api=%s %s", uuid, api, ctx(player)));
    }

    public static void teleportComplete(UUID uuid, @Nullable Boolean success, @Nullable Throwable throwable) {
        if (!verbose && throwable == null) return;
        Player fresh = Bukkit.getPlayer(uuid);
        String line = String.format("TELEPORT_COMPLETE uuid=%s success=%s ex=%s actual[%s] %s",
                uuid, success, throwable == null ? "none" : throwable.toString(), actual(fresh), ctx(fresh));
        if (throwable != null) {
            EventMain.getInstance().getLogger().warning("[TRACE " + Instant.now() + "] " + line);
        } else {
            log(line);
        }
    }

    public static void joinComplete(UUID uuid) {
        if (!verbose) return;
        Player fresh = Bukkit.getPlayer(uuid);
        log(String.format("EVENT_JOIN_COMPLETE uuid=%s actual[%s] %s", uuid, actual(fresh), ctx(fresh)));
    }

    public static void quit(UUID uuid, String phase) {
        if (!verbose) return;
        log(String.format("PLAYER_QUIT uuid=%s phase=%s %s", uuid, phase, ctx(null)));
    }

    public static void worldChange(Player player, String from, String to) {
        if (!verbose) return;
        log(String.format("WORLD_CHANGE name=%s uuid=%s from=%s to=%s %s",
                player.getName(), player.getUniqueId(), from, to, ctx(player)));
    }

    public static void schedulerHop(String site, @Nullable Entity entity) {
        if (!verbose) return;
        log(String.format("SCHEDULER_HOP site=%s %s", site, ctx(entity)));
    }

    public static void offRegionTeleport(Entity entity, String site) {
        String line = "OFF_REGION_TELEPORT site=" + site
                + " entity=" + entity.getUniqueId()
                + " type=" + entity.getType()
                + " " + ctx(entity);
        if (reportedSites.add(site)) {
            EventMain.getInstance().getLogger().warning("[TRACE " + Instant.now() + "] " + line
                    + " (rerouted onto the entity scheduler; stack trace follows, logged once per site)");
            new Throwable("off-region teleport requested at " + site).printStackTrace();
        } else {
            EventMain.getInstance().getLogger().warning("[TRACE " + Instant.now() + "] " + line + " (rerouted)");
        }
    }

    public static void resetReportedSites() {
        reportedSites.clear();
    }
}
