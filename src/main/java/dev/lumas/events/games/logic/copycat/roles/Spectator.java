package dev.lumas.events.games.logic.copycat.roles;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.copycat.Copycat;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Spectator extends CopycatRole {

    private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 140, 0, false, false, true);

    private final String title;
    private final String subtitle;
    private volatile boolean returning;

    public Spectator(Copycat ctx, EventPlayer eventPlayer) {
        this(ctx, eventPlayer, "<red><b>Eliminated", "<gray>You are now spectating.");
    }

    protected Spectator(Copycat ctx, EventPlayer eventPlayer, String title, String subtitle) {
        super(ctx, eventPlayer);
        this.title = title;
        this.subtitle = subtitle;
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
            player.showTitle(Util.title(title, subtitle));
        });
    }

    @Override
    public void tick() {
        this.operatePlayer(player -> {
            player.addPotionEffect(INVISIBILITY);
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (!ctx.getArena().contains(player.getLocation())) {
                returnToArena(player);
            }
        });
    }

    @Override
    public CompletableFuture<Void> cleanup() {
        CopycatRole replacement = ctx.getRoleMap().get(getUuid());
        boolean swapped = replacement != null && replacement != this;
        List<CompletableFuture<Void>> cleanups = new ArrayList<>();
        cleanups.add(this.operatePlayer(player -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            if (!swapped) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
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

    private void returnToArena(Player player) {
        if (returning || ctx.isStopping()) return;
        returning = true;
        player.setFallDistance(0f);
        this.teleportAsync(ctx.getArena().clamp(player.getLocation())).whenComplete((success, throwable) -> returning = false);
    }
}
