package dev.jsinco.luma.lumaevents.jobtokens;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobValue {

    ALCHEMIST(60000, 10),
    BLACKSMITH(60000, 10),
    BUILDER(60000, 9),
    COOK(60000, 10),
    DIGGER(60000, 8),
    FARMER(60000, 8),
    FISHERMAN(60000, 324),
    HUNTER(60000, 20),
    LUMBERJACK(60000, 8),
    MINER(60000, 8);


    private final int bound;
    private final int chance;

}
