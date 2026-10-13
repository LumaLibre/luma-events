package dev.lumas.events.games.logic.manor2;

import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;

public enum ManorTeam {
    GHOSTS("<white>Ghosts", "<white>Ghost", "<gray>Attack hunters, stay away from light"),
    HUNTERS("<red>Hunters", "<red>Hunter", "<gray>Use flashlight to reveal artifacts and ghosts"),
    UNDEFINED("null", "null", "");

    private static final String ASSIGNMENT_MESSAGE = "<gray>You're a <team>";

    private final Component plural;
    private final Component singular;
    private final Component description;

    ManorTeam(String plural, String singular, String description) {
        this.plural = Util.color(plural);
        this.singular = Util.color(singular);
        this.description = Util.color(description);
    }

    public Component plural() {
        return plural;
    }

    public Component singular() {
        return singular;
    }

    public Title assignmentTitle() {
        return Title.title(
            MiniMessage.miniMessage()
                .deserialize(ASSIGNMENT_MESSAGE, Placeholder.component("team", singular)),
            description
        );
    }
}
