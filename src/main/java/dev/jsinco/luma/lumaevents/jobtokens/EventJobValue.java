package dev.jsinco.luma.lumaevents.jobtokens;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobValue {

    ALCHEMIST(50000, 11),
    BLACKSMITH(50000, 11),
    BUILDER(50000, 10),
    COOK(50000, 11),
    DIGGER(50000, 9),
    FARMER(50000, 9),
    FISHERMAN(50000, 364),
    HUNTER(50000, 21),
    LUMBERJACK(50000, 12),
    MINER(50000, 9);


    private final int bound;
    private final int chance;

}
