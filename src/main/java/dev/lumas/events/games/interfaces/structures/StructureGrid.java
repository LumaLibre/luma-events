package dev.lumas.events.games.interfaces.structures;

import org.bukkit.Location;

import java.util.List;

/**
 * An interface representing a structure grid that can generate spawn locations.
 * @see BoundStrctureGrid
 * @see UnboundStructureGrid
 */
public interface StructureGrid {

    List<Location> generateSpawnLocations(int desiredPoints);
}
