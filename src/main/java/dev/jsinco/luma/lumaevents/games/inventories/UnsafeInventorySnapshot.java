package dev.jsinco.luma.lumaevents.games.inventories;

public class UnsafeInventorySnapshot extends RuntimeException {
    public UnsafeInventorySnapshot(String message) {
        super(message);
    }
}
