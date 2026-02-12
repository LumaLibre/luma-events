package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MicrogameMap extends OkaeriConfig {

    private String structureName;
    private int padding;

}
