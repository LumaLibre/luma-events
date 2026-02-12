package dev.lumas.events.configurable;

import dev.lumas.events.games.constants.MinigameConstant;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinigameState extends OkaeriConfig {

    private long lastGameLaunchTime = System.currentTimeMillis();

    private MinigameConstant lastMinigame = MinigameConstant.BOATRACE2;
}
