package dev.jsinco.luma.lumaevents.games.constants;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.BoatRace2;
import dev.jsinco.luma.lumaevents.games.logic.Manor;
import dev.jsinco.luma.lumaevents.games.logic.Paintball2_1;
import dev.jsinco.luma.lumaevents.games.logic.PanelParty;
import dev.jsinco.luma.lumaevents.games.logic.PropHunt;
import dev.jsinco.luma.lumaevents.games.logic.TNTTag;
import dev.jsinco.luma.lumaevents.games.logic.TheNabbits;
import dev.jsinco.luma.lumaevents.games.logic.Towers;
import dev.jsinco.luma.lumaevents.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
public enum MinigameConstant {

    // This enum should only contain real minigames!
    PAINTBALL2_1(Paintball2_1::new, "paintball2.1", "paintball2_1"),
    BOATRACE2(BoatRace2::new,"boatrace2", "boatrace"),
    TNTTAG(TNTTag::new, "tnttag"),
    TOWERS(Towers::new, "towers"),
    MANOR(Manor::new, "manor"),
    PROP_HUNT(PropHunt::new, "prophunt", "prop_hunt"),
    THE_NABBITS(TheNabbits::new, "thenabbits", "the_nabbits"),
    PANEL_PARTY(PanelParty::new, "panelparty", "panel_party")
    ;

    private final MinigameSupplier<?> supplier;
    private final String[] aliases;

    <T extends OkaeriConfig> MinigameConstant(MinigameSupplier<T> supplier, String... aliases) {
        this.supplier = supplier;
        this.aliases = aliases;
    }

    @SuppressWarnings("unchecked")
    public <T extends OkaeriConfig> Map<String, T> getDefinitions() {
        Config cfg = EventMain.getOkaeriConfig();

        return switch (this) {
            case PAINTBALL2_1 -> (Map<String, T>) cfg.getPaintballMaps();
            case BOATRACE2 -> (Map<String, T>) cfg.getBoatRaceMaps();
            case TNTTAG -> (Map<String, T>) cfg.getTntTagMaps();
            case TOWERS -> (Map<String, T>) cfg.getTowersMaps();
            case MANOR ->  (Map<String, T>) cfg.getManorMaps();
            case PROP_HUNT -> (Map<String, T>) cfg.getPropHuntMaps();
            case THE_NABBITS -> (Map<String, T>) cfg.getTheNabbitsMaps();
            case PANEL_PARTY -> (Map<String, T>) cfg.getPanelPartyMaps();
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
