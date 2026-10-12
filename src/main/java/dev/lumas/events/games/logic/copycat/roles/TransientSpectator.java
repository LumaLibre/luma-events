package dev.lumas.events.games.logic.copycat.roles;

import dev.lumas.events.games.logic.copycat.Copycat;
import lombok.Getter;

public final class TransientSpectator extends Spectator {

    @Getter
    private final Builder builder;

    public TransientSpectator(Copycat ctx, Builder builder) {
        super(ctx, builder.getEventPlayer(), "<green><b>Build submitted", "<gray>Go explore!");
        this.builder = builder;
    }

    public boolean owns(Builder candidate) {
        return this.builder == candidate;
    }
}
