package dev.lumas.events.obj.team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScarletTeam extends EventTeam {

    private static final String IDENTIFIER = "scarlet";
    private static final String DISPLAY_NAME = "<b><gradient:#dd2f40:#a81b51>Scarlet</gradient></b>";

    public ScarletTeam(Set<UUID> members, int points) {
        super(IDENTIFIER, DISPLAY_NAME, members, new HashSet<>(), points);
    }
}
