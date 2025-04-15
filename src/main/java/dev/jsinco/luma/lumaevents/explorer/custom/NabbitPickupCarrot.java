package dev.jsinco.luma.lumaevents.explorer.custom;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Called when a player picks up a carrot in the Nabbit minigame.
 */
@Getter
@AllArgsConstructor
public class NabbitPickupCarrot {
    private final int amount;
}
