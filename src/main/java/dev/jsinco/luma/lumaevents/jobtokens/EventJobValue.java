package dev.jsinco.luma.lumaevents.jobtokens;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobValue {

    ALCHEMIST(10),
    BLACKSMITH(15),
    BUILDER(17),
    COOK(15),
    DIGGER( 8),
    FARMER(9),
    FISHERMAN(364),
    HUNTER(17),
    LUMBERJACK(10),
    MINER(9);


    private final int bound = 30_000; //90_000; inflated tokens
    private final int chance;

}
