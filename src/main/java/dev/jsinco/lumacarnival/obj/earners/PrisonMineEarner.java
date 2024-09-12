package dev.jsinco.lumacarnival.obj.earners;

import dev.jsinco.lumacarnival.CarnivalToken;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrisonMineEarner implements GameEarner {

    private final UUID playerUUID;
    private int permanentAmount;
    private int amount;

    public PrisonMineEarner(UUID playerUUID, int amount) {
        this.playerUUID = playerUUID;
        this.amount = amount;
    }

    public PrisonMineEarner(UUID playerUUID, int permanentAmount, int amount) {
        this.playerUUID = playerUUID;
        this.permanentAmount = permanentAmount;
        this.amount = amount;
    }

    @Override
    public void cashIn(Player player) {
        int tokenAmount = amount / 75;
        amount = amount % 100;
        CarnivalToken.give(player, tokenAmount);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int getPermanentAmount() {
        return permanentAmount;
    }

    public void increaseAmount(int amount) {
        this.amount += amount;
        this.permanentAmount += amount;
    }


    @Override
    public String serialize() {
        return playerUUID.toString() + ";" + permanentAmount + ";" + amount;
    }


    public static PrisonMineEarner deserialize(String data) {
        String[] parts = data.split(";");
        return new PrisonMineEarner(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
