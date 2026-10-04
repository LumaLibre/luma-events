package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.glowapi.model.GlowColorManager;

import java.util.concurrent.CompletableFuture;

public class Spectator extends Manor2Role {


    protected Spectator(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        eventPlayer.operatePlayer(player -> {
            GlowColorManager.getInstance().update(player);
            manor2.getPacketGlow().removeTarget(player);
            manor2.getPacketGlow().addViewer(player);
        });
    }

    @Override
    public void tick() {

    }

    @Override
    public void die() {

    }

    @Override
    public CompletableFuture<Void> initiatePlay() {
        return null;
    }
}
