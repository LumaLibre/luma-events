package dev.lumas.events.games.logic.copycat;

public record BuildScore(
    int correct,
    int missing,
    int wrong,
    int removedGiven,
    int extra,
    int points,
    boolean perfect
) {

    public int mistakes() {
        return missing + wrong + removedGiven + extra;
    }

    public String summary() {
        if (perfect) {
            return "<green>Perfect copy!";
        }
        return "<red>" + missing + "</red> missing, <red>" + wrong + "</red> wrong, <red>" + (extra + removedGiven) + "</red> out of place";
    }
}
