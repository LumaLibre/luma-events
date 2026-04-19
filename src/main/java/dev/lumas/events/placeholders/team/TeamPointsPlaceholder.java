package dev.lumas.events.placeholders.team;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.PlaceholderMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.placeholders.PlaceholderManager;
import dev.lumas.events.placeholders.PlaceholderModule;
import dev.lumas.events.utility.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
        identifier = "teampoints",
        parent = PlaceholderManager.class
)
public class TeamPointsPlaceholder implements PlaceholderModule {
    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        int position = !args.isEmpty() ? Util.getInt(args.getFirst(), 1) : 1;

        List<EventTeam> eventTeams = EventTeamManager.eventTeams()
                .stream()
                .sorted((t1, t2) -> Integer.compare(t2.getPoints(), t1.getPoints()))
                .toList();

        if (eventTeams.isEmpty()) {
            return "No Teams";
        }

        if (position > eventTeams.size()) {
            return "#" + position + " Empty - 0";
        }

        EventTeam eventTeam = eventTeams.get(position - 1);
        return "# " + position + " " + eventTeam.getPlainTextDisplayName() + " - " + String.format("%,d", eventTeam.getPoints());
    }
}
