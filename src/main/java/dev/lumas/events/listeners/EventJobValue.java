package dev.lumas.events.listeners;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobValue {

    ALCHEMIST(9),
    BLACKSMITH(10),
    BUILDER(17),
    COOK(11),
    DIGGER(8),
    FARMER(9),
    FISHERMAN(364),
    HUNTER(6),
    LUMBERJACK(10),
    MINER(9);


    private final int bound = 90_000; // 125k p1
    private final int chance;

}
