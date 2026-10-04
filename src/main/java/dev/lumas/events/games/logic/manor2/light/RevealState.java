package dev.lumas.events.games.logic.manor2.light;

public enum RevealState {
    HIDDEN, // vanished + invisibility
    SILHOUETTE, // glow + invisibility
    EXPOSED; // glow

    public boolean isRevealed() {
        return this != HIDDEN;
    }
}
