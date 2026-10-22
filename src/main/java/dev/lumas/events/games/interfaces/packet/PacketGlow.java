package dev.lumas.events.games.interfaces.packet;

import dev.lumas.events.utility.Externals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.function.BiPredicate;

public interface PacketGlow {

    void start();

    void stop();

    void tick();

    void addViewer(Player viewer); // up to delegates to make sure this is thread safe

    void removeViewer(Player viewer);

    void addTarget(Entity target);

    void removeTarget(Entity target);

    static PacketGlow newInstance() {
        return newInstance((viewer, target) -> true);
    }

    static PacketGlow newInstance(BiPredicate<Player, Entity> filter) {
        return Externals.pluginExists("ProtocolLib") ? new ProtocolLibPacketGlow(filter) : noop();
    }

    static PacketGlow noop() {
        return new PacketGlow() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void tick() {
            }

            @Override
            public void addViewer(Player viewer) {
            }

            @Override
            public void removeViewer(Player viewer) {
            }

            @Override
            public void addTarget(Entity target) {
            }

            @Override
            public void removeTarget(Entity target) {
            }
        };
    }
}
