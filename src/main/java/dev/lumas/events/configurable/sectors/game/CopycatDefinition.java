package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@Getter
@ConfigSerializable
public class CopycatDefinition {

    @MapTied
    private Location lobby;
    @MapTied
    @Comment("Corner the plot grid grows out of (towards +x and +z). Keep the area around it empty")
    private Location origin;
    @Comment("Build time is this many seconds per removed block")
    private double buildSecondsPerBlock = 1.5;
    private int maxRounds = 20;
}
