package dev.lumas.events.utility.latency;

import org.bukkit.entity.Player;

public final class VanillaPingSource implements PingSource {

    @Override
    public String name() {
        return "keep-alive";
    }

    @Override
    public int pingMillis(Player player) {
        return Math.max(0, player.getPing());
    }
}
