package dev.jsinco.luma.lumaevents.challenges;

import java.io.Serializable;

public class MazeChallenge extends Challenge implements Serializable {

    public MazeChallenge(int currentStage) {
        super(ChallengeType.MAZE, 1);
        this.currentStage = currentStage;
    }


    public MazeChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.MAZE, 1);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }
}
