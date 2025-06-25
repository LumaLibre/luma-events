package dev.jsinco.luma.lumaevents.games.constants;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRace2Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.BoatRace2;
import dev.jsinco.luma.lumaevents.games.logic.Paintball2_1;
import dev.jsinco.luma.lumaevents.games.logic.TNTTag;
import dev.jsinco.luma.lumaevents.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

@Getter
public enum MinigameConstant {

    // This enum should only contain real minigames!
    PAINTBALL2_1(Paintball2_1.class, "paintball2.1", "paintball2_1"),
    BOATRACE2(BoatRace2.class, "boatrace2", "boatrace"),
    TNTTAG(TNTTag.class, "tnttag");

    private final Class<? extends Minigame> minigameClass;
    private final String[] aliases;

    MinigameConstant(Class<? extends Minigame> minigameClass, String... aliases) {
        this.minigameClass = minigameClass;
        this.aliases = aliases;
    }

    @SuppressWarnings("unchecked")
    public <T extends OkaeriConfig> Map<String, T> getDefinitions(Class<? extends Minigame> game) {
        Config cfg = EventMain.getOkaeriConfig();

        return switch (this) {
            case PAINTBALL2_1 -> (Map<String, T>) cfg.getPaintballMaps();
            case BOATRACE2 -> (Map<String, T>) cfg.getBoatRaceMaps();
            case TNTTAG -> (Map<String, T>) cfg.getTntTagMaps();
        };
    }

    public Supplier<Minigame> getSupplier() {
        var randomDefinition = Util.getRandom(getDefinitions(minigameClass).values());
        return getSupplier(randomDefinition);
    }

    public <T extends OkaeriConfig> Supplier<Minigame> getSupplier(T definition) {
        return switch (definition) {
            case Paintball2_1Definition paintball21Definition -> () -> new Paintball2_1(paintball21Definition);
            case BoatRace2Definition boatRace2Definition -> () -> new BoatRace2(boatRace2Definition);
            case MinigameDefinition minigameDefinition -> () -> new TNTTag(minigameDefinition);
            default -> throw new IllegalStateException("Unexpected value: " + definition);
        };
    }

    public static MinigameConstant random() {
        return Util.getRandom(values());
    }

    public static MinigameConstant fromClass(Class<? extends Minigame> gameClass) {
        for (MinigameConstant constant : values()) {
            if (constant.getMinigameClass().equals(gameClass)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No MinigameConstant found for class: " + gameClass.getCanonicalName());
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
}
