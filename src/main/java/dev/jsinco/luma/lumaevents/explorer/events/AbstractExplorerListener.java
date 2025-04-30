package dev.jsinco.luma.lumaevents.explorer.events;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

public abstract class AbstractExplorerListener implements Listener {

    public static void fire(Object event, Player player) {
        fire(event, player.getUniqueId());
    }

    public static void fireLater(Object event, Player player, long delay) {
        fireLater(event, player.getUniqueId(), delay);
    }

    public static void fire(Object event, UUID playerUUID) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(playerUUID);
        if (!eventPlayer.hasCompletedTutorialSection(TutorialSection.EXPLORER_MILES)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(EventMain.getInstance(),
                () -> eventPlayer.fireForExplorerMiles(event)
        );
    }

    public static void fireLater(Object event, UUID playerUUID, long delay) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(playerUUID);
        if (!eventPlayer.hasCompletedTutorialSection(TutorialSection.EXPLORER_MILES)) {
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(EventMain.getInstance(), () -> {
            if (!eventPlayer.isOnline()) {
                return; // Player logged off
            }
            eventPlayer.fireForExplorerMiles(event);
        }, delay);
    }
}
