package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@Getter
@ConfigSerializable
public class CopycatDefinition {

    @MapTied
    private Location lobby;
    @MapTied
    @Comment("Corner the plot grid grows out of (towards +x and +z). Keep the area around it empty")
    private Location origin;
    @Comment("Build time is this many seconds per removed block")
    private double buildSecondsPerBlock = 2.5;
    private int maxRounds = 20;
    @Comment("Per-schematic overrides. Schematics without an entry use the defaults")
    private List<BuildSettings> builds = List.of(BuildSettings.offGround("mini_skyblock.schem"));

    @Getter
    @ConfigSerializable
    public static class BuildSettings {

        @Comment("File name inside schematics/copycat")
        private String schem = "";
        @Comment("Float the build above the plot floor instead of resting on it")
        private boolean offGround = false;
        @Comment("Never give less than this many build seconds")
        private @Nullable Integer minTime;
        @Comment("Never give more than this many build seconds")
        private @Nullable Integer maxTime;
        @Comment("Blocks of these materials are always left in place")
        private List<Material> ignoredMaterials = List.of();

        public BuildSettings() {
        }

        static BuildSettings offGround(String schem) {
            BuildSettings settings = new BuildSettings();
            settings.schem = schem;
            settings.offGround = true;
            return settings;
        }
    }
}
