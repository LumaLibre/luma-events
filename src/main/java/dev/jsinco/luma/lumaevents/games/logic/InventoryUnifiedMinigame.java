package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.games.inventories.InventorySnapshot;
import dev.jsinco.luma.lumaevents.games.inventories.InventorySnapshotManager;
import dev.jsinco.luma.lumaevents.games.inventories.UnsafeInventorySnapshot;
import org.bukkit.entity.Player;

public abstract class InventoryUnifiedMinigame extends Minigame {
    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
    }

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventInventoryTampering) {
        super(name, description, duration, tickInterval, async, true, preventInventoryTampering);
    }

    @Override
    public boolean start(int seconds) {
        if (!this.active) {
            return false;
        }

        this.participants.forEach(participant -> {
            Player player = participant.getPlayer();
            if (player == null) {
                this.removeParticipant(participant);
            } else {
                InventorySnapshot inventorySnapshot = new InventorySnapshot(participant.getUuid(), player.getInventory().getContents());
                inventorySnapshot.backup();
                InventorySnapshotManager.INSTANCE.registerSnapshot(inventorySnapshot);
            }

        });

        return super.start(seconds);
    }

    @Override
    public boolean stop() {
        if (!this.active) {
            return false;
        }

        this.participants.forEach(participant -> {
            Player player = participant.getPlayer();
            if (player != null) {
                InventorySnapshot snapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(participant.getUuid());
                if (snapshot != null) {
                    snapshot.restore(player);
                    InventorySnapshotManager.INSTANCE.unregisterSnapshot(snapshot);
                } else {
                    Logging.errorLog("Failed to restore inventory for player: " + participant.getUuid() + ". No snapshot found.");
                }
            }
        });
        return super.stop();
    }
}
