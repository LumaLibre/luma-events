package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactType;
import dev.lumas.events.utility.effects.ParticleDefinition;
import dev.lumas.events.utility.effects.SoundDefinition;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.BoundingBox;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@Getter
@ConfigSerializable
public class Manor2Definition {

    private Location lobby;
    @MapTied
    private List<Location> hunterStartLocations;
    @MapTied
    private List<Location> ghostStartLocations;
    @MapTied
    private Region region = new Region();
    private long duration = 1000 * 60 * 5;
    @Comment("The ratio of hunters per ghost (rounds up)")
    private int hunterRatio = 10;
    @Comment("The time until a ghost is killed by being revealed (ticks)")
    private int ghostRevealTime = 100;
    @Comment("The time until a hunter is killed by being consumed by ghost (ticks)")
    private int hunterDeathTime = 50;
    @Comment("The score a ghost get when killing a hunter")
    private int ghostKillScore = 75;
    @Comment("The score a hunter get when killing a ghost")
    private int hunterKillScore = 150;
    @Comment("The score a hunter get when finding an artifact")
    private int artifactFoundScore = 25;
    @Comment("The score a hunter gets when finding a player artifact")
    private int playerArtifactFoundScore = 75;
    @Comment("The width of each region an artifact can spawn in")
    private double averageRegionWidth = 16;
    @Comment("Allow players to play as artifacts")
    private boolean artifactPlay = true;
    @Comment("What fraction of artifact regions should be filled depending on hunter count")
    private String regionFillFactor = "0.25 + min((hunter_count + 1) / 30, 0.75)";
    private LagCompensationSettings lagCompensation = new LagCompensationSettings();
    private List<ParticleDefinition> flashlightGroundParticles = List.of(
        new ParticleDefinition(
            Particle.DUST,
            15,
            10,
            new BoundingBox(0, 0.25, 0, 1, 0.5, 1),
            new Particle.DustOptions(Color.GRAY, 2f)
        ),
        new ParticleDefinition(
            Particle.WHITE_ASH,
            5,
            1,
            new BoundingBox(0, 2.25, 0, 1, 2.75, 1),
            null
        )
    );

    private List<SoundDefinition> artifactRevealSounds = List.of(
        new SoundDefinition(Sound.AMBIENT_CAVE, 20, 1f, null)
    );
    private List<SoundDefinition> artifactPickupSounds = List.of(
        new SoundDefinition(Sound.BLOCK_DECORATED_POT_INSERT_FAIL, 5, 1f, null)
    );
    private List<SoundDefinition> ghostRevealSounds = List.of(
        new SoundDefinition(Sound.ENTITY_GHAST_AMBIENT, 20, 1f, null)
    );

    private List<ArtifactType> artifacts = List.of(
        new ArtifactType(ItemType.GOLD_NUGGET, "<gold>Precious Gold", List.of("Worth it's weight? Yes", "Does it weigh much? No")),
        new ArtifactType(ItemType.ANVIL, "<gray>Heavy Anvil", List.of("Somehow you got the idea", "to carry this with you")),
        new ArtifactType(ItemType.COOKED_MUTTON, "<color:#FF9412>Old Beef Jerky", List.of("You don't think this is edible", "but still want it for your dog")),
        new ArtifactType(ItemType.BEACON, "<blue>Scientific Instrument", List.of("Nobody knows what it does")),
        new ArtifactType(ItemType.ANCIENT_DEBRIS, "<white>Just a pebble", List.of("You apparently like collecting stones")),
        new ArtifactType(ItemType.ACACIA_BOAT, "<color:#FF9412>A Boat?", List.of("There's unfortunately no water here")),
        new ArtifactType(ItemType.ITEM_FRAME, "<red>Artifact Frame", List.of("You might not find anything to frame though")),
        new ArtifactType(ItemType.ACACIA_BUTTON, "<color:#FF9412>Door Knob", List.of("Hoarding again, eh?")),
        new ArtifactType(ItemType.ACACIA_SAPLING, "<green>Memorial Sapling", List.of("To plant in remembrance of all", "the dead hunters")),
        new ArtifactType(ItemType.SUSPICIOUS_STEW, "<dark_purple>Putrid Dish", List.of("Smells and tastes horrible.", "Although tasting was never an option")),
        new ArtifactType(ItemType.DIRT, "<color:#FF9412>Normal Looking Dirt", List.of("You still don't know why you", "picked it up")),
        new ArtifactType(ItemType.APPLE, "<red>Perfect Apple", List.of("''One bite and all your dreams will come true.''", "Maybe it's better to leave it.")),
        new ArtifactType(ItemType.CLOCK, "<gold>Broken Clock", List.of("Seems to be moving though?")),
        new ArtifactType(ItemType.BAMBOO_BLOCK, "<color:#FF9412>Empty Cardboard Box", List.of("I've heard that cats like these")),
        new ArtifactType(ItemType.FISHING_ROD, "<blue>Fishing Rod", List.of("The line is a jumbled mess.", "Quite nice quality otherwise"))
    );

    private List<ArtifactType> playerArtifacts = List.of(
        new ArtifactType(ItemType.NETHERITE_INGOT, "Mysterious Ingot", List.of("Someone might find this interesting")),
        new ArtifactType(ItemType.BAMBOO, "World Tree Sapling", List.of("The world tree??", "Must be worth quite a lot")),
        new ArtifactType(ItemType.GOLD_BLOCK, "<gold>Block Of Gold", List.of("Definitely worth its weight in gold")),
        new ArtifactType(ItemType.FISHING_ROD, "<blue>Premium Fishing Rod", List.of("Could even catch the good old", "Loch Ness with it")),
        new ArtifactType(ItemType.DIAMOND, "<aqua>Large Gem", List.of("To keep for your partner")),
        new ArtifactType(ItemType.DIAMOND_HORSE_ARMOR, "<aqua>Decorated Horse Armor", List.of("It might too decorated to be practical",
            "still impressive"))
    );

    @Getter
    @ConfigSerializable
    public static class LagCompensationSettings {

        private boolean enabled = true;
        private int interpolationMillis = 120;
        private int maxRewindNano = 300;
    }
}
