package dev.lumas.events.games.exceptions;

public class NoAvailableMinigames extends IllegalStateException {
    public NoAvailableMinigames(String message) {
        super(message);
    }
}
