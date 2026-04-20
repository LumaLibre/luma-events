package dev.lumas.events.placeholders.team;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.PlaceholderMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.model.team.EventTeamPlayerHandle;
import dev.lumas.events.placeholders.PlaceholderManager;
import dev.lumas.events.placeholders.PlaceholderModule;
import dev.lumas.events.utility.Util;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
        identifier = "teamplayer",
        parent = PlaceholderManager.class
)
public class TeamPlayerPointsPlaceholder implements PlaceholderModule {
    @Override
    public @Nullable String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        EventTeamManager.Provider provider = !args.isEmpty() ? Util.getEnumFromString(EventTeamManager.Provider.class, args.getFirst()) : null;
        int position = args.size() > 1 ? Util.getInt(args.get(1), 1) : 1;

        if (provider == null) {
            return "Invalid Team";
        }

        EventTeam eventTeam = EventTeamManager.getByProvider(provider);

        if (eventTeam == null) {
            return "Team Not Found";
        }

        List<EventTeamPlayerHandle> members = eventTeam.getMembers()
                .stream()
                .sorted(Comparator.comparingInt(EventTeamPlayerHandle::getPoints).reversed())
                .toList();

        if (position <= members.size()) {
            EventTeamPlayerHandle handle = members.get(position - 1);

            return "#" + position + " " + handle.getLastKnownName() + " - " + String.format("%,d", handle.getPoints());
        }

        return "#" + position + " Empty - 0";
    }
}
