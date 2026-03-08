package dev.lumas.events.games.interfaces;

import dev.lumas.lumacore.utility.Logging;
import dev.lumas.events.games.events.MinigameInventoryRestoringQuitListener;
import dev.lumas.events.games.models.InventorySnapshot;
import dev.lumas.events.games.InventorySnapshotManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Executors;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class InventoryUnifiedMinigame extends Minigame {

    private final MinigameInventoryRestoringQuitListener quitListener = new MinigameInventoryRestoringQuitListener(this);

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
        this.addExtraListener(quitListener);
    }

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit, boolean preventDamage) {
        super(name, description, duration, tickInterval, async, preventExit, true, preventDamage);
        this.addExtraListener(quitListener);
    }

    protected InventoryUnifiedMinigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit, boolean preventInvTampering, boolean preventDamage) {
        super(name, description, duration, tickInterval, async, preventExit, preventInvTampering, preventDamage);
        this.addExtraListener(quitListener);
    }

    // do not override
    @Override
    protected void onPreStart() {
        List<EventPlayer> removed = new ArrayList<>();
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                removed.add(participant);
                continue;
            }

            Executors.runSync(player, () -> {
                InventorySnapshot inventorySnapshot = new InventorySnapshot(participant.getUuid(), player.getInventory().getContents());
                inventorySnapshot.backup();
                InventorySnapshotManager.INSTANCE.registerSnapshot(inventorySnapshot);
                player.getInventory().clear();

                InventoryView openInv = player.getOpenInventory();
                if (!openInv.getCursor().isEmpty()) {
                    openInv.setCursor(null);
                }
                player.closeInventory();

                if (this.defaultItem() != null) {
                    player.getInventory().setItemInMainHand(this.defaultItem());
                }
            });

        }

        for (EventPlayer p : removed) {
            Executors.runSync(p, () -> {
                this.removeParticipant(p, false);
            });
        }
    }

    @Override
    public void onPostStop() {
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                continue;
            }
            Executors.runSync(participant, () -> {
                InventorySnapshot snapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(participant.getUuid());
                if (snapshot != null) {
                    snapshot.restore(player);
                    InventorySnapshotManager.INSTANCE.unregisterSnapshot(snapshot);
                } else {
                    Logging.errorLog("Failed to restore inventory for player: " + participant.getUuid() + ". No snapshot found.");
                }
                this.tokenHandler(participant);
            });
        }
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        String m = "This minigame will modify your inventory during gameplay. <yellow>Disconnecting will automatically kick you from this game.</yellow>";
        player.sendMessage(m);
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        Player player = participant.getPlayer();
        Executors.runSync(participant, () -> {
            if (player != null) {
                InventorySnapshot snapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(participant.getUuid());
                if (snapshot != null) {
                    snapshot.restore(player);
                    InventorySnapshotManager.INSTANCE.unregisterSnapshot(snapshot);
                } else {
                    Logging.warningLog("Failed to restore inventory for player: " + participant.getUuid() + ". No snapshot found. (Participant removal)");
                }
            }
            if (!this.isOpen()) {
                this.tokenHandler(participant);
            }
        });
        return super.removeParticipant(participant, doTeleport);
    }

    @Nullable
    protected ItemStack defaultItem() {
        return null;
    }

    protected abstract void tokenHandler(EventPlayer participant);
}
