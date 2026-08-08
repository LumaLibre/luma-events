package dev.lumas.events.items;

import org.jetbrains.annotations.NotNull;

public record TokenSource(@NotNull Type type, @NotNull String detail) {

    public enum Type {
        COMMAND,
        MINIGAME,
        JOB,
        TOWN_INVITE
    }

    public static TokenSource command(@NotNull String senderName) {
        return new TokenSource(Type.COMMAND, senderName);
    }

    public static TokenSource minigame(@NotNull String minigameName) {
        return new TokenSource(Type.MINIGAME, minigameName);
    }

    public static TokenSource job(@NotNull String jobName) {
        return new TokenSource(Type.JOB, jobName);
    }

    public static TokenSource townInvite(@NotNull String detail) {
        return new TokenSource(Type.TOWN_INVITE, detail);
    }

    public String chatLabel() {
        return switch (this.type) {
            case MINIGAME -> "Minigame";
            case JOB -> this.detail;
            case COMMAND -> "Other";
            case TOWN_INVITE -> "Town invite";
        };
    }

    public String logLabel() {
        return this.type.name() + " (" + this.detail + ")";
    }
}
