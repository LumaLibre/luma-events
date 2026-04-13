package dev.lumas.events.obj.team;

import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

public class ScarletTeam extends EventTeam {

    private static final String IDENTIFIER = "scarlet";
    private static final Component DISPLAY_NAME = Util.color("<b><gradient:#dd2f40:#a81b51>Scarlet</gradient></b>");

    public ScarletTeam(Set<UUID> members, int points) {
        super(IDENTIFIER, DISPLAY_NAME, members, points);
    }
}
