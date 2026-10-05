package dev.lumas.events.games.logic.manor2;

import dev.lumas.events.configurable.sectors.Manor2Definition;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

public enum Team {
    GHOSTS(Manor2Definition.MessagesDefinition::getGhosts),
    HUNTERS(Manor2Definition.MessagesDefinition::getHunters),
    UNDEFINED(ignored -> "null");

    private final Function<Manor2Definition.MessagesDefinition, String> teamDisplay;

    Team(Function<Manor2Definition.MessagesDefinition, String> teamDisplay) {
        this.teamDisplay = teamDisplay;
    }

    public Component displayName(Manor2Definition.MessagesDefinition messagesDefinition) {
        return Util.color(teamDisplay.apply(messagesDefinition));
    }
}
