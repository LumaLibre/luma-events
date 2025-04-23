package dev.jsinco.luma.lumaevents.explorer.events;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

public abstract class AbstractExplorerListener implements Listener {

    public static void fire(Object event, Player player) {
        // TODO: Disabled.
//        fire(event, player.getUniqueId());
    }

    public static void fire(Object event, UUID playerUUID) {
        // TODO: Disabled.

        //todo: if havent talked to anais yet, return
//        EventPlayer eventPlayer = EventPlayerManager.getByUUID(playerUUID);
//
//        Bukkit.getScheduler().runTaskAsynchronously(EventMain.getInstance(),
//                () -> eventPlayer.fireForExplorerMiles(event)
//        );
    }
}
