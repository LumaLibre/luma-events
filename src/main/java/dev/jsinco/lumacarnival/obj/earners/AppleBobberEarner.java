package dev.jsinco.lumacarnival.obj.earners;

import dev.jsinco.lumacarnival.CarnivalToken;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AppleBobberEarner implements GameEarner {

    private final UUID playerUUID;
    private int permanentAmount;
    private int amount;



    public AppleBobberEarner(UUID playerUUID, int amount) {
        this.playerUUID = playerUUID;
        this.amount = amount;
    }

    public AppleBobberEarner(UUID playerUUID, int permanentAmount, int amount) {
        this.playerUUID = playerUUID;
        this.permanentAmount = permanentAmount;
        this.amount = amount;
    }

    @Override
    public void cashIn(Player player) {
        int tokenAmount = amount / 15;
        amount = amount % 15;
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


    public static AppleBobberEarner deserialize(String data) {
        String[] parts = data.split(";");
        return new AppleBobberEarner(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
