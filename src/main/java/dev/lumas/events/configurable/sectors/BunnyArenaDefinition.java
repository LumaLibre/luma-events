package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;

@Getter
public class BunnyArenaDefinition extends OkaeriConfig {

    @Comment("Enable or disable the Bunny Arena")
    private boolean bunnyArenaEnabled = true;

    @Comment("Should encapsulate the entire map")
    private Region playRegion = new Region();

    @Comment("Should be an area inside the playRegion where bunnies spawn")
    private Region spawnRegion = new Region();

    private int defaultMaxBunnies = 256;

    private int extraBunniesPerPlayer = 30;
}