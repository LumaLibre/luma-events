package dev.lumas.events.utility;

import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.items.TokenSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TokenLog {

    private static final Path FOLDER = EventMain.getInstance().getDataPath().resolve("token-logs");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object LOCK = new Object();
    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();

    private TokenLog() {}

    public static void record(@NotNull TokenSource source, @NotNull String receiver, @Nullable UUID uuid,
                              int amount, @NotNull TokenExchanging.TokenType type, @Nullable String note) {
        LocalDateTime now = LocalDateTime.now();

        StringBuilder line = new StringBuilder()
                .append(TIMESTAMP.format(now))
                .append(" | ").append(source.logLabel())
                .append(" | ").append(receiver).append(" (").append(uuid == null ? "unknown uuid" : uuid).append(')')
                .append(" | +").append(amount).append(' ').append(type.name());
        if (note != null) {
            line.append(" | ").append(note);
        }
        line.append(System.lineSeparator());

        Path file = FOLDER.resolve("tokens-" + FILE_DATE.format(now) + ".log");
        String entry = line.toString();

        if (EventMain.STOPPING) { // the async scheduler won't accept new tasks during shutdown
            append(file, entry);
            return;
        }
        Executors.runAsync(() -> append(file, entry));
    }

    private static void append(Path file, String entry) {
        synchronized (LOCK) {
            try {
                Files.createDirectories(FOLDER);
                Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException exception) {
                LOGGER.error("Failed to write to the token log: " + entry.strip(), exception);
            }
        }
    }
}
