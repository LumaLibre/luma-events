package dev.jsinco.luma.challenges;

import dev.jsinco.luma.ChallengeType;

public class BringItemsChallenge extends Challenge {
    public BringItemsChallenge(int currentStage) {
        super(ChallengeType.BRING_ITEMS, 4);
        this.currentStage = currentStage;
    }
}
