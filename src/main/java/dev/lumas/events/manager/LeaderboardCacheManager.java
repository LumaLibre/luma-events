package dev.lumas.events.manager;

import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public final class LeaderboardCacheManager extends AsynchronousRunnable {

    private static final LeaderboardCacheManager INSTANCE = new LeaderboardCacheManager();

    private static final long REFRESH_INTERVAL = 20 * 60L;
    private static final Map<MinigameConstant, List<LeaderboardEntry>> leaderboards = new ConcurrentHashMap<>();
    private static final List<PlaytimeEntry> playtimeLeaderboard = new CopyOnWriteArrayList<>();

    public record LeaderboardEntry(UUID uuid, String name, int score) {}
    public record PlaytimeEntry(UUID uuid, String name, long secondsPlayed) {}


    public static void start() {
        INSTANCE.repeatingAsync(0L, REFRESH_INTERVAL);
    }

    @Override
    public void accept(ScheduledTask task) {
        refresh();
    }

    private static void refresh() {
        List<EventPlayer> allPlayers = EventPlayerManager.loadAllFromDisk();

        for (MinigameConstant constant : MinigameConstant.values()) {
            List<LeaderboardEntry> sorted = allPlayers.stream()
                    .map(p -> new LeaderboardEntry(
                            p.getUuid(),
                            resolvePlayerName(p.getUuid()),
                            p.getPermanentScore(constant)
                    ))
                    .filter(e -> e.score() > 0)
                    .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                    .limit(10)
                    .toList();
            leaderboards.put(constant, sorted);
        }

        List<PlaytimeEntry> sortedPlaytime = allPlayers.stream()
                .map(p -> new PlaytimeEntry(
                        p.getUuid(),
                        resolvePlayerName(p.getUuid()),
                        p.getSecondsPlayed()
                ))
                .filter(e -> e.secondsPlayed() > 0)
                .sorted((a, b) -> Long.compare(b.secondsPlayed(), a.secondsPlayed()))
                .limit(10)
                .toList();

        playtimeLeaderboard.clear();
        playtimeLeaderboard.addAll(sortedPlaytime);
    }

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString();
    }

    @Nullable
    public static LeaderboardEntry getPosition(MinigameConstant constant, int position) {
        List<LeaderboardEntry> board = leaderboards.get(constant);
        if (board == null || position <= 0 || position > board.size()) return null;
        return board.get(position - 1);
    }

    @Nullable
    public static PlaytimeEntry getPlaytimePosition(int position) {
        if (position <= 0 || position > playtimeLeaderboard.size()) return null;
        return playtimeLeaderboard.get(position - 1);
    }
}