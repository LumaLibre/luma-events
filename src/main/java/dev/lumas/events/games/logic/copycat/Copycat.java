package dev.lumas.events.games.logic.copycat;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.game.CopycatDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.exceptions.MinigameException;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.logic.copycat.roles.Builder;
import dev.lumas.events.games.logic.copycat.roles.CopycatRole;
import dev.lumas.events.games.logic.copycat.roles.Spectator;
import dev.lumas.events.games.logic.copycat.roles.TransientSpectator;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.CopycatTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
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
    };
    static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();
    private static final String SCHEM_FOLDER = "copycat";
    private static final long TICK_INTERVAL = 2;
    private static final int PREVIEW_SECONDS = 15;
    private static final double MIN_HIDDEN_FRACTION = 0.25;
    private static final double MAX_HIDDEN_FRACTION = 0.60;
    private static final int ELIMINATIONS_PER_ROUND = 3;
    private static final long SHOWCASE_SECONDS = 7;
    private static final long INTERMISSION_SECONDS = 5;
    private static final long END_DELAY_SECONDS = 8;
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
    private final CopycatTokenFormula tokenFormula = new CopycatTokenFormula();
    private final Map<UUID, Integer> roundsSurvived = new ConcurrentHashMap<>();
    private final Map<UUID, BuildScore> roundScores = new ConcurrentHashMap<>();
    private final Deque<Builder> showcase = new ConcurrentLinkedDeque<>();
    private final List<Builder> eliminated = new CopyOnWriteArrayList<>();
    private final AtomicBoolean ended = new AtomicBoolean();
    @Getter
    private volatile Arena arena;
    private volatile Phase phase = Phase.SETUP;
    private volatile long phaseEndsAt;
    private volatile Round round;
    private volatile List<Builder> roundBuilders = List.of();
    private volatile int roundNumber;
    private volatile int plannedRounds = 1;
    private volatile CountdownBossBar bar;

    public Copycat(CopycatDefinition definition) {
        super("Copycat", Util.getRandom(SPLASH), 0, TICK_INTERVAL, true, true, false, false);
        Preconditions.checkState(definition.getLobby() != null, "Copycat needs a lobby");
        Preconditions.checkState(definition.getOrigin() != null, "Copycat needs an arena origin");
        this.definition = definition;
        this.lobby = definition.getLobby();

        File folder = EventMain.getInstance().getDataPath().resolve("schematics").resolve(SCHEM_FOLDER).toFile();
        List<CopycatBuild> builds = CopycatBuild.loadAll(folder);
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
            .mapToInt(build -> Round.buildSeconds(Round.hiddenCount(build, MAX_HIDDEN_FRACTION), definition.getBuildSecondsPerBlock()))
            .max()
            .orElseThrow();
        long roundSeconds = PREVIEW_SECONDS + longestBuild
            + ELIMINATIONS_PER_ROUND * SHOWCASE_SECONDS + INTERMISSION_SECONDS + 10;
        return TimeUnit.SECONDS.toMillis(SETUP_ALLOWANCE_SECONDS
            + roundSeconds * Math.max(1, definition.getMaxRounds())
            + END_DELAY_SECONDS);
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.teleportOnJoin(player, lobby);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        CopycatRole role = roleMap.remove(participant.getUuid());
        if (role != null) {
            role.cleanup();
        }
        showcase.removeIf(builder -> builder.getUuid().equals(participant.getUuid()));
        eliminated.removeIf(builder -> builder.getUuid().equals(participant.getUuid()));
        participant.teleportAsync(lobby);
        return super.removeParticipant(participant, doTeleport);
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
        int roundsToEliminateEveryone = Math.ceilDiv(Math.max(0, participants.size() - 1), ELIMINATIONS_PER_ROUND);
        this.plannedRounds = Math.max(1, participants.size() <= 1
            ? definition.getMaxRounds()
            : Math.min(definition.getMaxRounds(), roundsToEliminateEveryone));
        scoreboard.addScorers(List.copyOf(participants));
        List<Plot> plots = arena.getPlots();
        for (int i = 0; i < participants.size(); i++) {
            roleMap.put(new Builder(this, participants.get(i), plots.get(i)));
        }

        this.sendAudienceTitle("<yellow>Building the arena", "<gray>Hold tight...");
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
            this.sendAudienceMessage("Copy the build in front of you. Perfect copies score the most, mistakes <red>cost</red> points, and the <red>worst " + ELIMINATIONS_PER_ROUND + "</red> each round are out.");
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

    private double hiddenFraction(int number) {
        double progress = plannedRounds <= 1 ? 1.0 : Math.min(1.0, (number - 1) / (double) (plannedRounds - 1));
        return MIN_HIDDEN_FRACTION + (MAX_HIDDEN_FRACTION - MIN_HIDDEN_FRACTION) * progress;
    }

    private synchronized void startRound() {
        if (!this.active || this.stopping || phase == Phase.FINISHED) return;
        List<Builder> builders = builders();
        boolean lastOneStanding = builders.size() == 1 && roleMap.size() > 1;
        if (builders.isEmpty() || lastOneStanding || pool.isEmpty() || roundNumber >= definition.getMaxRounds()) {
            this.finish();
            return;
        }

        int number = ++roundNumber;
        Round next = new Round(number, pool.poll().rotated(RANDOM.nextInt(4)), hiddenFraction(number), definition.getBuildSecondsPerBlock());
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
        this.showBar("<yellow><b>Memorize!</b></yellow> <gray>|</gray> <white>%ss", BossBar.Color.YELLOW, PREVIEW_SECONDS);
        this.sendAudienceTitle("<yellow><b>Round " + roundNumber, "<gray>Memorize the build!");
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
        this.sendAudienceTitle("<green><b>Build!", "<gray>" + current.hiddenCount() + " blocks went missing, " + buildSeconds + "s to fix it");
        this.sendAudienceMessage("You have exactly the blocks that were removed. Put them back where they belong!");
    }

    private synchronized void beginScoring() {
        if (phase != Phase.BUILDING) return;
        this.phase = Phase.SCORING;
        this.stopBar();
        this.sendAudienceTitle("<gold><b>Time!", "<gray>Checking builds...");
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

        int cut = Math.min(ELIMINATIONS_PER_ROUND, builders.size() - 1);
        List<Builder> losers = scored.stream()
            .filter(builder -> !roundScores.get(builder.getUuid()).perfect())
            .sorted(Comparator.<Builder>comparingInt(builder -> roundScores.get(builder.getUuid()).points())
                .thenComparing(builder -> -roundScores.get(builder.getUuid()).mistakes()))
            .limit(Math.max(0, cut))
            .toList();

        for (Builder builder : builders) {
            if (!losers.contains(builder)) {
                roundsSurvived.merge(builder.getUuid(), 1, Integer::sum);
            }
        }

        eliminated.clear();
        eliminated.addAll(losers);
        showcase.clear();
        if (losers.isEmpty()) {
            this.sendAudienceMessage("<green>Everyone nailed it!</green> Nobody is eliminated this round.");
            this.beginIntermission();
            return;
        }
        for (int i = losers.size() - 1; i >= 0; i--) {
            showcase.addLast(losers.get(i));
        }
        this.phase = Phase.SHOWCASE;
        this.sendAudienceMessage("Let's take a look at the <red>" + losers.size() + "</red> weakest builds...");
        this.showNext();
    }

    private synchronized void showNext() {
        if (phase != Phase.SHOWCASE) return;
        Builder next = showcase.pollFirst();
        if (next == null) {
            for (Builder loser : eliminated) {
                this.eliminate(loser);
            }
            eliminated.clear();
            this.beginIntermission();
            return;
        }

        this.phaseEndsAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SHOWCASE_SECONDS);
        BuildScore score = roundScores.get(next.getUuid());
        String points = score == null ? "?" : String.valueOf(score.points());
        String summary = score == null ? "" : " <gray>|</gray> " + score.summary();
        Executors.teleportGroupAsync(participants, next.getPlot().getOverview());
        this.sendAudienceTitle("<red>" + next.getName() + "'s build", "<gold>" + points + " points</gold>" + summary);
        this.playAudienceSound(Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

    private void eliminate(Builder builder) {
        if (!controls(builder)) return;
        CopycatRole spectator = roleMap.swapRole(builder.getEventPlayer(), () -> new Spectator(this, builder.getEventPlayer()));
        spectator.play();
        this.sendAudienceMessage("<red>" + builder.getName() + "</red> has been eliminated.");
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
            if (!this.active || this.stopping || phase != Phase.BUILDING || roleMap.get(builder.getUuid()) != builder) return;
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
        this.returnItems(event.getPlayer(), data);
        builder.onBroken(pos);
        if (CopycatBuild.isTallPair(data)) {
            BlockPos partnerPos = CopycatBuild.partnerOf(pos, data);
            Block partner = block.getWorld().getBlockAt(partnerPos.x(), partnerPos.y(), partnerPos.z());
            if (CopycatBuild.isWaterlogged(partner.getBlockData())) {
                event.getPlayer().getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
            }
            partner.setBlockData(ArenaBlocks.AIR, false);
            builder.onBroken(partnerPos);
        }
    }

    private void returnItems(Player player, BlockData data) {
        player.getInventory().addItem(CopycatBuild.itemFor(data));
        if (CopycatBuild.isWaterlogged(data)) {
            player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
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
                builder.onPlaced(BlockPos.of(replaced.getBlock()), replaced.getBlock().getBlockData());
            }
            return;
        }
        builder.onPlaced(BlockPos.of(event.getBlockPlaced()), event.getBlockPlaced().getBlockData());
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
