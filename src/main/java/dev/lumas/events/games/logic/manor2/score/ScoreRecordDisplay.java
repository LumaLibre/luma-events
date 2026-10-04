package dev.lumas.events.games.logic.manor2.score;

import dev.lumas.events.configurable.sectors.Manor2Definition;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public interface ScoreRecordDisplay {

    Optional<Component> display(Map<UUID, Double> participants, Manor2Definition.MessagesDefinition messages);

    private static Component participantsDisplay(Map<UUID, Double> participants) {
        List<Component> participantNames = participants
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 15D)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .map(Util::displayName)
                .toList();
        if (!participantNames.isEmpty()) {
            return participantNames.stream()
                    .collect(Component.toComponent(Component.text(", ").color(NamedTextColor.GRAY)));
        } else {
            return participants
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .map(Util::displayName)
                    .collect(Component.toComponent(Component.text(", ").color(NamedTextColor.GRAY)));
        }
    }

    record GhostKilled(UUID ghost) implements ScoreRecordDisplay {

        @Override
        public Optional<Component> display(Map<UUID, Double> participants, Manor2Definition.MessagesDefinition messages) {
            List<String> allGhostKillMessages = messages.getGhostKilledMessages();
            String serialized = Util.PREFIX + allGhostKillMessages.get(ThreadLocalRandom.current().nextInt(allGhostKillMessages.size()));

            return Optional.of(
                    MiniMessage.miniMessage().deserialize(serialized,
                            Placeholder.component("ghost", Util.displayName(ghost)),
                            Placeholder.component("participants", participantsDisplay(participants))
                    )
            );
        }
    }

    record HunterKilled(UUID hunter) implements ScoreRecordDisplay {

        @Override
        public Optional<Component> display(Map<UUID, Double> participants, Manor2Definition.MessagesDefinition messages) {
            List<String> allHunterKilledMessages = messages.getHunterKilledMessages();
            String serialized = Util.PREFIX + allHunterKilledMessages.get(ThreadLocalRandom.current().nextInt(allHunterKilledMessages.size()));

            return Optional.of(
                    MiniMessage.miniMessage().deserialize(serialized,
                            Placeholder.component("hunter", Util.displayName(hunter)),
                            Placeholder.component("participants", participantsDisplay(participants))
                    )
            );
        }
    }
}
