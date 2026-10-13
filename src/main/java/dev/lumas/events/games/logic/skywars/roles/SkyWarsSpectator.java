package dev.lumas.events.games.logic.skywars.roles;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam;
import dev.lumas.events.model.EventPlayer;
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
            if (ctx.isOutOfBounds(player.getLocation())) {
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
            releaseBorder(player);
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
            if (ctx.isOutOfBounds(player.getLocation())) {
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
        this.teleportAsync(team.spawn()).whenComplete((success, throwable) -> returning = false);
    }
}
