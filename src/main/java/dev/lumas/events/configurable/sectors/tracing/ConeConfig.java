package dev.lumas.events.configurable.sectors.tracing;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConeConfig extends OkaeriConfig {

    private double range = 7.5;

    private double traceStep = 0.5;

    private double coneDegrees = 30;
}
