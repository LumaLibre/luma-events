package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.utility.latency.LatencyHandler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface TraceShooter {

    Location shootPos();

    long nanoTime(LatencyHandler latencyHandler);

    boolean notShooter(Entity entity);

    record PlayerTraceShooter(Player player) implements TraceShooter {
        @Override
        public Location shootPos() {
            return player.getEyeLocation();
        }

        @Override
        public long nanoTime(LatencyHandler latencyHandler) {
            return latencyHandler.timeOf(player);
        }

        @Override
        public boolean notShooter(Entity entity) {
            return entity != player;
        }
    }

    record EntityTraceShooter(Entity entity, Location shootPos) implements TraceShooter {

        @Override
        public long nanoTime(LatencyHandler latencyHandler) {
            return System.nanoTime();
        }

        @Override
        public boolean notShooter(Entity entity) {
            return entity != this.entity;
        }
    }

    record UnboundTraceShooter(Location shootPos) implements TraceShooter {

        @Override
        public long nanoTime(LatencyHandler latencyHandler) {
            return System.nanoTime();
        }

        @Override
        public boolean notShooter(Entity entity) {
            return false;
        }
    }

}
