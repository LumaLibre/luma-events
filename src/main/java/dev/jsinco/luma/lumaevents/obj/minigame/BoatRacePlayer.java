package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import io.papermc.paper.entity.TeleportFlag;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

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

    public BoatRacePlayer(EventPlayer eventPlayer, Boat boat) {
        this.eventPlayer = eventPlayer;
        this.checkpointsAchieved = new ArrayList<>();
        this.boat = boat;
    }

    public boolean hasAchievedCheckpoint(BoatRaceCheckpoint checkpoint) {
        if (this.finished) {
            return true;
        }
        // We need to make sure the racer is going in order
//        if (!checkpointsAchieved.isEmpty()) {
//            BoatRaceCheckpoint lastCheckpoint = checkpointsAchieved.getLast();
//            if (lastCheckpoint.getIndex() >= checkpoint.getIndex()) {
//                return false;
//            }
//        }
        return checkpointsAchieved.contains(checkpoint);
    }

    public void addCheckpoint(BoatRaceCheckpoint checkpoint) {
        checkpointsAchieved.add(checkpoint);
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
        return this.eventPlayer.getPlayer().equals(player);
    }

    public void teleportToLastCheckpoint() {
        if (checkpointsAchieved.isEmpty()) {
            return;
        }
        BoatRaceCheckpoint lastCheckpoint = checkpointsAchieved.getLast();
        Player player = eventPlayer.getPlayer();
        this.returningToCheckpoint = true;
        Location loc = lastCheckpoint.getCenterLocation(player.getPitch(), player.getYaw());
        // FIXME: Figure out why this doesn't work
        player.teleportAsync(
                loc,
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                TeleportFlag.EntityState.RETAIN_VEHICLE
        );
        Util.sendMsg(player, "Teleporting to last checkpoint!");
    }

    public static BoatRacePlayer of(EventPlayer eventPlayer, Boat boat) {
        return new BoatRacePlayer(eventPlayer, boat);
    }
}
