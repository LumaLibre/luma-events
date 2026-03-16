package dev.lumas.events.placeholders;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.PlaceholderMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.services.LeaderboardCacheService;
import dev.lumas.events.utility.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
        identifier = "playtime",
        parent = PlaceholderManager.class
)
public class PlaytimePlaceholder implements PlaceholderModule {

    private static final String FORMAT = "#%d %s - %,dh";

    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        if (args.isEmpty()) return null;

        int position = Util.getInt(args.getFirst(), 1);
        LeaderboardCacheService.PlaytimeEntry entry = LeaderboardCacheService.getPlaytimePosition(position);

        if (entry == null) {
            return "#" + position + " Empty - 0h";
        }

        long hoursPlayed = TimeUnit.SECONDS.toHours(entry.secondsPlayed());
        return String.format(FORMAT, position, entry.name(), hoursPlayed);
    }
}
