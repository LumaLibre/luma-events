package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class MinigamePreventDamageListener implements Listener {

    private final Minigame minigame;

    public MinigamePreventDamageListener(Minigame minigame) {
        this.minigame = minigame;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDamaged(EntityDamageEvent event) {
        minigame.ensureNotIllegal();

        if (event.getEntity() instanceof Player player && minigame.getBoundingBox().contains(player)) {
            event.setDamage(0.0);
        }
    }
}
