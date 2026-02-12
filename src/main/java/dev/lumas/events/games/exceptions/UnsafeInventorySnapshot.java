package dev.lumas.events.games.exceptions;

public class UnsafeInventorySnapshot extends RuntimeException {
    public UnsafeInventorySnapshot(String message) {
        super(message);
    }
}
