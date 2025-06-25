package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.games.exceptions.UnsafeInventorySnapshot;
import dev.jsinco.luma.lumaevents.games.obj.InventorySnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class InventorySnapshotManager {

    public static final InventorySnapshotManager INSTANCE = new InventorySnapshotManager();

    private final Set<InventorySnapshot> inventorySnapshots = new HashSet<>();

    public void registerSnapshot(InventorySnapshot snapshot) {
        if (!snapshot.isBackedUp()) {
            throw new UnsafeInventorySnapshot("Cannot register an inventory snapshot that has not been backed up.");
        }
        inventorySnapshots.add(snapshot);
    }

    public void unregisterSnapshot(InventorySnapshot snapshot) {
        inventorySnapshots.remove(snapshot);
    }

    @Nullable
    public InventorySnapshot getSnapshotByOwner(UUID owner) {
        return inventorySnapshots.stream()
                .filter(snapshot -> snapshot.getOwner().equals(owner))
                .findFirst()
                .orElse(null);
    }

    public boolean restoreSnapshot(UUID owner) {
        InventorySnapshot snapshot = getSnapshotByOwner(owner);
        if (snapshot != null) {
            if (!snapshot.restore()) {
                throw new IllegalStateException("Failed to restore inventory snapshot for player: " + owner);
            }
            unregisterSnapshot(snapshot);
            return true;
        }
        return false;
    }
}
