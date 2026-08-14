package dev.lumas.events.utility.latency;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface PingSource {

    String name();

    default void start() {}

    default void stop() {}

    default void probe(Player player) {}

    default void retainOnly(Set<UUID> keep) {}

    int pingMillis(Player player);
}
