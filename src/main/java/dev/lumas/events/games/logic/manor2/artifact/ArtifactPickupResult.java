package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.games.logic.manor2.roles.Artifact;

public interface ArtifactPickupResult {


    record NotFound() implements ArtifactPickupResult {

    }

    record FoundNormalArtifact() implements ArtifactPickupResult {

    }

    record FoundPlayerArtifact(Artifact playerArtifact) implements ArtifactPickupResult {

    }
}
