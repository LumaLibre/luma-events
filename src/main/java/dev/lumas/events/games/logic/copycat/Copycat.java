package dev.lumas.events.games.logic.copycat;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.game.CopycatDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.exceptions.MinigameException;
import dev.lumas.events.games.interfaces.HowToPlayDialogue;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.logic.copycat.roles.Builder;
import dev.lumas.events.games.logic.copycat.roles.CopycatRole;
import dev.lumas.events.games.logic.copycat.roles.Spectator;
import dev.lumas.events.games.logic.copycat.roles.TransientSpectator;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.CopycatTokenFormula;
import dev.lumas.events.games.tokenformula.ScoreRangeTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.vector.BlockPos;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Copycat extends InventoryUnifiedMinigame {

    private static final String[] SPLASH = {
        "Photographic memory required.",
        "Blink, and you'll miss it!",
        "No peeking!",
        "Faster, faster!"
    };
    static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();
    private static final String SCHEM_FOLDER = "copycat";
    private static final long TICK_INTERVAL = 2;
    private static final int PREVIEW_SECONDS = 16;
    private static final double MIN_HIDDEN_FRACTION = 0.25;
    private static final double MAX_HIDDEN_FRACTION = 0.50;
    private static final int HARDEST_ROUND = 10;
    private static final double TIME_SCALE_AT_ROUND_TEN = 1.9 / 2.1;
    private static final double TIME_SCALE_PER_ROUND = (1.0 - TIME_SCALE_AT_ROUND_TEN) / 9.0;
    private static final double MIN_TIME_SCALE = 0.5;
    private static final int MAX_FASTERS = 3;
    private static final int WORST_SHOWCASED = 2;
    private static final int BEST_SHOWCASED = 2;
    private static final double START_REQUIRED_ACCURACY = 0.30;
    private static final double MIN_REQUIRED_STEP = 0.01;
    private static final double MAX_REQUIRED_STEP = 0.03;
    public static final Title.Times QUICK_TITLE = Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1200), Duration.ofMillis(300));
    private static final Title.Times SHORT_TITLE = Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(400));
    private static final double MAX_REQUIRED_ACCURACY = 0.75;
    private static final long SHOWCASE_SECONDS = 7;
    private static final long INTERMISSION_SECONDS = 5;
    private static final long END_DELAY_SECONDS = 5;
    private static final long SETUP_ALLOWANCE_SECONDS = 45;
    private static final long CLEANUP_TIMEOUT_SECONDS = 30;
    public static final int CORRECT_POINTS = 10;
    public static final int WRONG_PENALTY = 5;
    public static final int EXTRA_PENALTY = 5;
    public static final int REMOVED_GIVEN_PENALTY = 5;
    public static final int PERFECT_BONUS = 50;
    public static final int SPEED_POINTS_PER_SECOND = 1;

    private final CopycatDefinition definition;
    private final Location lobby;
    private final Deque<CopycatBuild> pool;
    private final int maxFootprint;
    private final int maxHeight;
    @Getter
    private final MinigameRoleMap<CopycatRole> roleMap = new MinigameRoleMap<>(CopycatRole::cleanup);
    @Getter
    private final Scoreboard<EventPlayer> scoreboard = new Scoreboard<>();
    private final ScoreRangeTokenFormula tokenFormula = CopycatTokenFormula.formula();
    private final Map<UUID, Integer> roundsSurvived = new ConcurrentHashMap<>();
    private final Map<UUID, BuildScore> roundScores = new ConcurrentHashMap<>();
    private final Deque<Showcased> showcase = new ConcurrentLinkedDeque<>();
    private final List<Builder> eliminated = new CopyOnWriteArrayList<>();
    private final AtomicBoolean ended = new AtomicBoolean();
    @Getter
    private volatile Arena arena;
    private volatile Phase phase = Phase.SETUP;
    private volatile long phaseEndsAt;
    private volatile Round round;
    private volatile List<Builder> roundBuilders = List.of();
    private volatile int roundNumber;
    private volatile CountdownBossBar bar;

    public Copycat(CopycatDefinition definition) {
        super("Copycat", Util.getRandom(SPLASH), 0, TICK_INTERVAL, true, true, false, false);
        Preconditions.checkState(definition.getLobby() != null, "Copycat needs a lobby");
        Preconditions.checkState(definition.getOrigin() != null, "Copycat needs an arena origin");
        this.definition = definition;
        this.lobby = definition.getLobby();

        File folder = EventMain.getInstance().getDataPath().resolve("schematics").resolve(SCHEM_FOLDER).toFile();
        Map<String, BuildSettings> overrides = new HashMap<>();
        for (CopycatDefinition.BuildSettings entry : definition.getBuilds()) {
            overrides.put(entry.getSchem(), BuildSettings.from(entry));
        }
        List<CopycatBuild> builds = CopycatBuild.loadAll(folder, overrides);
        if (builds.isEmpty()) {
            throw new MinigameException("No schematics " + folder.getPath());
        }
        Collections.shuffle(builds, RANDOM);
        this.pool = new ArrayDeque<>(builds);
        this.maxFootprint = builds.stream().mapToInt(build -> Math.max(build.getSizeX(), build.getSizeZ())).max().orElseThrow();
        this.maxHeight = builds.stream().mapToInt(CopycatBuild::getSizeY).max().orElseThrow();
        this.setDuration(duration(definition, builds));
    }

    private static long duration(CopycatDefinition definition, List<CopycatBuild> builds) {
        int longestBuild = builds.stream()
            .mapToInt(build -> Round.buildSeconds(build, Round.hiddenCount(build, MAX_HIDDEN_FRACTION), definition.getBuildSecondsPerBlock()))
            .max()
            .orElseThrow();
        long roundSeconds = PREVIEW_SECONDS + longestBuild
            + (BEST_SHOWCASED + WORST_SHOWCASED) * SHOWCASE_SECONDS + INTERMISSION_SECONDS + 10;
        return TimeUnit.SECONDS.toMillis(SETUP_ALLOWANCE_SECONDS
            + roundSeconds * Math.max(1, definition.getMaxRounds())
            + END_DELAY_SECONDS);
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected HowToPlayDialogue howToPlay() {
        return HowToPlayDialogue.builder()
            .title("<b><gradient:#331D4D:#611e4e:#9f2e51>Copycat")
            .sections(List.of(
                new HowToPlayDialogue.ItemSection(ItemType.BRICKS.createItemStack(), "<gray><b>Premise</b></gray>", """
                    The goal of this minigame is to replicate
                    a specific build based on memory in a
                    short period of time. The timer gets
                    shorter as the game goes on.
                    """),
                new HowToPlayDialogue.ItemSection(ItemType.CRAFTER.createItemStack(), "gray><b>Memorize</b></gray>", """
                    You have <gold>16 seconds</gold> to memorize
                    a given build. After that, random blocks that are exposed to
                    air will be removed from the build and it will be your job to
                    fill them in.
                    """),
                new HowToPlayDialogue.ItemSection(ItemType.SKELETON_SKULL.createItemStack(), "<gray><b>Eliminations</b></gray>", """
                    Each round, you have to get a certain percentage
                    of the build correct. If you don't, you're out.
                    Eliminated players become spectators.
                    """)
            )).build();
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.teleportOnJoin(player, lobby);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport, boolean wasAfk) {
        CopycatRole role = roleMap.remove(participant.getUuid());
        if (role != null) {
            role.cleanup();
        }
        showcase.removeIf(entry -> entry.builder().getUuid().equals(participant.getUuid()));
        eliminated.removeIf(builder -> builder.getUuid().equals(participant.getUuid()));
        participant.teleportAsync(lobby);
        return super.removeParticipant(participant, doTeleport, wasAfk);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = scoreboard.getScore(participant);
        boolean winner = participant.equals(scoreboard.getTopScorer());
        tokenFormula.giveTokens(participant, Couple.of(roundsSurvived.getOrDefault(participant.getUuid(), 0), winner));
        participant.addPermanentScore(MinigameConstant.COPYCAT, Math.max(0, score));
    }

    @Override
    protected void handleStart() {
        Collections.shuffle(participants);
        this.arena = new Arena(definition.getOrigin(), participants.size(), maxFootprint, maxHeight);
        this.boundingBox = arena.getBounds();
        scoreboard.addScorers(List.copyOf(participants));
        List<Plot> plots = arena.getPlots();
        for (int i = 0; i < participants.size(); i++) {
            roleMap.put(new Builder(this, participants.get(i), plots.get(i)));
        }

        this.sendAudienceActionBar("Building the arena...");
        arena.build().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to build the Copycat arena", throwable);
                this.stop();
                return;
            }
            if (!this.active || this.stopping) return;
            for (CopycatRole role : roleMap) {
                role.play();
            }
            this.startRound();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;
        long now = System.currentTimeMillis();
        switch (phase) {
            case PREVIEW -> {
                if (now >= phaseEndsAt) beginBuilding();
            }
            case BUILDING -> {
                if (now >= phaseEndsAt || presentRoundBuilders().stream().allMatch(Builder::isLocked)) beginScoring();
            }
            case SHOWCASE -> {
                if (now >= phaseEndsAt) showNext();
            }
            case INTERMISSION -> {
                if (now >= phaseEndsAt) startRound();
            }
            default -> {
            }
        }
        for (CopycatRole role : roleMap) {
            role.tick();
        }
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.finish();
        }
    }

    @Override
    protected void handleStop() {
        this.phase = Phase.FINISHED;
        this.stopBar();

        List<CompletableFuture<Void>> cleanups = new ArrayList<>();
        for (CopycatRole role : roleMap) {
            try {
                cleanups.add(role.cleanup());
            } catch (Throwable throwable) {
                cleanups.add(CompletableFuture.failedFuture(throwable));
            }
        }

        Executors.teleportGroupAsync(participants, lobby);

        Arena toClear = this.arena;
        CompletableFuture.allOf(cleanups.toArray(CompletableFuture[]::new))
            .orTimeout(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                LOGGER.error("Not every Copycat player finished cleaning up", throwable);
                return null;
            })
            .thenCompose(ignored -> toClear == null ? CompletableFuture.completedFuture(null) : toClear.clear())
            .exceptionally(throwable -> {
                LOGGER.error("Failed to clean up the Copycat arena", throwable);
                return null;
            });
    }

    public boolean confinesBuilders() {
        return phase != Phase.SHOWCASE && phase != Phase.FINISHED;
    }

    private List<Builder> builders() {
        return roleMap.getMatching(Builder.class);
    }

    private List<Builder> presentRoundBuilders() {
        return roundBuilders.stream()
            .filter(builder -> roleMap.containsKey(builder.getUuid()))
            .toList();
    }

    private boolean controls(Builder builder) {
        CopycatRole current = roleMap.get(builder.getUuid());
        return current == builder || (current instanceof TransientSpectator transientSpectator && transientSpectator.owns(builder));
    }

    private static double hiddenFraction(int number) {
        double progress = Math.min(1.0, (number - 1) / (double) (HARDEST_ROUND - 1));
        return MIN_HIDDEN_FRACTION + (MAX_HIDDEN_FRACTION - MIN_HIDDEN_FRACTION) * progress;
    }

    private static double timeScale(int number) {
        return Math.max(MIN_TIME_SCALE, 1.0 - TIME_SCALE_PER_ROUND * (number - 1));
    }

    static double requiredAccuracy(int number) {
        double required = START_REQUIRED_ACCURACY;
        for (int round = 2; round <= number && required < MAX_REQUIRED_ACCURACY; round++) {
            double progress = Math.min(1.0, (round - 2) / (double) (HARDEST_ROUND - 2));
            required += MIN_REQUIRED_STEP + (MAX_REQUIRED_STEP - MIN_REQUIRED_STEP) * progress;
        }
        return Math.min(MAX_REQUIRED_ACCURACY, required);
    }

    private static String percent(double fraction) {
        return (int) Math.round(fraction * 100) + "%";
    }

    private static double accuracy(Builder builder, BuildScore score) {
        return score.correct() / (double) Math.max(1, builder.hiddenCount());
    }

    private static String faster(int number) {
        String words = String.join(", ", Collections.nCopies(Math.min(MAX_FASTERS, number - 1), "faster"));
        return Character.toUpperCase(words.charAt(0)) + words.substring(1) + "!";
    }

    private boolean hasNextRound() {
        List<Builder> builders = builders();
        boolean lastOneStanding = builders.size() == 1 && roleMap.size() > 1;
        return !builders.isEmpty() && !lastOneStanding && !pool.isEmpty() && roundNumber < definition.getMaxRounds();
    }

    private synchronized void startRound() {
        if (!this.active || this.stopping || phase == Phase.FINISHED) return;
        if (!hasNextRound()) {
            this.finish();
            return;
        }

        List<Builder> builders = builders();
        int number = ++roundNumber;
        Round next = new Round(number, pool.poll().rotated(RANDOM.nextInt(4)), hiddenFraction(number), definition.getBuildSecondsPerBlock(), timeScale(number));
        this.round = next;
        this.roundBuilders = List.copyOf(builders);
        this.phase = Phase.SETUP;

        List<CompletableFuture<Void>> pastes = new ArrayList<>(builders.size());
        for (Builder builder : builders) {
            pastes.add(builder.assign(next));
        }
        CompletableFuture.allOf(pastes.toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to paste the Copycat build " + next.getBuild().getName(), throwable);
                this.stop();
                return;
            }
            if (!this.active || this.stopping) return;
            this.beginPreview();
        });
    }

    private synchronized void beginPreview() {
        this.phase = Phase.PREVIEW;
        this.phaseEndsAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PREVIEW_SECONDS);
        this.showBar("<yellow><b>Preview</b></yellow> <gray>|</gray> <white>%ss", BossBar.Color.YELLOW, PREVIEW_SECONDS);
        this.sendAudienceTitle("<yellow><b>Round " + roundNumber, "<gray>Memorize the build!", QUICK_TITLE);
        this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
    }

    private synchronized void beginBuilding() {
        if (phase != Phase.PREVIEW) return;
        Round current = this.round;
        int buildSeconds = current.buildSeconds();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(buildSeconds);
        current.startBuilding(deadline);
        this.phase = Phase.BUILDING;
        this.phaseEndsAt = deadline;
        for (Builder builder : presentRoundBuilders()) {
            builder.uncover();
        }
        this.showBar("<green><b>Build!</b></green> <gray>|</gray> <white>%ss", BossBar.Color.GREEN, buildSeconds);
        this.sendAudienceTitle("<green><b>Go!", "<gray>" + current.hiddenCount() + " blocks went missing, " + buildSeconds + "s to get " + percent(requiredAccuracy(roundNumber)) + " right", QUICK_TITLE);
    }

    private synchronized void beginScoring() {
        if (phase != Phase.BUILDING) return;
        this.phase = Phase.SCORING;
        this.stopBar();
        this.sendAudienceTitle("<gold><b>Time!", "<gray>Checking builds...", QUICK_TITLE);
        this.roundScores.clear();

        Round current = this.round;
        List<CompletableFuture<Void>> reads = new ArrayList<>();
        for (Builder builder : presentRoundBuilders()) {
            reads.add(builder.readBuildArea().thenAccept(actual ->
                roundScores.put(builder.getUuid(), builder.score(actual, current.getBuildDeadline()))));
        }
        CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to score Copycat builds", throwable);
                this.stop();
                return;
            }
            if (!this.active || this.stopping) return;
            this.applyScores();
        });
    }

    private synchronized void applyScores() {
        List<Builder> builders = presentRoundBuilders();
        List<Builder> scored = new ArrayList<>();
        for (Builder builder : builders) {
            BuildScore score = roundScores.get(builder.getUuid());
            if (score == null) continue;
            scored.add(builder);
            scoreboard.addScore(builder.getEventPlayer(), score.points());
            builder.getEventPlayer().sendMessage("<gold>" + score.points() + " points</gold> <gray>-</gray> " + score.summary());
        }

        double required = requiredAccuracy(roundNumber);
        for (Builder builder : scored) {
            double accuracy = accuracy(builder, roundScores.get(builder.getUuid()));
            String verdict = accuracy >= required ? "<green>You Made it!" : "<red>You didn't make it...";
            builder.getEventPlayer().sendMessage("You got <gold>" + percent(accuracy) + "</gold> of the missing blocks right (needed " + percent(required) + ") — " + verdict);
        }
        List<Builder> losers = scored.stream()
            .filter(builder -> accuracy(builder, roundScores.get(builder.getUuid())) < required)
            .sorted(Comparator.<Builder>comparingDouble(builder -> accuracy(builder, roundScores.get(builder.getUuid())))
                .thenComparingInt(builder -> roundScores.get(builder.getUuid()).points()))
            .toList();
        if (!losers.isEmpty() && losers.size() == builders.size()) {
            this.sendAudienceMessage("<yellow>Nobody reached " + percent(required) + "!</yellow> Everyone gets another chance.");
            losers = List.of();
        }

        for (Builder builder : builders) {
            if (!losers.contains(builder)) {
                roundsSurvived.merge(builder.getUuid(), 1, Integer::sum);
            }
        }

        eliminated.clear();
        eliminated.addAll(losers);
        showcase.clear();
        List<Builder> cut = losers;
        List<Builder> best = scored.stream()
            .filter(builder -> !cut.contains(builder))
            .sorted(Comparator.<Builder>comparingDouble(builder -> accuracy(builder, roundScores.get(builder.getUuid())))
                .thenComparingInt(builder -> roundScores.get(builder.getUuid()).points())
                .reversed())
            .limit(BEST_SHOWCASED)
            .toList();
        for (Builder builder : best) {
            showcase.addLast(new Showcased(builder, true));
        }
        List<Builder> worst = losers.subList(0, Math.min(WORST_SHOWCASED, losers.size()));
        for (int i = worst.size() - 1; i >= 0; i--) {
            showcase.addLast(new Showcased(worst.get(i), false));
        }

        if (losers.isEmpty() && !builders.isEmpty() && scored.size() == builders.size()) {
            this.sendAudienceMessage("<green>Nobody was eliminated this round.");
        } else if (!losers.isEmpty()) {
            this.sendAudienceMessage("<red>" + losers.size() + "</red> " + (losers.size() == 1 ? "player" : "players") + " didn't reach " + percent(required) + ".");
        }
        if (showcase.isEmpty()) {
            this.beginIntermission();
            return;
        }
        this.phase = Phase.SHOWCASE;
        this.showNext();
    }

    private synchronized void showNext() {
        if (phase != Phase.SHOWCASE) return;
        Showcased entry = showcase.pollFirst();
        if (entry == null) {
            for (Builder loser : eliminated) {
                this.eliminate(loser);
            }
            eliminated.clear();
            this.beginIntermission();
            return;
        }

        Builder next = entry.builder();
        this.phaseEndsAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SHOWCASE_SECONDS);
        BuildScore score = roundScores.get(next.getUuid());
        String accuracy = score == null ? "?" : percent(accuracy(next, score));
        String points = score == null ? "?" : String.valueOf(score.points());
        Executors.teleportGroupAsync(participants, next.getPlot().getOverview());
        if (entry.best()) {
            this.sendAudienceTitle("<green>" + next.getName() + "'s build", "<gold>" + accuracy + " correct</gold> <gray>|</gray> <gold>" + points + " points", SHORT_TITLE);
            this.playAudienceSound(Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        } else {
            String summary = score == null ? "" : " <gray>|</gray> " + score.summary();
            this.sendAudienceTitle("<red>" + next.getName() + "'s build", "<gold>" + accuracy + " correct</gold>" + summary, SHORT_TITLE);
            this.playAudienceSound(Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        }
    }

    private record Showcased(Builder builder, boolean best) {
    }

    private void eliminate(Builder builder) {
        if (!controls(builder)) return;
        CopycatRole spectator = roleMap.swapRole(builder.getEventPlayer(), () -> new Spectator(this, builder.getEventPlayer()));
        spectator.play();
        this.sendAudienceMessage("<red>" + builder.getName() + "</red> was eliminated.");
    }

    public void submit(Builder builder) {
        if (phase != Phase.BUILDING || roleMap.get(builder.getUuid()) != builder) return;
        builder.lock();
        Round current = this.round;
        builder.readBuildArea().thenAccept(actual -> {
            BuildScore score = builder.score(actual, current.getBuildDeadline());
            int hidden = Math.max(1, builder.hiddenCount());
            int accuracy = (int) Math.round(100.0 * score.correct() / hidden);
            builder.getEventPlayer().sendMessage("Build submitted with <gold>" + accuracy + "% accuracy</gold> <gray>(" + score.correct() + "/" + hidden + " blocks back where they belong)</gray>");
            if (!this.active || this.stopping || phase != Phase.BUILDING || roleMap.get(builder.getUuid()) != builder)
                return;
            roleMap.swapRole(builder.getEventPlayer(), () -> new TransientSpectator(this, builder)).play();
        });
    }

    private synchronized void beginIntermission() {
        this.phase = Phase.INTERMISSION;
        this.phaseEndsAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(INTERMISSION_SECONDS);
        for (Builder builder : roundBuilders) {
            if (!controls(builder)) continue;
            if (roleMap.get(builder.getUuid()) != builder) {
                roleMap.swapRole(builder.getEventPlayer(), () -> builder);
            }
            builder.clearBuildArea();
            builder.clearInventory();
            builder.play();
        }
        //scoreboard.showScores(this.audience); // TODO:
        if (hasNextRound()) {
            double current = requiredAccuracy(roundNumber);
            double next = requiredAccuracy(roundNumber + 1);
            String subtitle = next > current
                ? "<gray>Less time, and you now need <gold>" + percent(next) + "</gold> right"
                : "<gray>Less time per block next round";
            if (next > current) {
                this.sendAudienceMessage("Get at least <gold>" + percent(next) + "</gold> of the missing blocks right to survive this round.");
            }
            this.sendAudienceTitle("<gold><b>" + faster(roundNumber + 1), subtitle, SHORT_TITLE);
            this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.8f + 0.1f * Math.min(MAX_FASTERS, roundNumber));
        }
    }

    private void finish() {
        if (!this.active || this.stopping || !ended.compareAndSet(false, true)) return;
        this.phase = Phase.FINISHED;
        this.stopBar();
        scoreboard.handleGameEnd(this.audience, null);
        CountdownBossBar.builder()
            .title("<red><b>Game Over</b></red> <gray>|</gray> <white>%ss")
            .audience(this.audience)
            .color(BossBar.Color.RED)
            .seconds(END_DELAY_SECONDS)
            .callback(this::stop)
            .build()
            .start();
    }

    private void showBar(String title, BossBar.Color color, float seconds) {
        this.stopBar();
        this.bar = CountdownBossBar.builder()
            .title(title)
            .color(color)
            .seconds(seconds)
            .audience(this.audience)
            .build()
            .start();
    }

    private void stopBar() {
        CountdownBossBar current = this.bar;
        if (current != null) {
            current.stop(false);
            this.bar = null;
        }
    }

    @Nullable
    private Builder editingBuilder(Player player, Block block) {
        Builder builder = roleMap.as(player.getUniqueId(), Builder.class);
        if (builder == null || phase != Phase.BUILDING || builder.isLocked()) {
            return null;
        }
        return builder.getPlot().inBuildArea(BlockPos.of(block)) ? builder : null;
    }

    private boolean inArena(Block block) {
        Arena current = this.arena;
        return current != null && current.contains(block.getLocation());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        if (editingBuilder(event.getPlayer(), event.getBlock()) == null) {
            event.setCancelled(true);
            return;
        }
        event.setInstaBreak(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        Builder builder = editingBuilder(event.getPlayer(), event.getBlock());
        if (builder == null) {
            event.setCancelled(true);
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        Block block = event.getBlock();
        BlockData data = block.getBlockData();
        BlockPos pos = BlockPos.of(block);
        this.returnItems(event.getPlayer(), Placed.of(block));
        builder.onBroken(pos);
        if (CopycatBuild.isTallPair(data)) {
            BlockPos partnerPos = CopycatBuild.partnerOf(pos, data);
            Block partner = block.getWorld().getBlockAt(partnerPos.x(), partnerPos.y(), partnerPos.z());
            Material bucket = CopycatBuild.extraBucket(partner.getBlockData());
            if (bucket != null) {
                event.getPlayer().getInventory().addItem(new ItemStack(bucket));
            }
            partner.setBlockData(ArenaBlocks.AIR, false);
            builder.onBroken(partnerPos);
        }
    }

    private void returnItems(Player player, Placed placed) {
        player.getInventory().addItem(CopycatBuild.itemFor(placed.data(), placed.detail()));
        Material bucket = CopycatBuild.extraBucket(placed.data());
        if (bucket != null) {
            player.getInventory().addItem(new ItemStack(bucket));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        Builder builder = editingBuilder(event.getPlayer(), event.getBlockPlaced());
        if (builder == null) {
            event.setCancelled(true);
            Builder role = roleMap.as(event.getPlayer().getUniqueId(), Builder.class);
            if (role != null && phase == Phase.BUILDING) {
                event.getPlayer().sendActionBar(Util.color(role.isLocked()
                    ? "<green>Your build is locked in"
                    : "<red>Stay inside the marked build area"));
            }
            return;
        }
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState replaced : multi.getReplacedBlockStates()) {
                if (!builder.getPlot().inBuildArea(BlockPos.of(replaced.getBlock()))) {
                    event.setCancelled(true);
                    event.getPlayer().sendActionBar(Util.color("<red>Stay inside the marked build area"));
                    return;
                }
            }
            for (BlockState replaced : multi.getReplacedBlockStates()) {
                builder.onPlaced(BlockPos.of(replaced.getBlock()), settle(replaced.getBlock()));
            }
            return;
        }
        builder.onPlaced(BlockPos.of(event.getBlockPlaced()), settle(event.getBlockPlaced()));
    }

    private static Placed settle(Block block) {
        BlockData data = block.getBlockData();
        BlockData settled = CopycatBuild.settle(data);
        if (settled != data) {
            block.setBlockData(settled, false);
        }
        return new Placed(settled, Placed.isTile(settled) ? TileDetail.of(block.getState()) : null);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCauldronChange(CauldronLevelChangeEvent event) {
        this.ensureNotIllegal();
        if (!inArena(event.getBlock())) return;
        Builder builder = event.getEntity() instanceof Player player && isParticipant(player)
            ? editingBuilder(player, event.getBlock())
            : null;
        boolean bucket = event.getReason() == CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY
            || event.getReason() == CauldronLevelChangeEvent.ChangeReason.BUCKET_FILL;
        if (builder == null || !bucket) {
            event.setCancelled(true);
            return;
        }
        builder.onPlaced(BlockPos.of(event.getBlock()), CopycatBuild.settle(event.getNewState().getBlockData()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        Block block = event.getBlock();
        Builder builder = editingBuilder(event.getPlayer(), block);
        if (builder == null) {
            event.setCancelled(true);
            return;
        }
        BlockData data = block.getBlockData();
        if (CopycatBuild.isCauldron(data.getMaterial())) return;
        if (event.getBucket() == Material.WATER_BUCKET && data instanceof Waterlogged waterlogged && !data.getMaterial().isAir()) {
            Waterlogged after = (Waterlogged) waterlogged.clone();
            after.setWaterlogged(true);
            builder.onPlaced(BlockPos.of(block), after);
            return;
        }
        Material fluid = event.getBucket() == Material.WATER_BUCKET ? Material.WATER
            : event.getBucket() == Material.LAVA_BUCKET ? Material.LAVA : null;
        if (fluid == null || !data.getMaterial().isAir()) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Util.color("<red>That won't fit there"));
            return;
        }
        builder.onPlaced(BlockPos.of(block), fluid.createBlockData());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        this.ensureNotIllegal();
        if (!isParticipant(event.getPlayer())) return;
        Block block = event.getBlock();
        Builder builder = editingBuilder(event.getPlayer(), block);
        BlockData data = block.getBlockData();
        if (builder == null) {
            event.setCancelled(true);
            return;
        }
        if (CopycatBuild.isCauldron(data.getMaterial())) return;
        if (CopycatBuild.isFluid(data.getMaterial())) {
            builder.onBroken(BlockPos.of(block));
            return;
        }
        if (!(data instanceof Waterlogged waterlogged)) {
            event.setCancelled(true);
            return;
        }
        Waterlogged after = (Waterlogged) waterlogged.clone();
        after.setWaterlogged(false);
        builder.onPlaced(BlockPos.of(block), after);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        if (roleMap.is(event.getPlayer().getUniqueId(), Spectator.class)) {
            event.setCancelled(true);
            return;
        }
        if (!Builder.isDoneItem(event.getItem())) return;
        event.setCancelled(true);
        Builder builder = roleMap.as(event.getPlayer().getUniqueId(), Builder.class);
        if (builder != null) {
            this.submit(builder);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        this.ensureNotIllegal();
        if (!(event.getWhoClicked() instanceof Player player) || !isParticipant(player)) return;
        boolean touchesDoneItem = Builder.isDoneItem(event.getCurrentItem())
            || Builder.isDoneItem(event.getCursor())
            || event.getHotbarButton() == Builder.DONE_SLOT
            || (event.getClick() == ClickType.SWAP_OFFHAND && Builder.isDoneItem(player.getInventory().getItemInOffHand()));
        if (touchesDoneItem) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        this.ensureNotIllegal();
        if (!(event.getWhoClicked() instanceof Player player) || !isParticipant(player)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (event.getView().getInventory(rawSlot) == player.getInventory() && event.getView().convertSlot(rawSlot) == Builder.DONE_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        this.ensureNotIllegal();
        if (isParticipant(event.getPlayer()) && (Builder.isDoneItem(event.getMainHandItem()) || Builder.isDoneItem(event.getOffHandItem()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        this.ensureNotIllegal();
        if (isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (event.getEntity() instanceof Player player && isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        this.ensureNotIllegal();
        if (event.getEntity() instanceof Player player && isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        if (isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        this.ensureNotIllegal();
        if (!inArena(event.getBlock())) return;
        Material type = event.getBlock().getType();
        if (!type.isSolid() || type.hasGravity()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFlow(BlockFromToEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        this.ensureNotIllegal();
        if (inArena(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        this.ensureNotIllegal();
        Arena current = this.arena;
        if (current != null && current.contains(event.getLocation())) event.setCancelled(true);
    }

    private enum Phase {
        SETUP,
        PREVIEW,
        BUILDING,
        SCORING,
        SHOWCASE,
        INTERMISSION,
        FINISHED
    }
}
