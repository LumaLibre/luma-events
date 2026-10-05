package dev.lumas.events.configurable.sectors.tracing;

import lombok.Getter;
import lombok.Setter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@Setter
@ConfigSerializable
public class ConeConfig {

    private double range = 7.5;

    private double traceStep = 0.5;

    private double coneDegrees = 30;
}
