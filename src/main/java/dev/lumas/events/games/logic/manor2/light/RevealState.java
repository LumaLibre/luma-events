package dev.lumas.events.games.logic.manor2.light;

public enum RevealState {
    HIDDEN, // vanished + invisibility
    SILHOUETTE, // glow + invisibility
    EXPOSED; // glow

    public boolean isRevealed() {
        return this != HIDDEN;
    }

    public boolean moreRevealed(RevealState otherState) {
        if (this == SILHOUETTE) {
            return otherState != EXPOSED;
        }
        return this != HIDDEN;
    }
}
