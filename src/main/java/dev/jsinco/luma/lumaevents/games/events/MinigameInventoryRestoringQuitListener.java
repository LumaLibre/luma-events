package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.obj.InventorySnapshot;
import dev.jsinco.luma.lumaevents.games.InventorySnapshotManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
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
        InventorySnapshot inventorySnapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(event.getPlayer().getUniqueId());
        if (inventorySnapshot != null) {
            inventorySnapshot.restore(event.getPlayer());
            InventorySnapshotManager.INSTANCE.unregisterSnapshot(inventorySnapshot);
        }
        EventPlayer eplayer = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());
        minigame.removeParticipant(eplayer);
    }
}
