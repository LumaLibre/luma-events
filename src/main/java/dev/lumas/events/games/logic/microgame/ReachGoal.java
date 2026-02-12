package dev.lumas.events.games.logic.microgame;

import dev.lumas.events.games.interfaces.Microgame;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.WorldTiedBoundingBox;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.List;

@Setter
public class ReachGoal extends Microgame {

    private WorldTiedBoundingBox goalBox = null;

    public ReachGoal(List<EventPlayer> eventPlayers, GenericStructure structure, Runnable onEnd, long timeLimit, int padding) {
        super(eventPlayers, structure, onEnd, timeLimit, padding);
    }

    @Override
    protected void heartbeat(long remainder) {
        if (goalBox == null) {
            throw new IllegalStateException("Goal box not set for ReachGoal microgame.");
        }

        for (EventPlayer eventPlayer : this.eventPlayers) {
            Player bukkitPlayer = eventPlayer.getPlayer();
            if (bukkitPlayer == null) continue;

            if (goalBox.contains(bukkitPlayer.getLocation())) {
                this.end(eventPlayer);
                break;
            }
        }
    }
}
