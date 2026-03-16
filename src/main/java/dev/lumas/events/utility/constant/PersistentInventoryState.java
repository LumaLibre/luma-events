package dev.lumas.events.utility.constant;

public enum PersistentInventoryState {
    MAIN_INVENTORY,
    TRANSIENT_INVENTORY;

    public PersistentInventoryState opposite() {
        return this == TRANSIENT_INVENTORY ? MAIN_INVENTORY : TRANSIENT_INVENTORY;
    }
}
