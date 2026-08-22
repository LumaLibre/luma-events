package dev.lumas.events.listeners;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.utility.JoinTrace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Register(Autowire.LISTENER)
public class TraceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!JoinTrace.isVerbose()) return;
        Minigame current = MinigameManager.getInstance().getCurrent();
        String phase = current.isOpen() ? "queue" : current.isActive() ? "active" : "idle";
        JoinTrace.quit(event.getPlayer().getUniqueId(), phase);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!JoinTrace.isVerbose()) return;
        JoinTrace.worldChange(event.getPlayer(), event.getFrom().getName(), event.getPlayer().getWorld().getName());
    }
}
