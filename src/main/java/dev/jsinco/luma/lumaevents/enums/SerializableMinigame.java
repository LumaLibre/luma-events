package dev.jsinco.luma.lumaevents.enums;

import dev.jsinco.luma.lumaevents.games.logic.BoatRace;
import dev.jsinco.luma.lumaevents.games.logic.Envoys;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.Paintball;
import lombok.Getter;

@Getter
public enum SerializableMinigame {
    PAINTBALL(Paintball.class),
    ENVOYS(Envoys.class),
    BOATRACE(BoatRace.class);

    private final Class<? extends Minigame> minigame;

    SerializableMinigame(Class<? extends Minigame> minigame) {
        this.minigame = minigame;
    }
}
