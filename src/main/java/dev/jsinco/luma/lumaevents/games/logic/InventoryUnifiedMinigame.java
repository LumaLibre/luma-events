package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.games.events.MinigameInventoryRestoringQuitListener;
import dev.jsinco.luma.lumaevents.games.inventories.InventorySnapshot;
import dev.jsinco.luma.lumaevents.games.inventories.InventorySnapshotManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class InventoryUnifiedMinigame extends Minigame {

    private final MinigameInventoryRestoringQuitListener quitListener = new MinigameInventoryRestoringQuitListener(this);
    private ItemStack defaultItem;

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
        this.addExtraListener(quitListener);
    }

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit) {
        super(name, description, duration, tickInterval, async, preventExit, true);
        this.addExtraListener(quitListener);
    }

    @Override
    public boolean start(int seconds) {
        if (!this.active) {
            return false;
        }


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

            if (this.defaultItem != null) {
                player.getInventory().setItemInMainHand(this.defaultItem);
            }
        }

        return super.start(seconds);
    }

    @Override
    public boolean stop() {
        if (!this.active) {
            return false;
        }

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

    @Nullable
    public ItemStack defaultItem() {
        return null;
    };
}
