package dev.lumas.events.configurable.sectors;

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
    private Location team1SpawnLocation;
    private Location team2SpawnLocation;
    private Region region = new Region();
    private TeamConfig team1 = new TeamConfig("Team Yellow", "yellow");
    private TeamConfig team2 = new TeamConfig("Team Blue", "blue");
    private int timeLimitSeconds = 360;
    private int freezeHitsRequired = 3;
    private int unfreezeHitsRequired = 2;
    private boolean allowFreezingWhileFrozen = false;
    private boolean allowUnfreezingWhileFrozen = false;
    private boolean allowHealingWhileFrozen = true;
    private int freezePoints = 3;
    private int unfreezePoints = 2;
    private int minimumTokens = 5;
    private int tokensPerPoint = 1;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TeamConfig extends OkaeriConfig {

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
