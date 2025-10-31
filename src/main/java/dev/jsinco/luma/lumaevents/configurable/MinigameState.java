package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinigameState extends OkaeriConfig {

    private long lastGameLaunchTime = System.currentTimeMillis();

    private MinigameConstant lastMinigame = MinigameConstant.MANOR;
}
