package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TokenBlackListedPlayer extends OkaeriConfig {
    private UUID uuid;
    private int current;
    private int max;
}
