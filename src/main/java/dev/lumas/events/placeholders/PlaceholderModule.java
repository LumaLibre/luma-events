package dev.lumas.events.placeholders;

import dev.lumas.core.model.placeholder.AbstractPlaceholder;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.obj.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(eventPlayer.getUuid());
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : offlinePlayer.getUniqueId().toString();

        return "#" + position + " " + name + " - " + String.format("%,d", eventPlayer.getPermanentScore(minigameConstant));
    }
}
