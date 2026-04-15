package dev.lumas.events.model.team;

import java.util.HashMap;

public class IvoryTeam extends EventTeam {

    private static final String IDENTIFIER = "ivory";
    private static final String DISPLAY_NAME = "<b><gradient:#c4adb0:#a48995>Ivory</gradient></b>";

    public IvoryTeam() {
        super(IDENTIFIER, DISPLAY_NAME, new HashMap<>());
    }
}
