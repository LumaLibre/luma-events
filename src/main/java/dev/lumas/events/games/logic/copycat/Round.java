package dev.lumas.events.games.logic.copycat;

import lombok.Getter;

@Getter
public final class Round {

    private static final int MIN_BUILD_SECONDS = 15;

    private final int number;
    private final CopycatBuild build;
    private final double hiddenFraction;
    private final double secondsPerBlock;
    private volatile long buildDeadline;

    Round(int number, CopycatBuild build, double hiddenFraction, double secondsPerBlock) {
        this.number = number;
        this.build = build;
        this.hiddenFraction = hiddenFraction;
        this.secondsPerBlock = secondsPerBlock;
    }

    void startBuilding(long deadline) {
        this.buildDeadline = deadline;
    }

    public int hiddenCount() {
        return hiddenCount(build, hiddenFraction);
    }

    public int buildSeconds() {
        return buildSeconds(hiddenCount(), secondsPerBlock);
    }

    static int hiddenCount(CopycatBuild build, double fraction) {
        int total = build.blockCount();
        return Math.clamp(Math.round(total * fraction), 1, Math.max(1, total - 1));
    }

    static int buildSeconds(int hiddenBlocks, double secondsPerBlock) {
        return Math.max(MIN_BUILD_SECONDS, (int) Math.ceil(hiddenBlocks * secondsPerBlock));
    }
}
