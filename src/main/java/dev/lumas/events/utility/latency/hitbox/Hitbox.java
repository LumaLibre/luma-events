package dev.lumas.events.utility.latency.hitbox;

import dev.lumas.events.configurable.sectors.tracing.ConeConfig;
import dev.lumas.events.utility.latency.tracing.TraceShooter;
import dev.lumas.events.utility.latency.tracing.Tracing;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sample positions are used for {@link Tracing#traceCone(TraceShooter, Predicate, Comparator, ConeConfig)}
 *
 * @param shape         a shape of bounding boxes to track beams
 * @param wholeBounding bounding box that contains all other bounding boxes
 * @param sampleOffsets the offset positions for each sample in relation to whole bounding box center
 */
public record Hitbox(List<Section> shape, BoundingBox wholeBounding,
                     Vector... sampleOffsets) {

    public record Section(HitboxPart part, BoundingBox boundingBox) {

    }
}
