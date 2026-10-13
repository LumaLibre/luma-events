package dev.lumas.events.games.logic.skywars.roles;

import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam;
import dev.lumas.events.model.EventPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SkyWarsPlayer extends SkyWarsRole {

    private static final long BORDER_DAMAGE_INTERVAL = 1000;
    private static final double BORDER_DAMAGE_BASE = 1.0;
    private static final double BORDER_DAMAGE_PER_BLOCK = 0.5;
    private static final double BORDER_DAMAGE_MAX = 8.0;

    private long lastBorderDamage = -1;

    public SkyWarsPlayer(SkyWars skyWars, EventPlayer eventPlayer, SkyWarsTeam team) {
        super(skyWars, eventPlayer, team);
    }

    @Override
    public void play() {
        this.teleportAsync(team.spawn()).thenAccept(success -> {
            if (!success) return;
            this.operatePlayer(player -> {
                if (isCurrent()) {
                    player.getInventory().setHelmet(team.helmet());
                }
                ctx.getBorder().show(player);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setAllowFlight(false);
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
        });
    }

    @Override
    public CompletableFuture<Void> cleanup() {
        return this.operatePlayer(player -> {
            if (SkyWarsTeam.isTeamHelmet(player.getInventory().getHelmet())) {
                player.getInventory().setHelmet(null);
            }
            releaseBorder(player);
        });
    }

    @Override
    public void tick() {
        this.operatePlayer(player -> {
            if (!isCurrent() || player.isDead()) return;
            double outside = ctx.getBorder().distanceOutside(player.getLocation());
            if (outside <= 0) return;
            long now = System.currentTimeMillis();
            if (now - lastBorderDamage < BORDER_DAMAGE_INTERVAL) return;
            lastBorderDamage = now;
            double damage = Math.min(BORDER_DAMAGE_MAX, BORDER_DAMAGE_BASE + BORDER_DAMAGE_PER_BLOCK * outside);
            player.damage(damage, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
        });
    }

    @Override
    public boolean attack(SkyWarsRole victim) {
        if (this.isTeammate(victim)) {
            // TODO
            this.eventPlayer.sendActionBar("That's your teammate.");
            return false; // don't harm teammates
        }
        return true;
    }

    @Override
    public void die(PlayerDeathEvent event, @Nullable SkyWarsRole killer) {
        if (killer != null) {
            ctx.getScoreboard().addScore(killer, 5);
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.getWorld().strikeLightningEffect(player.getLocation());
        for (ItemStack drop : event.getDrops()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        event.getDrops().clear();
        player.getInventory().clear();
        normalize(player);

        SkyWarsRole spectator = ctx.getRoleMap().swapRole(this, () -> new SkyWarsSpectator(ctx, this.eventPlayer, this.team));
        spectator.play();
        Component deathMessage = event.deathMessage();
        ctx.sendAudienceMessage(deathMessage);
        event.deathMessage(null);
        ctx.finish();
    }
}
