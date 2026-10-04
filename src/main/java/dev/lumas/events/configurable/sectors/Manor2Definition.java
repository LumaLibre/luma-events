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
                """
                        <ghost> got blinded to death by <participants>
                        """
        );

        private List<String> hunterKilledMessages = List.of(
                """
                        <hunter> got haunted to death by <participants>
                        """
        );
    }
}