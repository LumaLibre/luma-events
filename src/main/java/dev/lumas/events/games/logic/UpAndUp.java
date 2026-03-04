package dev.lumas.events.games.logic;

import dev.lumas.events.configurable.sectors.UpAndUpDefinition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.obj.EventPlayer;

public final class UpAndUp extends InventoryUnifiedMinigame {

    public UpAndUp(UpAndUpDefinition def) {
        super("Up & Up", "none", 30, 2, true);
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
    }

    @Override
    protected void handleStart() {

    }

    @Override
    protected void onRunnable(long timeLeft) {

    }

    @Override
    protected void handleStop() {

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        return false;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }
}
