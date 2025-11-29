package dev.jsinco.luma.lumaevents.games.logic.microgame;

import dev.jsinco.luma.lumaevents.games.interfaces.Microgame;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.Structure;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import lombok.Builder;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.List;

@Setter
@Builder
public class ReachGoal extends Microgame {

    private WorldTiedBoundingBox goalBox = null;

    public ReachGoal(List<EventPlayer> eventPlayers, Structure structure, Runnable onEnd, long timeLimit, int padding) {
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
