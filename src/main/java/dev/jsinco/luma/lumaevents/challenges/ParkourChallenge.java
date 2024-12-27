package dev.jsinco.luma.lumaevents.challenges;

public class ParkourChallenge extends Challenge {

    public ParkourChallenge(int currentStage) {
        super(ChallengeType.PARKOUR, 1);
        this.currentStage = currentStage;
    }
}
