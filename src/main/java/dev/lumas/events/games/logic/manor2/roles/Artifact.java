package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.logic.manor2.artifact.ArtifactRegion;
import dev.lumas.events.model.EventPlayer;

public class Artifact extends Manor2Role {
    private final ArtifactRegion region;

    protected Artifact(EventPlayer eventPlayer, ArtifactRegion region) {
        super(eventPlayer);
        this.region = region;
    }

    @Override
    public void tick() {

    }

    @Override
    public void die() {

    }

    @Override
    public void initiatePlay() {

    }
}
