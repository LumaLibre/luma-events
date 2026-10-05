package dev.lumas.events.games.constants;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.ToggleableDefinition;
import dev.lumas.events.configurable.sectors.MinigameDefinition;
import dev.lumas.events.configurable.sectors.game.BoatRace2Definition;
import dev.lumas.events.configurable.sectors.game.BombermanDefinition;
import dev.lumas.events.configurable.sectors.game.FreezeTagDefinition;
import dev.lumas.events.configurable.sectors.game.IncursionDefinition;
import dev.lumas.events.configurable.sectors.game.Manor2Definition;
import dev.lumas.events.configurable.sectors.game.ManorMinigameDefinition;
import dev.lumas.events.configurable.sectors.game.MineBattleDefinition;
import dev.lumas.events.configurable.sectors.game.MobArenaDefinition;
import dev.lumas.events.configurable.sectors.game.Paintball2_1Definition;
import dev.lumas.events.configurable.sectors.game.PanelPartyMinigameDefinition;
import dev.lumas.events.configurable.sectors.game.SulfurSoccerDefinition;
import dev.lumas.events.configurable.sectors.game.TNTRunDefinition;
import dev.lumas.events.configurable.sectors.game.TheNabbitsMinigameDefinition;
import dev.lumas.events.configurable.sectors.game.skywars.SkyWarsDefinition;
import dev.lumas.events.configurable.sectors.game.towers.TowersDefinition;
import dev.lumas.events.games.exceptions.NoAvailableMinigames;
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
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.mobarena.MobArenaRewrite;
import dev.lumas.events.games.logic.skywars.SkyWars;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public enum MinigameConstant {

    // This enum should only contain real minigames!
    PAINTBALL2_1(Paintball2_1Definition.class, Paintball2_1::new, "Paintball 2.1", "paintball2.1"),
    BOATRACE2(BoatRace2Definition.class, BoatRace2::new, "Boat Race 2", "boatrace2"),
    TNTTAG(MinigameDefinition.class, TNTTag::new, "TNT Tag", "tnttag"),
    TOWERS(TowersDefinition.class, Towers::new, "Towers", "towers"),
    MANOR(ManorMinigameDefinition.class, Manor::new, "Manor", "manor"),
    PROP_HUNT(ManorMinigameDefinition.class, PropHunt::new, "Prop Hunt", "prophunt"),
    THE_NABBITS(TheNabbitsMinigameDefinition.class, TheNabbits::new, "The Nabbits", "the_nabbits"),
    PANEL_PARTY(PanelPartyMinigameDefinition.class, PanelParty::new, "Panel Party", "panelparty"),
    MINEBATTLE(MineBattleDefinition.class, (config) -> new MineBattle(config), "MineBattle", "minebattle"),
    TNTRUN(TNTRunDefinition.class, TNTRun::new, "TNT Run", "tnt_run"),
    FREEZE_TAG(FreezeTagDefinition.class, FreezeTag::new, "Freeze Tag", "freeze_tag"),
    SOCCER(SulfurSoccerDefinition.class, Soccer::new, "Soccer", "sulfur_soccer", "soccer"),
    INCURSION(IncursionDefinition.class, Incursion::new, "Incursion", "incursion"),
    BOMBERMAN(BombermanDefinition.class, Bomberman::new, "Bomberman", "bomberman"),
    SKYWARS(SkyWarsDefinition.class, SkyWars::new, "SkyWars", "skywars"),
    MANOR_2(Manor2Definition.class, Manor2::new, "Manor 2", "manor2"),
    MOB_ARENA(MobArenaDefinition.class, MobArenaRewrite::new, "Mob Arena", "mob_arena")
    ;

    private final Class<?> definitionType;
    private final MinigameSupplier<?> supplier;
    private final String displayName;
    private final String[] aliases;

    <T> MinigameConstant(Class<T> definitionType, MinigameSupplier<T> supplier, String displayName, String... aliases) {
        this.definitionType = definitionType;
        this.supplier = supplier;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getDefinitions() {
        return (Map<String, T>) EventMain.getConfigManager().getDefinitions(this);
    }

    public <T> Map<String, T> getEnabledDefinitions() {
        Map<String, T> enabled = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : this.<T>getDefinitions().entrySet()) {
            if (entry.getValue() instanceof ToggleableDefinition toggleable && !toggleable.isEnabled()) continue;
            enabled.put(entry.getKey(), entry.getValue());
        }
        return enabled;
    }

    @Nullable
    public <T> T randomEnabledDefinition() {
        Map<String, T> enabled = getEnabledDefinitions();
        return enabled.isEmpty() ? null : Util.getRandom(enabled.values());
    }

    public Minigame instantiateWithRandomDefinition() {
        var randomDefinition = randomEnabledDefinition();
        if (randomDefinition == null) {
            throw new NoAvailableMinigames("Cannot start " + this.displayName + ": every one of its maps is disabled!");
        }
        return instantiate(randomDefinition);
    }

    public <T> Minigame instantiate(T definition) {
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
    private interface MinigameSupplier<T> {
        Minigame supply(T definition);
    }
}
