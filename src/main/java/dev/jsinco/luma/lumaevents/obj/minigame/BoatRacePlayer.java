package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BoatRacePlayer {

    private final EventPlayer eventPlayer;
    private final List<BoatRaceCheckpoint> checkpointsAchieved;
    private final Boat boat;

    private boolean finished = false;
    private boolean returningToCheckpoint = false;
    private boolean usedCheckpoint = false;

    public BoatRacePlayer(EventPlayer eventPlayer, Boat boat) {
        this.eventPlayer = eventPlayer;
        this.checkpointsAchieved = new ArrayList<>();
        this.boat = boat;
    }

    public boolean hasAchievedCheckpoint(BoatRaceCheckpoint checkpoint) {
        if (this.finished) {
            return true;
        }
        return checkpointsAchieved.contains(checkpoint);
    }

    public void addCheckpoint(BoatRaceCheckpoint checkpoint) {
        checkpointsAchieved.add(checkpoint);
        this.usedCheckpoint = false;
    }

    public boolean finish(int checkpointCount) {
        if (checkpointsAchieved.size() >= checkpointCount) {
            this.finished = true;
            Bukkit.getScheduler().runTask(EventMain.getInstance(), this.boat::remove);
            return true;
        }
        return false;
    }

    public boolean is(EventPlayer eventPlayer) {
        return this.eventPlayer.equals(eventPlayer);
    }

    public boolean is(Player player) {
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return false;
        }
        return bukkitPlayer.equals(player);
    }

    public void teleportToLastCheckpoint() {
        if (checkpointsAchieved.isEmpty() || this.usedCheckpoint) {
            return;
        }
        BoatRaceCheckpoint lastCheckpoint = checkpointsAchieved.getLast();
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        this.returningToCheckpoint = true;
        Location loc = lastCheckpoint.getCenterLocation(player.getPitch(), player.getYaw());
        player.teleportAsync(loc).whenComplete((aVoid, throwable) -> {
            boat.setVelocity(new Vector(0, 0, 0));
            boat.teleportAsync(loc).whenComplete((aVoid1, throwable1) -> {
                        boat.addPassenger(player);
                        Util.sendMsg(player, "Teleported to last checkpoint!");
                        this.returningToCheckpoint = false;
                        this.usedCheckpoint = true;
            });
        });
    }

    public boolean isOnline() {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return false;
        }
        return player.isOnline();
    }

    public static BoatRacePlayer of(EventPlayer eventPlayer, Boat boat) {
        return new BoatRacePlayer(eventPlayer, boat);
    }
}
