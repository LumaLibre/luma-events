package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BoatRacePlayer {

    private final EventPlayer eventPlayer;
    private final List<BoatRaceCheckpoint> checkpointsAchieved;
    private final Boat boat;

    public BoatRacePlayer(EventPlayer eventPlayer, Boat boat) {
        this.eventPlayer = eventPlayer;
        this.checkpointsAchieved = new ArrayList<>();
        this.boat = boat;
    }

    public boolean hasAchievedCheckpoint(BoatRaceCheckpoint checkpoint) {
        return checkpointsAchieved.contains(checkpoint);
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
