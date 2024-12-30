package dev.jsinco.luma.lumaevents;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobConstants {

    ALCHEMIST(50000, 10),
    BLACKSMITH(50000, 10),
    BUILDER(50000, 9),
    COOK(50000, 10),
    DIGGER(50000, 8),
    FARMER(50000, 8),
    FISHERMAN(50000, 324),
    HUNTER(50000, 20),
    LUMBERJACK(50000, 8),
    MINER(50000, 8);


    private final int bound;
    private final int chance;

}
