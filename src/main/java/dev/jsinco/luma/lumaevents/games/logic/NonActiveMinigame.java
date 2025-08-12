package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

public final class NonActiveMinigame extends Minigame {

    public NonActiveMinigame() {
        super("NotARealMinigame", "Not a real minigame", 0, 1, false, false, false, false);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void handleStart() {

    }

    @Override
    protected void handleStop() {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    protected void onRunnable(long timeLeft) {

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        return false;
    }
}
