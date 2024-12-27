package dev.jsinco.luma.challenges;

import dev.jsinco.luma.ChallengeType;

import java.io.Serializable;

public class MazeChallenge extends Challenge implements Serializable {

    public MazeChallenge(int currentStage) {
        super(ChallengeType.MAZE, 1);
        this.currentStage = currentStage;
    }

}
