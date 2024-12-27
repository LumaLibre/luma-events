package dev.jsinco.luma.lumaevents.challenges;

public class BringItemsChallenge extends Challenge {
    public BringItemsChallenge(int currentStage) {
        super(ChallengeType.BRING_ITEMS, 4);
        this.currentStage = currentStage;
    }
}
