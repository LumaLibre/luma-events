package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRace2Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.ManorMinigameDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.TowersDefinition;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Config extends OkaeriConfig {

    @Comment("Enable or disable automatic minigames")
    private boolean automaticMinigames = false;

    @Comment("Enable or disable job token payouts")
    private boolean jobTokenPayouts = false;

    @Comment("Automatic minigame cooldown in milliseconds")
    private long automaticMinigameCooldown = 7200000L;

    @Comment("Token multiplier")
    private double tokenMultiplier = 1.0;

    @Comment("Location for /event")
    private Location eventSpawnLocation = null;

    @Comment("Game drop-off location for minigames")
    private Location gameDropOffLocation = null;

    @Comment("Enabled minigames for automatic selection")
    private List<MinigameConstant> enabledAutomaticMinigames = List.of(
            MinigameConstant.BOATRACE2,
            MinigameConstant.TOWERS,
            MinigameConstant.PROP_HUNT
    );

    @Comment("Minigame definition for 'Boatrace 2'")
    private Map<String, BoatRace2Definition> boatRaceMaps = Map.of(
            "default", new BoatRace2Definition()
    );

    @Comment("Minigame definition for 'Towers'")
    private Map<String, TowersDefinition> towersMaps = Map.of(
            "default", new TowersDefinition()
    );

    @Comment("Minigame definition for 'Prop Hunt'")
    private Map<String, ManorMinigameDefinition> propHuntMaps = Map.of(
            "default", new ManorMinigameDefinition()
    );

    @Comment("Minigame definition for 'Paintball 2.1'")
    private Map<String, Paintball2_1Definition> paintballMaps = Map.of(
            "default", new Paintball2_1Definition()
    );



    @Comment("Minigame definition for 'TNT Tag'")
    private Map<String, MinigameDefinition> tntTagMaps = Map.of();


    @Comment("Minigame definition for 'Manor'")
    private Map<String, ManorMinigameDefinition> manorMaps = Map.of();

}

