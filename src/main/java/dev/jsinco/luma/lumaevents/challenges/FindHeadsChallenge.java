package dev.jsinco.luma.lumaevents.challenges;

public class FindHeadsChallenge extends Challenge {
    public FindHeadsChallenge(int currentStage) {
        super(ChallengeType.FIND_HEADS, 15);
        this.currentStage = currentStage;
    }

    public FindHeadsChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.FIND_HEADS, 15);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
