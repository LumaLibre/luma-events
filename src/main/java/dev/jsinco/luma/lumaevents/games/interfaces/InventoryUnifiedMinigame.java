package dev.jsinco.luma.lumaevents.games.interfaces;

import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.games.events.MinigameInventoryRestoringQuitListener;
import dev.jsinco.luma.lumaevents.games.obj.InventorySnapshot;
import dev.jsinco.luma.lumaevents.games.InventorySnapshotManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class InventoryUnifiedMinigame extends Minigame {

    private final MinigameInventoryRestoringQuitListener quitListener = new MinigameInventoryRestoringQuitListener(this);

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
        this.addExtraListener(quitListener);
    }

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit) {
        super(name, description, duration, tickInterval, async, preventExit, true);
        this.addExtraListener(quitListener);
    }

    @Override
    protected void onPreStart() {
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                this.removeParticipant(participant);
                continue;
            }

            InventorySnapshot inventorySnapshot = new InventorySnapshot(participant.getUuid(), player.getInventory().getContents());
            inventorySnapshot.backup();
            InventorySnapshotManager.INSTANCE.registerSnapshot(inventorySnapshot);
            player.getInventory().clear();

            if (this.defaultItem() != null) {
                player.getInventory().setItemInMainHand(this.defaultItem());
            }
        }
    }

    @Override
    public boolean stop() {
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                continue;
            }
            InventorySnapshot snapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(participant.getUuid());
            if (snapshot != null) {
                snapshot.restore(player);
                InventorySnapshotManager.INSTANCE.unregisterSnapshot(snapshot);
            } else {
                Logging.errorLog("Failed to restore inventory for player: " + participant.getUuid() + ". No snapshot found.");
            }
        }

        return super.stop();
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
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
        return super.removeParticipant(participant);
    }

    @Nullable
    protected ItemStack defaultItem() {
        return null;
    }
}
