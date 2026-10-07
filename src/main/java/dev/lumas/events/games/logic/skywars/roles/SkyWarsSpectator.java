package dev.lumas.events.games.logic.skywars.roles;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkyWarsSpectator extends SkyWarsRole {

    private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 140, 0, false, false, true);

    private volatile boolean returning = false;

    protected SkyWarsSpectator(SkyWars skyWars, EventPlayer eventPlayer, SkyWarsTeam team) {
        super(skyWars, eventPlayer, team);
    }

    @Override
    public void play() {
        Player self = this.eventPlayer.getPlayer();
        if (self == null) return;

        for (EventPlayer other : ctx.getParticipants()) {
            if (!other.getUuid().equals(this.getUuid())) {
                other.operatePlayer(viewer -> viewer.hidePlayer(EventMain.getInstance(), self));
            }
        }
        this.operatePlayer(player -> {
            player.getInventory().clear();
            player.addPotionEffect(INVISIBILITY);
            player.setAllowFlight(true);
            player.setFlying(true);
            if (!ctx.getArena().contains(player.getLocation())) {
                returnToArena(player);
            }
        });
    }

    @Override
    public CompletableFuture<Void> cleanup() {
        List<CompletableFuture<Void>> cleanups = new ArrayList<>();
        cleanups.add(this.operatePlayer(player -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setFlying(false);
            player.setAllowFlight(false);
        }));

        Player self = this.eventPlayer.getPlayer();
        if (self != null) {
            for (EventPlayer other : ctx.getParticipants()) {
                if (!other.getUuid().equals(this.getUuid())) {
                    cleanups.add(other.operatePlayer(viewer -> viewer.showPlayer(EventMain.getInstance(), self)));
                }
            }
        }
        return CompletableFuture.allOf(cleanups.toArray(CompletableFuture[]::new));
    }

    @Override
    public void tick() {
        this.operatePlayer(player -> {
            if (!ctx.getArena().contains(player.getLocation())) {
                returnToArena(player);
            }
            player.addPotionEffect(INVISIBILITY);
        });
    }

    @Override
    public boolean attack(SkyWarsRole victim) {
        return false;
    }

    @Override
    public void die(PlayerDeathEvent event, @Nullable SkyWarsRole killer) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        normalize(player);
        returnToArena(player);
    }

    private void returnToArena(Player player) {
        if (returning || ctx.isStopping()) return;
        returning = true;
        player.setFallDistance(0f);
        Location location = player.getLocation();
        if (location.getY() < ctx.getArena().getMinY()) {
            location = aboveVoid(location);
        }
        this.teleportAsync(clampToArena(location)).whenComplete((success, throwable) -> returning = false);
    }

    private Location aboveVoid(Location location) {
        World world = location.getWorld();
        int highestY = world.getHighestBlockYAt(location);
        Location up = location.clone();
        up.setY(highestY >= world.getMinHeight() ? highestY + 2 : team.spawn().getY());
        return up;
    }

    private Location clampToArena(Location location) {
        WorldTiedBoundingBox arena = ctx.getArena();
        return new Location(arena.getWorld(),
            Math.clamp(location.getX(), arena.getMinX() + 1, arena.getMaxX() - 1),
            Math.clamp(location.getY(), arena.getMinY() + 1, arena.getMaxY() - 1),
            Math.clamp(location.getZ(), arena.getMinZ() + 1, arena.getMaxZ() - 1),
            location.getYaw(), location.getPitch());
    }
}
