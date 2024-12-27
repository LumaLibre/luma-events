package dev.jsinco.luma.challenges;

import dev.jsinco.luma.ChallengeType;

public class ParkourChallenge extends Challenge {

    public ParkourChallenge(int currentStage) {
        super(ChallengeType.PARKOUR, 1);
        this.currentStage = currentStage;
    }
}
