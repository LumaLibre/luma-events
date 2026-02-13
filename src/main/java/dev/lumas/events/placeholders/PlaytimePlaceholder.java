package dev.lumas.events.placeholders;

import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.manager.placeholder.PlaceholderInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;


@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "playtime",
        parent = PlaceholderManager.class
)
public class PlaytimePlaceholder implements PlaceholderModule {

    private static final String FORMAT = "#%d %s - %,dh";

    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        if (args.isEmpty()) return null;

        int position = Util.getInt(args.getFirst(), 1);

        List<EventPlayer> eventPlayersSorted = EventPlayerManager.eventPlayers().stream().sorted(
                (a, b) -> Long.compare(b.getSecondsPlayed(), a.getSecondsPlayed())
        ).toList();
        if (eventPlayersSorted.size() < position || position <= 0) {
            return "#" + position + " Empty - 0h";
        }

        EventPlayer eventPlayer = eventPlayersSorted.get(position - 1);
        if (eventPlayer == null) {
            return "#" + position + " Unknown - 0h";
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(eventPlayer.getUuid());
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        long hoursPlayed = TimeUnit.SECONDS.toHours(eventPlayer.getSecondsPlayed());

        return String.format(FORMAT, position, name, hoursPlayed);
    }
}
