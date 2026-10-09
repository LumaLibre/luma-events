package dev.lumas.events.games.logic.manor2;

import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;

public enum ManorTeam {
    GHOSTS("<gradient:white:gray>Ghosts", "<gray>Attack hunters, stay away from light"),
    HUNTERS("<gradient:gray:red>Hunters", "<gray>Use flashlight to reveal artifacts and ghosts"),
    ARTIFACTS("<gradient:yellow:gold>Artifacts", "<gray>Hide from hunters in this region"),
    UNDEFINED("null", "");

    private static final String ASSIGNMENT_MESSAGE = "<gray>Team <team>";

    private final String displayName;
    private final String description;

    ManorTeam(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public Component displayName() {
        return Util.color(displayName);
    }

    public Title assignmentTitle() {
        return Title.title(
            MiniMessage.miniMessage().deserialize(ASSIGNMENT_MESSAGE, Placeholder.component("team", displayName())),
            Util.color(description)
        );
    }
}
