package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BoatRacePlayer {

    private final EventPlayer eventPlayer;
    private final List<BoatRaceCheckpoint> checkpointsAchieved;
    private final Boat boat;

    private boolean finished = false;

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
    }

    public void finish(int checkpointCount) {
        if (checkpointsAchieved.size() >= checkpointCount) {
            this.finished = true;
        }
    }

    public boolean is(EventPlayer eventPlayer) {
        return this.eventPlayer.equals(eventPlayer);
    }

    public boolean is(Player player) {
        return this.eventPlayer.getPlayer().equals(player);
    }

    public static BoatRacePlayer of(EventPlayer eventPlayer, Boat boat) {
        return new BoatRacePlayer(eventPlayer, boat);
    }
}
