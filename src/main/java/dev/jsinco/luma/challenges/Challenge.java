package dev.jsinco.luma.challenges;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public abstract class Challenge implements Serializable {

    protected final ChallengeType type;
    protected final int stages;

    protected int currentStage;
    protected boolean assigned;

    protected Challenge(ChallengeType type, int stages) {
        this.type = type;
        this.stages = stages;
    }

    public boolean isCompleted() {
        return currentStage >= stages;
    }

    public boolean addStage(int amount) {
        currentStage += amount;
        return isCompleted();
    }
}
