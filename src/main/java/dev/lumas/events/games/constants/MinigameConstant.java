package dev.lumas.events.games.constants;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.sectors.BombermanDefinition;
import dev.lumas.events.configurable.sectors.MineBattleDefinition;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.logic.BoatRace2;
import dev.lumas.events.games.logic.Bomberman;
import dev.lumas.events.games.logic.FreezeTag;
import dev.lumas.events.games.logic.Incursion;
import dev.lumas.events.games.logic.Manor;
import dev.lumas.events.games.logic.MineBattle;
import dev.lumas.events.games.logic.Paintball2_1;
import dev.lumas.events.games.logic.PanelParty;
import dev.lumas.events.games.logic.PropHunt;
import dev.lumas.events.games.logic.Soccer;
import dev.lumas.events.games.logic.TNTRun;
import dev.lumas.events.games.logic.TNTTag;
import dev.lumas.events.games.logic.TheNabbits;
import dev.lumas.events.games.logic.Towers;
import dev.lumas.events.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
public enum MinigameConstant {

    // This enum should only contain real minigames!
    FREEZE_TAG(FreezeTag::new, "Freeze Tag", "freezetag", "freeze_tag"),
    PAINTBALL2_1(Paintball2_1::new, "Paintball 2.1", "paintball2.1", "paintball2_1"),
    BOATRACE2(BoatRace2::new, "Boat Race 2", "boatrace2", "boatrace"),
    TNTTAG(TNTTag::new, "TNT Tag", "tnttag"),
    TOWERS(Towers::new, "Towers", "towers"),
    MANOR(Manor::new, "Manor", "manor"),
    PROP_HUNT(PropHunt::new, "Prop Hunt", "prophunt", "prop_hunt"),
    THE_NABBITS(TheNabbits::new, "The Nabbits", "thenabbits", "the_nabbits"),
    PANEL_PARTY(PanelParty::new, "Panel Party", "panelparty", "panel_party"),
    MINEBATTLE((config) -> new MineBattle((MineBattleDefinition) config), "MineBattle", "minebattle", "mine_battle"),
    TNTRUN(TNTRun::new, "TNT Run", "tntrun", "tnt_run"),
    SOCCER(Soccer::new, "Soccer", "sulfur_soccer", "soccer"),
    INCURSION(Incursion::new, "Incursion", "incursion"),
    BOMBERMAN((config) -> new Bomberman((BombermanDefinition) config), "Bomberman", "bomberman", "bomber_man")
    ;

    private final MinigameSupplier<?> supplier;
    private final String displayName;
    private final String[] aliases;

    <T extends OkaeriConfig> MinigameConstant(MinigameSupplier<T> supplier, String displayName, String... aliases) {
        this.supplier = supplier;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    @SuppressWarnings("unchecked")
    public <T extends OkaeriConfig> Map<String, T> getDefinitions() {
        Config cfg = EventMain.getOkaeriConfig();

        return switch (this) {
            case FREEZE_TAG -> (Map<String, T>) cfg.getFreezeTagMaps();
            case PAINTBALL2_1 -> (Map<String, T>) cfg.getPaintballMaps();
            case BOATRACE2 -> (Map<String, T>) cfg.getBoatRaceMaps();
            case TNTTAG -> (Map<String, T>) cfg.getTntTagMaps();
            case TOWERS -> (Map<String, T>) cfg.getTowersMaps();
            case MANOR ->  (Map<String, T>) cfg.getManorMaps();
            case PROP_HUNT -> (Map<String, T>) cfg.getPropHuntMaps();
            case THE_NABBITS -> (Map<String, T>) cfg.getTheNabbitsMaps();
            case PANEL_PARTY -> (Map<String, T>) cfg.getPanelPartyMaps();
            case MINEBATTLE -> (Map<String, T>) cfg.getMineBattleMaps();
            case TNTRUN -> (Map<String, T>) cfg.getTntRunMaps();
            case SOCCER -> (Map<String, T>) cfg.getSulfurSoccerMaps();
            case INCURSION -> (Map<String, T>) cfg.getIncursionMaps();
            case BOMBERMAN -> (Map<String, T>) cfg.getBombermanMaps();
        };
    }

    public Minigame instantiateWithRandomDefinition() {
        var randomDefinition = Util.getRandom(getDefinitions().values());
        return instantiate(randomDefinition);
    }

    public <T extends OkaeriConfig> Minigame instantiate(T definition) {
        return ((MinigameSupplier<T>) this.supplier).supply(definition);
    }

    public static MinigameConstant random() {
        return Util.getRandom(values());
    }

    @Nullable
    public static MinigameConstant fromAlias(String alias) {
        for (MinigameConstant constant : values()) {
            for (String a : constant.getAliases()) {
                if (a.equalsIgnoreCase(alias)) {
                    return constant;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface MinigameSupplier<T extends OkaeriConfig> {
        Minigame supply(T definition);
    }
}
