package dev.jsinco.lumacarnival.obj.earners;

import org.bukkit.entity.Player;

public interface GameEarner {
    int getPermanentAmount();
    int getAmount();
    String serialize();
    void cashIn(Player player);
}
