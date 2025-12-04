package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;

public class BringItemsChallenge extends Challenge {

    public static final int STAGES = 4;

    public BringItemsChallenge(int currentStage) {
        super(ChallengeType.BRING_ITEMS, STAGES);
        this.currentStage = currentStage;
    }

    public BringItemsChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.BRING_ITEMS, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
