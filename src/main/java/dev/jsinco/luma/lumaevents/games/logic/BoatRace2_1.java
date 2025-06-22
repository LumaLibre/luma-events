package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

public class BoatRace2_1 extends Minigame {
    protected BoatRace2_1(String name, String description, long duration, long tickInterval, boolean async) {
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
