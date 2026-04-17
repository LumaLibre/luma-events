package dev.lumas.events.model.team;

import java.util.HashMap;

public class ScarletTeam extends EventTeam {

    private static final String IDENTIFIER = "scarlet";
    private static final String DISPLAY_NAME = "<b><gradient:#dd2f40:#a81b51>Scarlet</gradient></b>";
    private static final String COLOR = "#a81b51";
    private static final String CHAT_COLOR = "#EE5968";

    public ScarletTeam() {
        super(IDENTIFIER, DISPLAY_NAME, COLOR, CHAT_COLOR, new HashMap<>());
    }
}
