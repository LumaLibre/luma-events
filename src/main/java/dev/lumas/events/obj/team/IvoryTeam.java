package dev.lumas.events.obj.team;

import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

public class IvoryTeam extends EventTeam {

    private static final String IDENTIFIER = "ivory";
    private static final Component DISPLAY_NAME = Util.color("<b><gradient:#c4adb0:#a48995>Ivory</gradient></b>");

    public IvoryTeam(Set<UUID> members, int points) {
        super(IDENTIFIER, DISPLAY_NAME, members, points);
    }
}
