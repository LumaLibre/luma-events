package dev.lumas.events.games.logic.skywars.roles;

import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam;
import dev.lumas.events.model.EventPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ActivePlayer extends SkyWarsRole {

    public ActivePlayer(SkyWars skyWars, EventPlayer eventPlayer, SkyWarsTeam team) {
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
            });
        });
    }

    @Override
    public CompletableFuture<Void> cleanup() {
        return this.operatePlayer(player -> {
            if (SkyWarsTeam.isTeamHelmet(player.getInventory().getHelmet())) {
                player.getInventory().setHelmet(null);
            }
        });
    }

    @Override
    public void tick() {

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
        Player player = event.getPlayer();
        event.setCancelled(true);
        player.getWorld().strikeLightningEffect(player.getLocation());
        for (ItemStack drop : event.getDrops()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        event.getDrops().clear();
        player.getInventory().clear();
        normalize(player);

        SkyWarsRole spectator = ctx.getRoleMap().swapRole(this, () -> new Spectator(ctx, this.eventPlayer, this.team));
        spectator.play();
        Component deathMessage = event.deathMessage();
        ctx.sendAudienceMessage(deathMessage);
        event.deathMessage(null);
        ctx.checkForWinner();
    }
}
