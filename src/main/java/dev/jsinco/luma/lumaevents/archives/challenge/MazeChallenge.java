package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;

import java.io.Serializable;

public class MazeChallenge extends Challenge implements Serializable {

    public static final int STAGES = 1;

    public MazeChallenge(int currentStage) {
        super(ChallengeType.MAZE, STAGES);
        this.currentStage = currentStage;
    }


    public MazeChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.MAZE, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
