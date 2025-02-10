package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.obj.EventTeamType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Config extends OkaeriConfig {

    @Comment("Don't touch me")
    private EventTeamType lastChosenTeam = EventTeamType.ROSETHORN;

    @Comment("Paintball")
    private MinigameDefinition paintball = new MinigameDefinition();
    @Comment("Envoys")
    private MinigameDefinition envoys = new MinigameDefinition();
    @Comment("BoatRace")
    private BoatRaceDefinition boatRace = new BoatRaceDefinition();
}
