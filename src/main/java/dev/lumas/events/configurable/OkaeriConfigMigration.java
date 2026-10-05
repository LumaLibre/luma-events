package dev.lumas.events.configurable;

import dev.lumas.events.games.constants.MinigameConstant;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@NullMarked
@UtilityClass
public final class OkaeriConfigMigration {

    private static final Map<MinigameConstant, String> LEGACY_MAP_SECTIONS = Map.ofEntries(
            entry(MinigameConstant.FREEZE_TAG, "freezeTagMaps"),
            entry(MinigameConstant.PAINTBALL2_1, "paintballMaps"),
            entry(MinigameConstant.BOATRACE2, "boatRaceMaps"),
            entry(MinigameConstant.TNTTAG, "tntTagMaps"),
            entry(MinigameConstant.TOWERS, "towersMaps"),
            entry(MinigameConstant.MANOR, "manorMaps"),
            entry(MinigameConstant.PROP_HUNT, "propHuntMaps"),
            entry(MinigameConstant.THE_NABBITS, "theNabbitsMaps"),
            entry(MinigameConstant.PANEL_PARTY, "panelPartyMaps"),
            entry(MinigameConstant.MINEBATTLE, "mineBattleMaps"),
            entry(MinigameConstant.TNTRUN, "tntRunMaps"),
            entry(MinigameConstant.SOCCER, "sulfurSoccerMaps"),
            entry(MinigameConstant.INCURSION, "incursionMaps"),
            entry(MinigameConstant.BOMBERMAN, "bombermanMaps"),
            entry(MinigameConstant.SKYWARS, "skyWarsMaps"),
            entry(MinigameConstant.MANOR_2, "manor2Maps"),
            entry(MinigameConstant.MOB_ARENA, "mobArenaMaps")
    );

    public static boolean migrateMaps(ConfigurationNode legacyConfig, MinigameConstant minigame, ConfigurationNode defaultShared, ConfigurationNode shared, ConfigurationNode maps) throws SerializationException {
        String section = LEGACY_MAP_SECTIONS.get(minigame);
        if (section == null || !legacyConfig.node(section).isMap()) {
            return false;
        }
        ConfigurationNode legacyMaps = legacyConfig.node(section).copy();
        if (legacyMaps.empty()) {
            legacyConfig.removeChild(section);
            return false;
        }

        Class<?> type = minigame.getDefinitionType();
        shared.set(Collections.emptyMap());
        for (Map.Entry<Object, ? extends ConfigurationNode> legacyMap : legacyMaps.childrenMap().entrySet()) {
            ConfigSchema.normalizeKeys(maps.node(legacyMap.getKey()).from(legacyMap.getValue()), type);
        }

        List<? extends ConfigurationNode> migrated = List.copyOf(maps.childrenMap().values());
        ConfigSchema.extractCommon(migrated, type, shared);
        ConfigSchema.merge(shared, defaultShared, type, false);
        for (ConfigurationNode map : migrated) {
            ConfigSchema.removeMatching(map, shared, type);
        }
        legacyConfig.removeChild(section);
        return true;
    }
}
