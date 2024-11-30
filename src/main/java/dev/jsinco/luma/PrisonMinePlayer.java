package dev.jsinco.luma;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PrisonMinePlayer {

    private final UUID uuid;
    private int totalBlocksMined;
    private int points;
    private String currentMine;

    public PrisonMinePlayer(UUID uuid) {
        this(uuid, 0, 0);
    }

    public PrisonMinePlayer(UUID uuid, int totalBlocksMined, int points) {
        this.uuid = uuid;
        this.totalBlocksMined = totalBlocksMined;
        this.points = points;
        this.currentMine = ThanksgivingEvent.getOkaeriConfig().mines.keySet().iterator().next();
    }

    public PrisonMinePlayer(UUID uuid, int totalBlocksMined, int points, String currentMine) {
        this.uuid = uuid;
        this.totalBlocksMined = totalBlocksMined;
        this.points = points;
        this.currentMine = currentMine;
    }



    public UUID getUuid() {
        return uuid;
    }


    public void addBlocksMined(int amount) {
        this.totalBlocksMined += amount;
    }

    public void setTotalBlocksMined(int totalBlocksMined) {
        this.totalBlocksMined = totalBlocksMined;
    }

    public int getTotalBlocksMined() {
        return totalBlocksMined;
    }


    public void addPoints(int points) {
        this.points += points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    public void setCurrentMine(String currentMine) {
        this.currentMine = currentMine;
    }

    public String getCurrentMine() {
        return currentMine;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    public String toString() {
        return "PrisonMinePlayer{" +
                "uuid=" + uuid +
                ", totalBlocksMined=" + totalBlocksMined +
                ", points=" + points +
                '}';
    }
}
