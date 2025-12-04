package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;

public class ParkourChallenge extends Challenge {

    public static final int STAGES = 10;

    public ParkourChallenge(int currentStage) {
        super(ChallengeType.PARKOUR, STAGES);
        this.currentStage = currentStage;
    }

    public ParkourChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.PARKOUR, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
