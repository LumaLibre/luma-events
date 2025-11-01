package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.obj.InventorySnapshot;
import dev.jsinco.luma.lumaevents.games.InventorySnapshotManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MinigameInventoryRestoringQuitListener implements Listener {

    private final Minigame minigame;

    public MinigameInventoryRestoringQuitListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        minigame.ensureNotIllegal();
        Player player = event.getPlayer();
        InventorySnapshot inventorySnapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(player.getUniqueId());
        if (inventorySnapshot != null) {
            inventorySnapshot.restore(player);
            InventorySnapshotManager.INSTANCE.unregisterSnapshot(inventorySnapshot);
        }
        EventPlayer eplayer = EventPlayerManager.getByUUID(player.getUniqueId());
        minigame.removeParticipant(eplayer);
        //minigame.sendAudienceMessage( eplayer.getName() + " has left the minigame.");

        Location gameDropOffLocation = minigame.getGameDropOffLocation();
        if (gameDropOffLocation != null) {
            player.teleport(gameDropOffLocation);
        }
    }
}
