package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class Manor2Definition extends OkaeriConfig {

    private Location lobby;
    private List<Location> hunterStartLocations;
    private List<Location> ghostStartLocations;
    private Region region = new Region();
    private long duration = 1000 * 60 * 5;
    @Comment("The ratio of hunters per ghost (rounds up)")
    private int hunterRatio = 10;
    @Comment("The time until a ghost is killed by being revealed (ticks)")
    private int ghostRevealTime = 100;
    @Comment("The time until a hunter is killed by being consumed by ghost (ticks)")
    private int hunterDeathTime = 50;
    private int ghostKillScore = 50;
    private int hunterKillScore = 150;
    private LagCompensationSettings lagCompensation = new LagCompensationSettings();
    private MessagesDefinition messages;

    @Getter
    public static class LagCompensationSettings extends OkaeriConfig {

        private boolean enabled = true;
        private int interpolationMillis = 120;
        private int maxRewindNano = 300;
    }

    @Getter
    public static class MessagesDefinition extends OkaeriConfig {

        private List<String> ghostKilledMessages = List.of(
                "<gray><white><ghost></white> got blinded to death by <red><participants></red>",
                "<gray><white><ghost></white> got their sheet stolen by <red><participants></red> (death)"
        );

        private List<String> hunterKilledMessages = List.of(
                "<gray><red><hunter></red> got haunted to death by <white><participants></white>"
        );

        private String scoreBoardBorder = "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ";
        private String scoreRecord = """
                <green><gold><position></gold> is <red><scorer></red> with <gold><points> points</gold>!
                """;
        private String gameOver = """
                <yellow>Game Over
                <gold> <winner> has won!"
                """;
        private String hunters = "<gold>Hunters";
        private String ghosts = "<gradient:white:gray>Ghosts";
    }
}