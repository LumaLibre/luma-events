package dev.lumas.events.games;

import dev.lumas.events.games.exceptions.UnsafeInventorySnapshot;
import dev.lumas.events.games.models.InventorySnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventorySnapshotManager {

    public static final InventorySnapshotManager INSTANCE = new InventorySnapshotManager();
    private final Map<UUID, InventorySnapshot> inventorySnapshots = new ConcurrentHashMap<>();

    public void registerSnapshot(InventorySnapshot snapshot) {
        if (!snapshot.isBackedUp()) {
            throw new UnsafeInventorySnapshot("Cannot register an inventory snapshot that has not been backed up.");
        }
        inventorySnapshots.put(snapshot.getOwner(), snapshot);
    }

    public void unregisterSnapshot(InventorySnapshot snapshot) {
        inventorySnapshots.remove(snapshot.getOwner(), snapshot);
    }

    @Nullable
    public InventorySnapshot getSnapshotByOwner(UUID owner) {
        return inventorySnapshots.get(owner);
    }

    public boolean restoreSnapshot(UUID owner) {
        InventorySnapshot snapshot = inventorySnapshots.remove(owner);
        if (snapshot != null) {
            if (!snapshot.restore()) {
                throw new IllegalStateException("Failed to restore inventory snapshot for player: " + owner);
            }
            return true;
        }
        return false;
    }
}
