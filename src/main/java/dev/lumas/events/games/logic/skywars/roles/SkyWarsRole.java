package dev.lumas.events.games.logic.skywars.roles;

import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam;
import dev.lumas.events.model.EventPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class SkyWarsRole extends MinigameRole {

    protected final SkyWars ctx;
    @Getter
    protected final SkyWarsTeam team;

    protected SkyWarsRole(SkyWars ctx, EventPlayer eventPlayer, SkyWarsTeam team) {
        super(eventPlayer);
        this.ctx = ctx;
        this.team = team;
    }

    public boolean isTeammate(SkyWarsRole other) {
        return team.equals(other.team);
    }

    protected boolean isCurrent() {
        return ctx.isActive() && !ctx.isStopping() && ctx.getRoleMap().get(this.getUuid()) == this;
    }

    protected void releaseBorder(Player player) {
        if (ctx.isStopping() || !ctx.getRoleMap().containsKey(this.getUuid())) {
            player.setWorldBorder(null);
        }
    }

    protected static void normalize(Player player) {
        player.setHealth(20.0);
        player.setFallDistance(0f);
        player.setFireTicks(0);
    }

    public abstract void play();
    public abstract void tick();
    public abstract boolean attack(SkyWarsRole victim);
    public abstract void die(PlayerDeathEvent event, @Nullable SkyWarsRole killer);
    public abstract CompletableFuture<Void> cleanup();
}
