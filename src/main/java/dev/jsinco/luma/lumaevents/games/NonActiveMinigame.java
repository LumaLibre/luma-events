package dev.jsinco.luma.lumaevents.games;

public non-sealed class NonActiveMinigame extends Minigame {

    public NonActiveMinigame() {
        super("NotARealMiniGame", "Not a real minigame", 0, 1);
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
}
