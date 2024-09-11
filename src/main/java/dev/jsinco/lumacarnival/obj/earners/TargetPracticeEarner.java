package dev.jsinco.lumacarnival.obj.earners;

import dev.jsinco.lumacarnival.CarnivalToken;
import dev.jsinco.lumacarnival.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TargetPracticeEarner implements GameEarner {

    private final UUID playerUUID;
    private int permanentAmount;
    private int totalAmount;
    private int queuedAmount;

    private Player player;


    public TargetPracticeEarner(UUID playerUUID, int totalAmount) {
        this.playerUUID = playerUUID;
        this.totalAmount = totalAmount;
        this.queuedAmount = 0;

        this.player = Bukkit.getPlayer(playerUUID);
    }

    public TargetPracticeEarner(UUID playerUUID, int permanentAmount, int totalAmount) {
        this.playerUUID = playerUUID;
        this.permanentAmount = permanentAmount;
        this.totalAmount = totalAmount;
        this.queuedAmount = 0;

        this.player = Bukkit.getPlayer(playerUUID);
    }

    @Nullable
    public Player getPlayer() {
        if (player == null) {
            player = Bukkit.getPlayer(playerUUID);
        }
        return player;
    }

    @Override
    public void cashIn(Player player) {
        int tokenAmount = totalAmount / 50;
        Util.msg(getPlayer(), "Cashing in targets for <b><gold>$tokenAmount</gold></b> tokens!");
        CarnivalToken.give(player, tokenAmount);
    }

    @NotNull
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public int getAmount() {
        return totalAmount;
    }

    @Override
    public int getPermanentAmount() {
        return permanentAmount;
    }

    public int getQueuedAmount() {
        return queuedAmount;
    }

    public void setQueuedAmount(int amount) {
        this.queuedAmount = amount;
    }

    public void increaseAmount(int amount) {
        this.totalAmount += amount;
        this.permanentAmount += amount;
    }

    public void increaseQueuedAmount(int amount) {
        this.queuedAmount += amount;
    }


    @Override
    public String serialize() {
        return playerUUID.toString() + ";" + permanentAmount + ";" + totalAmount;
    }


    public static TargetPracticeEarner deserialize(String data) {
        String[] parts = data.split(";");
        return new TargetPracticeEarner(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
