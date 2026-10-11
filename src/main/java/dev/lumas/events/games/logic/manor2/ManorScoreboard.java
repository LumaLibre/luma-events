package dev.lumas.events.games.logic.manor2;

import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManorScoreboard extends Scoreboard<EventPlayer> {

    private static final List<String> GHOST_KILLED_MESSAGES = List.of(
        "<white><ghost></white> got blinded to death by <red><participants></red>",
        "<white><ghost></white> got their sheet stolen by <red><participants></red> (death)",
        "<white><ghost></white> experienced to many photons by <red><participants></red> (death)"
    );
    private static final List<String> HUNTER_KILLED_MESSAGES = List.of(
        "<red><hunter></red> got haunted to death by <white><participants></white>",
        "<red><hunter></red> forgot how to breath by <white><participants></white>",
        "<red><hunter></red> completed <white><participants></white>'s bucket list (death)"
    );
    private static final List<String> ARTIFACT_FOUND_MESSAGES = List.of(
        "You found a completely worthless artifact",
        "You found a dusty relic",
        "You found something, but can't get yourself to let it go",
        "You found an artifact, but it might not be that useful"
    );
    private static final List<String> PLAYER_ARTIFACT_FOUND_MESSAGES = List.of(
        "<red><participants></red> found an artifact in perfect condition",
        "<red><participants></red> found a nice looking family heirloom",
        "<red><participants></red> found a national treasure"
    );
    private static final String SCORE_RECORD = "<green><gold>#<position></gold> is <red><scorer></red> with <gold><points> points</gold>! (<team>)";
    private static final int TOP_SCORERS = 5;
    private static final double NAMED_PARTICIPANT_MIN_PERCENTAGE = 15D;

    private final Manor2 ctx;
    private final Map<UUID, ManorTeam> scorerTeams = new ConcurrentHashMap<>();

    public ManorScoreboard(Manor2 ctx) {
        this.ctx = ctx;
    }

    public void ghostKilled(Ghost ghost, Map<UUID, Integer> shooterTicks) {
        Map<UUID, Double> attribution = new HashMap<>();
        int revealTime = ctx.getDefinition().getGhostRevealTime();
        shooterTicks.forEach((shooter, ticks) -> attribution.put(shooter, 100D * ticks / revealTime));

        award(attribution, ctx.getDefinition().getHunterKillScore());
        announce(GHOST_KILLED_MESSAGES,
            Placeholder.component("ghost", Util.displayName(ghost.getUuid())),
            Placeholder.component("participants", participantsDisplay(attribution))
        );
    }

    public void hunterKilled(GhostHunter hunter, Ghost ghost) {
        Map<UUID, Double> attribution = Map.of(ghost.getUuid(), 100D);

        award(attribution, ctx.getDefinition().getGhostKillScore());
        announce(HUNTER_KILLED_MESSAGES,
            Placeholder.component("hunter", Util.displayName(hunter.getUuid())),
            Placeholder.component("participants", participantsDisplay(attribution))
        );
    }

    public void artifactFound(GhostHunter eventPlayer) {
        Map<UUID, Double> attribution = Map.of(eventPlayer.getUuid(), 100D);

        award(attribution, ctx.getDefinition().getArtifactFoundScore());
        if (eventPlayer.getEventPlayer().getPlayer() instanceof Player player) {
            player.sendMessage(Util.color(
                Util.getRandom(ARTIFACT_FOUND_MESSAGES),
                Util.TEXT_COLOR
            ));
        }
    }

    public void playerArtifactFound(GhostHunter eventPlayer) {
        Map<UUID, Double> attribution = Map.of(eventPlayer.getUuid(), 100D);

        award(attribution, ctx.getDefinition().getArtifactFoundScore());
        announce(PLAYER_ARTIFACT_FOUND_MESSAGES,
            Placeholder.component("participants", participantsDisplay(attribution))
        );
    }

    @Override
    public void showScores(Audience audience) {
        Component border = Util.color(BORDER);

        audience.sendMessage(border);
        int position = 1;
        for (Map.Entry<EventPlayer, Integer> entry : sortedScores().entrySet()) {
            if (position > TOP_SCORERS) {
                break;
            }
            audience.sendMessage(MiniMessage.miniMessage().deserialize(SCORE_RECORD,
                Formatter.number("position", position++),
                Placeholder.component("scorer", Util.displayName(entry.getKey().getUuid())),
                Formatter.number("points", entry.getValue()),
                Placeholder.component("team", scorerTeams
                    .getOrDefault(entry.getKey().getUuid(), ManorTeam.UNDEFINED)
                    .displayName()
                )
            ));
        }
        audience.sendMessage(border);
    }

    /**
     * Used when showing the teams at game end
     * <p>
     * It's necessary to keep track of this, as roles get changed when the player dies
     *
     * @param playerUuid the scorer
     * @param manorTeam  the manorTeam of the scorer
     */
    public void registerTeam(UUID playerUuid, ManorTeam manorTeam) {
        scorerTeams.put(playerUuid, manorTeam);
    }

    private void award(Map<UUID, Double> attribution, int sharedScore) {
        attribution.forEach((uuid, percentage) -> {
            if (ctx.getRoleMap().get(uuid) instanceof Manor2Role scorer) {
                addScore(scorer.getEventPlayer(), (int) Math.round(percentage / 100D * sharedScore));
            }
        });
    }

    private void announce(List<String> messages, TagResolver... resolvers) {
        ctx.getAudience().sendMessage(Util.color(
            Util.PREFIX + Util.getRandom(messages),
            Util.TEXT_COLOR,
            resolvers
        ));
    }

    private static Component participantsDisplay(Map<UUID, Double> attribution) {
        List<UUID> named = attribution.entrySet().stream()
            .filter(entry -> entry.getValue() > NAMED_PARTICIPANT_MIN_PERCENTAGE)
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();
        if (named.isEmpty()) {
            named = attribution.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        }
        return named.stream()
            .map(Util::displayName)
            .collect(Component.toComponent(Component.text(", ").color(NamedTextColor.GRAY)));
    }
}
