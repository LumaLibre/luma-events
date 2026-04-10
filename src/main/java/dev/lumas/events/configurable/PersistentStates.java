package dev.lumas.events.configurable;

import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.manager.EventTeamManager;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersistentStates extends OkaeriConfig {

    private long lastGameLaunchTime = System.currentTimeMillis();

    private MinigameConstant lastMinigame = MinigameConstant.BOATRACE2;

    private EventTeamManager.Provider lastChosenTeam = EventTeamManager.Provider.SCARLET;
}
