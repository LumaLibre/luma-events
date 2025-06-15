package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;

public class SumoMinigame extends InventoryUnifiedMinigame {
    public SumoMinigame(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
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
