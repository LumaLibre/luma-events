package dev.lumas.events.placeholders;

import dev.lumas.core.model.placeholder.AbstractPlaceholder;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.services.LeaderboardCacheService;

public interface PlaceholderModule extends AbstractPlaceholder<EventMain> {

    default String infoForMinigamePosition(MinigameConstant minigameConstant, int position) {
        LeaderboardCacheService.LeaderboardEntry entry = LeaderboardCacheService.getPosition(minigameConstant, position);
        if (entry == null) {
            return "#" + position + " Empty - 0";
        }
        return "#" + position + " " + entry.name() + " - " + String.format("%,d", entry.score());
    }
}
