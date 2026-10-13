package dev.lumas.events.games.logic.copycat;

import lombok.Getter;

@Getter
public final class Round {

    private static final int MIN_BUILD_SECONDS = 15;
    private static final int FULL_FRACTION_BLOCKS = 60;
    private static final double MIN_SIZE_SCALE = 0.4;

    private final int number;
    private final CopycatBuild build;
    private final double hiddenFraction;
    private final double secondsPerBlock;
    private final double timeScale;
    private volatile long buildDeadline;

    Round(int number, CopycatBuild build, double hiddenFraction, double secondsPerBlock, double timeScale) {
        this.number = number;
        this.build = build;
        this.hiddenFraction = hiddenFraction;
        this.secondsPerBlock = secondsPerBlock;
        this.timeScale = timeScale;
    }

    void startBuilding(long deadline) {
        this.buildDeadline = deadline;
    }

    public int hiddenCount() {
        return hiddenCount(build, hiddenFraction);
    }

    public int buildSeconds() {
        return buildSeconds(build, hiddenCount(), secondsPerBlock * timeScale);
    }

    static int hiddenCount(CopycatBuild build, double fraction) {
        int removable = build.removableCount();
        if (removable == 0) return 0;
        double scaled = fraction * sizeScale(build.blockCount());
        return Math.clamp(Math.round(removable * scaled), 1, Math.max(1, removable - 1));
    }

    static double sizeScale(int blocks) {
        if (blocks <= FULL_FRACTION_BLOCKS) return 1.0;
        return Math.max(MIN_SIZE_SCALE, Math.sqrt(FULL_FRACTION_BLOCKS / (double) blocks));
    }

    static int buildSeconds(CopycatBuild build, int hiddenBlocks, double secondsPerBlock) {
        int seconds = Math.max(MIN_BUILD_SECONDS, (int) Math.ceil(hiddenBlocks * secondsPerBlock));
        return build.getSettings().clampSeconds(seconds);
    }
}
