package dev.lumas.events.configurable;

import dev.lumas.events.configurable.sectors.BoatRace2Definition;
import dev.lumas.events.configurable.sectors.ManorMinigameDefinition;
import dev.lumas.events.configurable.sectors.MineBattleDefinition;
import dev.lumas.events.configurable.sectors.MinigameDefinition;
import dev.lumas.events.configurable.sectors.Paintball2_1Definition;
import dev.lumas.events.configurable.sectors.PanelPartyMinigameDefinition;
import dev.lumas.events.configurable.sectors.TNTRunDefinition;
import dev.lumas.events.configurable.sectors.TheNabbitsMinigameDefinition;
import dev.lumas.events.configurable.sectors.TowersDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
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
            MinigameConstant.PANEL_PARTY,
            MinigameConstant.TNTRUN,
            MinigameConstant.MINEBATTLE
    );

    @Comment("Commands allowed while participating in a game")
    private List<String> commandWhitelist = List.of(
            "g", "l", "pc", "sc", "msg", "r", "tell", "partychat", "staffchat"
    );


    // valentines 2026

    @Comment("Minigame definition for 'Panel Party'")
    private Map<String, PanelPartyMinigameDefinition> panelPartyMaps = Map.of(
            "default", new PanelPartyMinigameDefinition()
    );

    @Comment("Minigame definition for 'TNTRun'")
    private Map<String, TNTRunDefinition> tntRunMaps = Map.of(
            "default", new TNTRunDefinition()
    );

    @Comment("Minigame definition for 'MineBattle'")
    private Map<String, MineBattleDefinition> mineBattleMaps = Map.of(
            "default", new MineBattleDefinition()
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

    @Comment("Minigame definition for 'The Nabbits'")
    private Map<String, TheNabbitsMinigameDefinition> theNabbitsMaps = Map.of();

}

