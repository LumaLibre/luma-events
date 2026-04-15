package dev.lumas.events.games.logic;

import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.model.EventPlayer;

public final class NonActiveMinigame extends Minigame {

    public NonActiveMinigame() {
        super("NotARealMinigame", "Not a real minigame", 0, 1, true, false, false, false);
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
