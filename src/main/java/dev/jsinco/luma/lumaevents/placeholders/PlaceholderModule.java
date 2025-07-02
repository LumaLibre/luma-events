package dev.jsinco.luma.lumaevents.placeholders;

import dev.jsinco.luma.lumacore.manager.placeholder.AbstractPlaceholder;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

import java.util.List;

public interface PlaceholderModule extends AbstractPlaceholder<EventMain> {

    default String infoForMinigamePosition(MinigameConstant minigameConstant, int position) {
        List<EventPlayer> eventPlayersSorted = EventPlayerManager.eventPlayers().stream().sorted(
                        (a, b) -> Integer.compare(b.getPermanentScore(minigameConstant), a.getPermanentScore(minigameConstant))
                ).toList();
        if (eventPlayersSorted.size() < position || position <= 0) {
            return "#" + position + " Empty - 0";
        }

        EventPlayer eventPlayer = eventPlayersSorted.get(position - 1);
        if (eventPlayer == null) {
            return "#" + position + " Unknown - 0";
        }
        return "#" + position + " " + eventPlayer.getName() + " - " + String.format("%,d", eventPlayer.getPermanentScore(minigameConstant));
    }
}
