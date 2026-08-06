package dev.lumas.events.listeners;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventJobValue {

    ALCHEMIST(6),
    BLACKSMITH(12),
    BUILDER(13),
    COOK(13),
    DIGGER( 8),
    FARMER(9),
    FISHERMAN(295),
    HUNTER(3),
    LUMBERJACK(8),
    MINER(8);


    private final int bound = 120_000;
    private final int chance;

}
