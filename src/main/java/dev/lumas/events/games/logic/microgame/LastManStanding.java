package dev.lumas.events.games.logic.microgame;

import dev.lumas.events.games.interfaces.Microgame;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class LastManStanding extends Microgame {

    public LastManStanding(List<EventPlayer> eventPlayers, GenericStructure structure, Runnable onEnd, long timeLimit, int padding) {
        super(eventPlayers, structure, onEnd, timeLimit, padding);
    }

    @Override
    protected void heartbeat(long remainder) {
        List<Player> alivePlayers = this.boundingBox.getEntities(Player.class);
        if (alivePlayers.size() <= 1) {
            EventPlayer winner = null;
            if (alivePlayers.size() == 1) {
                Player alivePlayer = alivePlayers.getFirst();
                winner = EventPlayerManager.getByUUID(alivePlayer.getUniqueId());
            }
            this.end(winner);
        }
    }
}
