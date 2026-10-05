package dev.lumas.events.configurable;

import dev.lumas.events.games.constants.MinigameConstant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalStates {

    private long lastGameLaunchTime = System.currentTimeMillis();

    private MinigameConstant lastMinigame = MinigameConstant.BOATRACE2;

}
