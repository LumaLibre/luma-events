package dev.lumas.events.model.team;

import java.util.HashMap;

public class IvoryTeam extends EventTeam {

    private static final String IDENTIFIER = "ivory";
    private static final String DISPLAY_NAME = "<b><gradient:#c4adb0:#a48995>Ivory</gradient></b>";
    private static final String COLOR = "#a48995";
    private static final String CHAT_COLOR = "#c4adb0";

    public IvoryTeam() {
        super(IDENTIFIER, DISPLAY_NAME, COLOR, CHAT_COLOR, new HashMap<>());
    }
}
