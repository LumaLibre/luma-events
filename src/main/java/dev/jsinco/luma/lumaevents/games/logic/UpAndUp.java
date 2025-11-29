package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.configurable.sectors.UpAndUpDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

public final class UpAndUp extends InventoryUnifiedMinigame {

    public UpAndUp(UpAndUpDefinition def) {
        super("Up & Up", "none", 30, 2, false);
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
    }

    @Override
    protected void handleTokens() {

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
}
