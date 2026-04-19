package dev.lumas.events.configurable.sectors;

import dev.lumas.events.manager.EventTeamManager;
import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;

@Getter
@Setter
public class FreezeTagDefinition extends OkaeriConfig {

    private Location lobbyLocation;
    private Location team1SpawnLocation; // TODO: Move this into TeamConfig
    private Location team2SpawnLocation; // TODO: Move this into TeamConfig
    private Region region = new Region();
    private TeamConfig team1 = new TeamConfig(EventTeamManager.Provider.SCARLET, "Scarlet", "red");
    private TeamConfig team2 = new TeamConfig(EventTeamManager.Provider.IVORY, "Ivory", "white");
    private int timeLimitSeconds = 360;
    private int freezeHitsRequired = 3;
    private int unfreezeHitsRequired = 4;
    private int roundWinsRequired = 2;
    private boolean allowFreezingWhileFrozen = false;
    private boolean allowUnfreezingWhileFrozen = false;
    private boolean allowHealingWhileFrozen = true;
    private int freezePoints = 6;
    private int unfreezePoints = 4;
    private int minimumTokens = 5;
    private int tokensPerPoint = 1;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TeamConfig extends OkaeriConfig {

        private EventTeamManager.Provider provider;
        private String name;
        private String color;

        public NamedTextColor getNamedTextColor() {
            NamedTextColor c = NamedTextColor.NAMES.value(color.toLowerCase());
            return c != null ? c : NamedTextColor.WHITE;
        }

        public Color getArmorColor() {
            int rgb = getNamedTextColor().value();
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
    }
}
