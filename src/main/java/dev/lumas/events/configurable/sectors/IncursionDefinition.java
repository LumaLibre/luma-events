package dev.lumas.events.configurable.sectors;

import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.BlockFaces;
import dev.lumas.events.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.List;

@Getter
@Setter
public class IncursionDefinition extends OkaeriConfig {

    @Comment("Where participants wait before the game starts and are sent once it has concluded")
    private Location lobbyLocation;

    @Comment("Bounding box of the entire map, used to tell whether something happens inside this game")
    private Region bounds = new Region();

    private TeamDefinition team1 = new TeamDefinition("Scarlet", "red");
    private TeamDefinition team2 = new TeamDefinition("Ivory", "white");

    @Comment("Total game length, sides are swapped at half time")
    private int gameLengthSeconds = 480;

    @Comment("How long players are frozen at the start of each half while the start countdown is shown")
    private int startCooldownTicks = 100;

    @Comment("How long a player is frozen at their spawn after scoring, dying or leaving the map")
    private int respawnFreezeTicks = 40;

    @Comment("How long a player cannot deal or take damage after being sent back to their spawn")
    private int respawnInvincibilityTicks = 60;

    @Comment("Points awarded for jumping into the enemy team's hole")
    private int holePoints = 10;

    @Comment("Points awarded for killing an enemy")
    private int killPoints = 5;

    @Comment("How many points a participant has to earn for one token")
    private int pointsPerToken = 5;

    @Comment("Tokens every participant gets, regardless of their score")
    private int minimumTokens = 5;

    @Comment("Our equivalent of a sniper (spyglass)")
    private SniperSettings sniper = new SniperSettings();

    @Comment("Our equivalent of a shotgun (goat horn)")
    private ShotgunSettings shotgun = new ShotgunSettings();

    @Comment("Our equivalent of a sidearm (pufferfish)")
    private SpitterSettings spitter = new SpitterSettings();

    @Comment("Floor positions orbs float above (in world,x,y,z form)")
    private List<Location> orbSpawns = List.of();

    @Comment("How long after being collected an orb comes back")
    private int orbRespawnTicks = 600;

    @Comment("The grenades orbs hand out (eggs)")
    private GrenadeSettings grenade = new GrenadeSettings();

    @Comment("Rooms a miniboss is spawned in and locked inside of (world,x,y,z/world,x,y,z)")
    private List<WorldTiedBoundingBox> bossRooms = List.of();

    @Comment("The minibosses guarding boss rooms")
    private MinibossSettings miniboss = new MinibossSettings();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SniperSettings extends OkaeriConfig {

        @Comment("How long the shot has to be charged before releasing fires it")
        private int chargeTicks = 20;

        @Comment("Cooldown after a shot was fired (0 to disable)")
        private int cooldownTicks = 55;

        @Comment("How far the shot travels (it always stops at the first solid block)")
        private double range = 75.0;

        @Comment("Max distance between the beam and the player's hitbox")
        private double hitRadius = 0.1;

        @Comment("True damage dealt to every player the beam passes through")
        private double damage = 15.0;

        @Comment("How hard hits shove players away from the shooter")
        private double knockback = 0.8;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ShotgunSettings extends OkaeriConfig {

        @Comment("Cooldown between shots")
        private int cooldownTicks = 30;

        @Comment("How far the cone reaches")
        private double range = 6.5;

        @Comment("Total width of the cone in degrees (45 means 22.5 degrees to either side)")
        private double coneDegrees = 45.0;

        @Comment("True damage dealt at point blank range")
        private double damage = 25.0;

        @Comment({
                "How much of the damage is lost at maximum range",
                "0 = full damage everywhere, 1 = nothing at the edge"
        })
        private double damageFalloff = 0.85;

        @Comment("How hard hits shove players away from the shooter")
        private double knockback = 0.6;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SpitterSettings extends OkaeriConfig {

        @Comment("Cooldown between shots")
        private int cooldownTicks = 15;

        @Comment("How fast the stream leaves the hand (in blocks per tick)")
        private double speed = 1.2;

        @Comment("How much the stream drops per tick (in blocks)")
        private double gravity = 0.06;

        @Comment("How many degrees above the player's aim the stream is launched at")
        private double launchAngleDegrees = 10.0;

        @Comment("How far the stream may travel (it always stops at the first solid block)")
        private double range = 22.0;

        @Comment("How close the stream has to pass a player's hitbox to soak them")
        private double hitRadius = 0.15;

        @Comment("True damage dealt to the first player the stream hits")
        private double damage = 6.5;

        @Comment({
                "How much of the damage is lost at maximum range",
                "0 = full damage everywhere, 1 = nothing at the edge"
        })
        private double damageFalloff = 0.25;

        @Comment("How hard hits shove players along the stream's path")
        private double knockback = 0.2;

        @Comment("How many points the stream is drawn at per tick (purely cosmetic)")
        private int trailSteps = 4;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GrenadeSettings extends OkaeriConfig {

        @Comment("True damage dealt at the centre of the blast")
        private double damage = 22.0;

        @Comment("How far the blast reaches")
        private double radius = 4.5;

        @Comment({
                "How much of the damage is lost at the edge of the blast",
                "0 = full damage everywhere, 1 = nothing at the edge"
        })
        private double damageFalloff = 0.7;

        @Comment("How hard the blast shoves players away from it")
        private double knockback = 0.7;

        @Comment("Whether a wall between the blast and a player protects them")
        private boolean requireLineOfSight = true;

        @Comment("Blue egg: how long targets stay frosted over")
        private int freezeTicks = 150;

        @Comment("Blue egg: how long targets are slowed for")
        private int slownessTicks = 100;

        @Comment("Blue egg: strength of that slowness, 0 being Slowness I")
        private int slownessAmplifier = 2;

        @Comment("Brown egg: how long targets burn for")
        private int fireTicks = 100;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MinibossSettings extends OkaeriConfig {

        @Comment("Which mob guards every boss room: DROWNED (trident), PARCHED (bow) or WITCH (potions)")
        private MinibossType type = MinibossType.DROWNED;

        @Comment("How much health a miniboss spawns with")
        private double health = 100.0;

        @Comment("How long after being slain a miniboss comes back (0 = only at half time)")
        private int respawnTicks = 1200;

        @Comment("How many random grenades a miniboss drops when it is slain (single number or range like '2-4')")
        private String grenadeDrops = "2-4";

        @Comment("Points awarded for killing a miniboss")
        private int points = 20;
    }

    public enum MinibossType {
        DROWNED,
        PARCHED,
        WITCH
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamDefinition extends OkaeriConfig {

        private String name = "Team";

        @Comment("A NamedTextColor (e.g. red, green, blue, ...)")
        private String color = "white";

        @Comment("Flat rectangle the team spawns across, so players don't stack inside each other")
        private SpawnArea spawnArea = new SpawnArea();

        @Comment("The hole enemies score in by jumping into it")
        private Region hole = new Region();

        public TeamDefinition(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public NamedTextColor getNamedTextColor() {
            NamedTextColor namedTextColor = NamedTextColor.NAMES.value(color.toLowerCase());
            return namedTextColor != null ? namedTextColor : NamedTextColor.WHITE;
        }

        public Color getArmorColor() {
            int rgb = getNamedTextColor().value();
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpawnArea extends OkaeriConfig {

        private Location corner1;
        private Location corner2;

        @Comment("Direction players look in when they spawn: NORTH/EAST/SOUTH/WEST or a diagonal such as NORTH_EAST")
        private BlockFace facing = BlockFace.NORTH;

        public Location randomSpawn() {
            if (corner1 == null) {
                throw new IllegalStateException("Spawn area is missing its first corner");
            }
            Location other = corner2 != null ? corner2 : corner1;

            int minX = Math.min(corner1.getBlockX(), other.getBlockX());
            int maxX = Math.max(corner1.getBlockX(), other.getBlockX());
            int minZ = Math.min(corner1.getBlockZ(), other.getBlockZ());
            int maxZ = Math.max(corner1.getBlockZ(), other.getBlockZ());

            return new Location(
                    corner1.getWorld(),
                    minX + Util.RANDOM.nextInt(maxX - minX + 1) + 0.5,
                    corner1.getBlockY(),
                    minZ + Util.RANDOM.nextInt(maxZ - minZ + 1) + 0.5,
                    getYaw(),
                    0f
            );
        }

        public float getYaw() {
            return BlockFaces.isHorizontal(facing) ? BlockFaces.faceToYaw(facing) : 0f;
        }
    }
}
