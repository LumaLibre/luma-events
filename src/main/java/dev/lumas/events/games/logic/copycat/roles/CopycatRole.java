package dev.lumas.events.games.logic.copycat.roles;

import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.logic.copycat.Copycat;
import dev.lumas.events.model.EventPlayer;

import java.util.concurrent.CompletableFuture;

public abstract class CopycatRole extends MinigameRole {

    protected final Copycat ctx;

    protected CopycatRole(Copycat ctx, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.ctx = ctx;
    }

    protected boolean isCurrent() {
        return ctx.isActive() && !ctx.isStopping() && ctx.getRoleMap().get(this.getUuid()) == this;
    }

    public abstract void play();

    public abstract void tick();

    public abstract CompletableFuture<Void> cleanup();
}
