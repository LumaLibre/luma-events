package dev.lumas.events.configurable.sectors;

import dev.lumas.events.games.logic.manor2.artifact.ArtifactType;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.effects.ParticleDefinition;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.BoundingBox;

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
    private int artifactFoundScore = 50;
    private LagCompensationSettings lagCompensation = new LagCompensationSettings();
    private MessagesDefinition messages = new MessagesDefinition();
    private List<ParticleDefinition> flashlightGroundParticles = List.of(
            new ParticleDefinition(
                    Particle.DUST,
                    15,
                    10,
                    new BoundingBox(0, 0, 0, 1, 1, 1),
                    new Particle.DustOptions(Color.GRAY, 2f)
            )
    );
    private List<WorldTiedBoundingBox> artifactSpawningRegions = List.of();
    private List<ArtifactType> artifacts = List.of(
            new ArtifactType(ItemType.GOLD_INGOT, "<gold>Precious Gold", List.of("Worth it's weight? Yes", "Does it weigh much? No")),
            new ArtifactType(ItemType.ANVIL, "<gray>Heavy Anvil", List.of("Somehow you got the idea", "to carry this with you")),
            new ArtifactType(ItemType.COOKED_MUTTON, "<brown>Old Beef Jerky", List.of("You don't think this is edible", "but still want it for your dog")),
            new ArtifactType(ItemType.BEACON, "<blue>Scientific Instrument", List.of("Nobody knows what it does")),
            new ArtifactType(ItemType.ANCIENT_DEBRIS, "<white>Just a pebble", List.of("You apparently like collecting stones")),
            new ArtifactType(ItemType.ACACIA_BOAT, "<brown>A Boat?", List.of("There's unfortunately no water here")),
            new ArtifactType(ItemType.ITEM_FRAME, "<red>Artifact Frame", List.of("You might not find anything to frame though")),
            new ArtifactType(ItemType.ACACIA_BUTTON, "<brown>Door Knob", List.of("Hoarding again, eh?")),
            new ArtifactType(ItemType.ACACIA_SAPLING, "<green>Memorial Sapling", List.of("To plant in remembrance of all", "the dead hunters")),
            new ArtifactType(ItemType.SUSPICIOUS_STEW, "<purple>Putrid Dish", List.of("Smells and tastes horrible.", "Although tasting was never an option")),
            new ArtifactType(ItemType.DIRT, "<brown>Normal Looking Dirt", List.of("You still don't know why you", "picked it up")),
            new ArtifactType(ItemType.APPLE, "<red>Perfect Apple", List.of("''One bite and all your dreams will come true.''", "Maybe it's better to leave it.")),
            new ArtifactType(ItemType.CLOCK, "<gold>Broken Clock", List.of("Seems to be moving though?")),
            new ArtifactType(ItemType.BAMBOO_BLOCK, "<brown>Empty Cardboard Box", List.of("I've heard that cats like these")),
            new ArtifactType(ItemType.FISHING_ROD, "<blue>Fishing Rod", List.of("The line is a jumbled cluster.", "Quite nice quality otherwise"))
    );

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

        private List<String> artifactFoundMessages = List.of(
                "<gray><red><participants></red> found a completely worthless artifact",
                "<gray><red><participants></red> found a dusty relic"
        );

        private String scoreBoardBorder = "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ";
        private String scoreRecord = """
                <green><gold>#<position></gold> is <red><scorer></red> with <gold><points> points</gold>! (<team>)
                """;
        private String gameOver = """
                <yellow>Game Over
                <gold> <winner> has won!"
                """;
        private String teamAssignmentShowMessage = "<gray>You are in the <team> team";
        private String hunters = "<gradient:gray:red>Hunters";
        private String ghosts = "<gradient:white:gray>Ghosts";
        private String hunterDescription = "<gray>Kill ghosts and find artifacts with your flashlight";
        private String ghostDescription = "<gray>Attack hunters, stay away from light";
        private List<String> artifactExtraLore = List.of("This item has no use");
        private List<String> splash = List.of(
                "Are you afraid of ghosts?",
                "Boooooooooooo!!!!",
                "Ghost Busters!",
                "Who you gonna call?",
                "House of Dracula!",
                "Trick or treat?",
                "Here's Johny!!!",
                "Put these foolish ambitions to rest",
                "Wazzzuup!",
                "Did you pray enough?",
                "It's alive! It's alive!",
                "Be afraid. Be very afraid",
                "What is the color of night?",
                "This is not a dream!",
                "Someone must extinguish thy flame.",
                "Don't cross the streams!!!!"
        );
    }
}