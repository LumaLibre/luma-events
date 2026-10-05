package dev.lumas.events.configurable.sectors.tracing;

import dev.lumas.events.utility.effects.ParticleDefinition;
import dev.lumas.events.utility.effects.SoundDefinition;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@Getter
@ConfigSerializable
public class BeamConfig {

    @Comment("Beam range")
    private double range = 75.0;

    @Comment("Tracing step")
    private double step = 0.5;

    @Comment("Max distance between the beam and the player's hitbox to for a hit")
    private double hitMargin = 0.1;

    @Comment("The particles the beam will show")
    private List<ParticleDefinition> beamParticles = List.of();

    @Comment("The particles the beam will show on hit")
    private List<ParticleDefinition> hitParticles = List.of();

    @Comment("The sounds the beam will play")
    private List<SoundDefinition> beamSounds = List.of();

    @Comment("The sounds the beam will play on hit")
    private List<SoundDefinition> hitSounds = List.of();
}
