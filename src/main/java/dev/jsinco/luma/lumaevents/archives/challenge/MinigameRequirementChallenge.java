package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;

public class MinigameRequirementChallenge extends Challenge {

    public static final int STAGES = 15;

    public MinigameRequirementChallenge(int currentStage) {
        super(ChallengeType.PLAY_MINIGAMES, STAGES);
        this.currentStage = currentStage;
    }

    public MinigameRequirementChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.PLAY_MINIGAMES, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
