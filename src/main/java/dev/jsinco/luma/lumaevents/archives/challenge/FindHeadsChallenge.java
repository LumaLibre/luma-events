package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;

public class FindHeadsChallenge extends Challenge {

    public static final int STAGES = 25;

    public FindHeadsChallenge(int currentStage) {
        super(ChallengeType.FIND_HEADS, STAGES);
        this.currentStage = currentStage;
    }

    public FindHeadsChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.FIND_HEADS, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
