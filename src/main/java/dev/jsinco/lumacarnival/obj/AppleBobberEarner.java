package dev.jsinco.lumacarnival.obj;

import java.util.UUID;

public class AppleBobberEarner {

    private final UUID playerUUID;
    private int amount;



    public AppleBobberEarner(UUID playerUUID, int amount) {
        this.playerUUID = playerUUID;
        this.amount = amount;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }


    public int getAmount() {
        return amount;
    }

    public void increaseAmount(int amount) {
        this.amount += amount;
    }
}
