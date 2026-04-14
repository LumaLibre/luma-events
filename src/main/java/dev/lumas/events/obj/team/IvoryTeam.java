package dev.lumas.events.obj.team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IvoryTeam extends EventTeam {

    private static final String IDENTIFIER = "ivory";
    private static final String DISPLAY_NAME = "<b><gradient:#c4adb0:#a48995>Ivory</gradient></b>";

    public IvoryTeam(Set<UUID> members, int points) {
        super(IDENTIFIER, DISPLAY_NAME, members, new HashSet<>(), points);
    }
}
