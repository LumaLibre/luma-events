package dev.lumas.events.games.logic;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.IncursionDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.IncursionTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.latency.LatencyTracker;
import dev.lumas.lumaitems.particles.ParticleDisplay;
import dev.lumas.lumaitems.particles.Particles;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parched;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Witch;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

public final class Incursion extends InventoryUnifiedMinigame {

    private static final NamespacedKey FREEZE_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_freeze");
    private static final NamespacedKey SPEED_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_speed");
    private static final NamespacedKey MINIBOSS_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_miniboss");
    private static final NamespacedKey KIT_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_kit");
    private static final NamespacedKey WEAPON_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_weapon");
    private static final NamespacedKey GRENADE_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_grenade");

    private static final double PLAYER_HALF_WIDTH = 0.35;
    private static final double PLAYER_HALF_HEIGHT = 0.95;

    private static final int ORB_RENDER_PERIOD_TICKS = 2;
    private static final int ORB_SHELL_POINTS = 32;
    private static final double ORB_RADIUS = 0.4;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final Particle.DustOptions ORB_SHELL_DUST = new Particle.DustOptions(Color.fromRGB(12, 10, 18), 0.7f);
    private static final Particle.DustOptions ORB_CORE_DUST = new Particle.DustOptions(Color.WHITE, 1.1f);
    private static final double ORB_FLOAT_HEIGHT = 0.7;
    private static final double ORB_PICKUP_RADIUS = 0.55;
    private static final int ORB_CORE_POINTS = 5;
    private static final double ORB_CORE_SPREAD = 0.08;
    private static final String FIREWORK_KEY = "incursion_firework";

    private static final int MINIBOSS_TICK_PERIOD_TICKS = 2;
    private static final int MINIBOSS_SPAWN_ATTEMPTS = 12;
    private static final int MINIBOSS_HEIGHT_BLOCKS = 2;
    private static final double MINIBOSS_ROOM_INSET = 0.4;
    private static final long MINIBOSS_STALL_MILLIS = 3_000L;
    private static final long MINIBOSS_TARGET_SCAN_MILLIS = 500L;
    private static final double MINIBOSS_ROOM_TARGET_MARGIN = 15.0;
    private static final double MINIBOSS_TARGET_RANGE_SQUARED = 20.0 * 20.0;
    private static final List<GoalKey<Creature>> MINIBOSS_DISABLED_GOALS = List.of(
            VanillaGoal.RANDOM_STROLL,
            VanillaGoal.WATER_AVOIDING_RANDOM_STROLL,
            VanillaGoal.MOVE_THROUGH_VILLAGE,
            VanillaGoal.FLEE_SUN,
            VanillaGoal.RESTRICT_SUN,
            VanillaGoal.TRY_FIND_WATER,
            VanillaGoal.DROWNED_GO_TO_WATER
    );

    private static final Particle.DustOptions SPIT_DUST = new Particle.DustOptions(Color.fromRGB(70, 145, 230), 0.5f);

    private static final Color BLUNDERHORN_GUNMETAL = Color.fromRGB(88, 92, 99);
    private static final double BLUNDERHORN_TEAM_TINT = 0.4;

    private static final int CHARGE_BAR_SEGMENTS = 10;
    private static final int SHOTGUN_PELLETS = 16;
    private static final double BEAM_STEP = 0.5;

    // Heights of the spheres a player's hitbox is approximated by for the blunderhorn's cone test
    private static final double[] BODY_SAMPLE_FRACTIONS = {0.2, 0.5, 0.8};

    private static final double HEAD_DROP_BELOW_EYE = 0.32;
    private static final double MAX_HEAD_FRACTION = 0.5;

    private static final Title.Times CHARGE_TITLE_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(400), Duration.ZERO);
    private static final Title.Times HEADSHOT_TITLE_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(200));

    // How long after a hit its attacker still gets credited with the kill
    private static final long DAMAGE_CREDIT_MILLIS = 10_000L;

    private static final int HISTORY_TICKS = 20;
    private static final long MAX_REWIND_NANOS = TimeUnit.MILLISECONDS.toNanos((HISTORY_TICKS - 4) * 50L);

    private static final long INVINCIBILITY_AURA_PERIOD_MILLIS = 100L;
    private static final Duration INVINCIBILITY_TITLE_STAY = Duration.ofMillis(400);
    private static final double INVINCIBILITY_AURA_RADIUS = 0.75;
    private static final int INVINCIBILITY_AURA_POINTS = 8;

    private static final AttributeModifier FREEZE_MODIFIER =
            new AttributeModifier(FREEZE_KEY, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private static final List<Attribute> FREEZE_ATTRIBUTES = List.of(Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH);

    private static final double MAX_IMBALANCE_SLOWDOWN = 0.6;
    private static final double LN_2 = Math.log(2.0);

    private static final Set<Integer> WARNING_SECONDS = Set.of(60, 30, 10, 3, 2, 1);

    private static final double LETHAL_FALL_BLOCKS = 10.0;
    private static final double VANILLA_FALL_GRACE_BLOCKS = 3.0;
    private static final double LETHAL_FALL_DAMAGE = 1_000.0;

    private static final Set<EntityDamageEvent.DamageCause> FIRE_DAMAGE_CAUSES =
            Set.of(EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK);

    private static final List<Grenade> GRENADES = List.of(Grenade.values());

    private static final List<FireworkEffect.Type> FIREWORK_TYPES = Arrays.stream(FireworkEffect.Type.values())
            .filter(type -> type != FireworkEffect.Type.BALL_LARGE) // BALL_LARGE is a bit much
            .toList();

    private final IncursionDefinition definition;
    private final IncursionTokenFormula tokenFormula;

    private final double baseSpeedMultiplier;

    private final boolean imbalanceEnabled;
    private final double imbalanceSlowdownAtDouble;
    private final double imbalanceMaxSlowdown;
    private final double imbalanceMinSlowdown;

    @Nullable
    private volatile IncursionTeam slowedTeam;
    private volatile double slowdown = 0.0;

    private final MapSide side1;
    private final MapSide side2;

    private final Scoreboard<IncursionTeam> scoreboard = new Scoreboard<>();
    private final ConcurrentHashMap<UUID, Integer> points = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> frozenAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> invincibleUntil = new ConcurrentHashMap<>();
    private final Set<UUID> respawning = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Integer> charging = new ConcurrentHashMap<>();
    private final Set<UUID> chargeReady = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, DamageCredit> lastDamageCredit = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, EventPlayer> participantsByUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, IncursionTeam> teamsByUuid = new ConcurrentHashMap<>();
    private final Set<Integer> announcedSwapWarnings = ConcurrentHashMap.newKeySet();
    private final Set<Integer> announcedEndWarnings = ConcurrentHashMap.newKeySet();

    private volatile IncursionTeam team1;
    private volatile IncursionTeam team2;
    private volatile boolean secondHalf = false;
    private volatile boolean kickoffInProgress = false;
    private final List<Orb> orbs = new CopyOnWriteArrayList<>();
    private final List<ScheduledTask> orbTasks = new ArrayList<>();

    // Every participant's and miniboss' hitbox over the last HISTORY_TICKS ticks, keyed by entity
    private final ConcurrentHashMap<UUID, Trail> trails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Recorder> recorders = new ConcurrentHashMap<>();
    private final LatencyTracker latency = new LatencyTracker(this::onlineParticipants);

    private final List<Miniboss> minibosses;
    private final ConcurrentHashMap<UUID, Miniboss> minibossesByEntity = new ConcurrentHashMap<>();
    private final int minibossDropsMin;
    private final int minibossDropsMax;

    private CountdownBossBar countdownBossBar;
    private ScheduledTask auraTask;

    public Incursion(IncursionDefinition definition) {
        super(
                "Incursion",
                "Dive into the enemy team's hole to score!",
                Util.secsToMillis(definition.getGameLengthSeconds()),
                10,
                true,
                true,
                false,
                true
        );
        this.definition = definition;
        this.boundingBox = definition.getBounds().toBlockBoundingBox();
        this.side1 = new MapSide(definition.getTeam1().getSpawnArea(), definition.getTeam1().getHole().toBlockBoundingBox());
        this.side2 = new MapSide(definition.getTeam2().getSpawnArea(), definition.getTeam2().getHole().toBlockBoundingBox());
        this.tokenFormula = new IncursionTokenFormula(definition.getPointsPerToken());

        this.baseSpeedMultiplier = Math.max(0.0, definition.getMovementSpeedMultiplier());

        IncursionDefinition.ImbalanceCompensationSettings imbalance = definition.getImbalanceCompensation();
        this.imbalanceEnabled = imbalance.isEnabled();
        this.imbalanceMaxSlowdown = Math.clamp(imbalance.getMaxSlowdown(), 0.0, MAX_IMBALANCE_SLOWDOWN);
        this.imbalanceSlowdownAtDouble = Math.clamp(imbalance.getSlowdownAtDoubleSize(), 0.0, MAX_IMBALANCE_SLOWDOWN);
        this.imbalanceMinSlowdown = Math.max(0.0, imbalance.getMinSlowdown());

        List<Miniboss> rooms = new ArrayList<>();
        for (WorldTiedBoundingBox room : definition.getBossRooms()) {
            if (room == null || room.getWorld() == null) continue;
            rooms.add(new Miniboss(room));
        }
        this.minibosses = List.copyOf(rooms);

        int[] drops = parseRange(definition.getMiniboss().getGrenadeDrops());
        this.minibossDropsMin = drops[0];
        this.minibossDropsMax = drops[1];
    }

    // "2-4" -> {2, 4}, "3" -> {3, 3}, invalid -> {0, 0}
    private static int[] parseRange(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return new int[]{0, 0};
        int dash = raw.indexOf('-', 1);
        int low = Math.max(0, Util.getInt((dash < 0 ? raw : raw.substring(0, dash)).trim(), 0));
        int high = dash < 0 ? low : Math.max(0, Util.getInt(raw.substring(dash + 1).trim(), low));
        return new int[]{Math.min(low, high), Math.max(low, high)};
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        participantsByUuid.put(player.getUuid(), player);
        this.latency.start();
        player.operatePlayer(this::applySpeed);
        this.teleportOnJoin(player, definition.getLobbyLocation());
        return true;
    }

    @Override
    protected void handleStart() {
        this.team1 = new IncursionTeam(definition.getTeam1(), side1);
        this.team2 = new IncursionTeam(definition.getTeam2(), side2);
        this.slowedTeam = null;
        this.slowdown = 0.0;

        this.latency.start(); // No-op if already started
        EventMain.getInstance().getLogger().info("Incursion is measuring latency with " + latency.sourceName());

        List<EventPlayer> drafted = draftTeams();
        for (EventPlayer participant : drafted) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue; // Shouldn't happen
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                player.setFoodLevel(20);
                player.setSaturation(20f);
                applySpeed(player);
                equipKit(player, team);
            });
        }
        logSpeedState("game start");

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        this.countdownBossBar.start();

        this.auraTask = Executors.runRepeatingAsync(TimeUnit.MILLISECONDS, 0, INVINCIBILITY_AURA_PERIOD_MILLIS, _ -> {
            tickInvincibilityAura();
            tickSniperCharge();
            handleMinibossRespawn();
            tickRecorders();
        });

        this.startOrbs();
        this.minibosses.forEach(Miniboss::spawn);
        this.kickoff("<gold><b>First Half");
    }

    private List<EventPlayer> draftTeams() {
        IncursionDefinition.TeamBalancingSettings settings = definition.getTeamBalancing();

        List<EventPlayer> drafted = new ArrayList<>(this.participants);
        Collections.shuffle(drafted, RANDOM);
        for (EventPlayer participant : drafted) {
            participantsByUuid.put(participant.getUuid(), participant);
        }

        if (!settings.isByPing()) {
            for (int i = 0; i < drafted.size(); i++) {
                teamsByUuid.put(drafted.get(i).getUuid(), i % 2 == 0 ? team1 : team2);
            }
            return drafted;
        }

        Map<UUID, Integer> pings = measurePings(drafted);
        int jitter = Math.max(0, settings.getJitterMillis());
        Map<UUID, Integer> order = new HashMap<>();
        for (EventPlayer participant : drafted) {
            order.put(participant.getUuid(),
                    pings.get(participant.getUuid()) + RANDOM.nextInt(-jitter, jitter + 1));
        }

        // Worst connections first, so the picks left over at the end fine-tune the split
        drafted.sort(Comparator.comparingInt((EventPlayer participant) -> order.get(participant.getUuid())).reversed());

        int cap = (drafted.size() + 1) / 2;
        int tie = Math.max(0, settings.getTieMillis());
        int[] totals = new int[2];
        int[] sizes = new int[2];

        for (EventPlayer participant : drafted) {
            int side;
            if (sizes[0] >= cap) side = 1;
            else if (sizes[1] >= cap) side = 0;
            else if (Math.abs(totals[0] - totals[1]) <= tie) side = RANDOM.nextInt(2);
            else side = totals[0] < totals[1] ? 0 : 1;

            totals[side] += pings.get(participant.getUuid());
            sizes[side]++;
            teamsByUuid.put(participant.getUuid(), side == 0 ? team1 : team2);
        }

        EventMain.getInstance().getLogger().info(String.format(
                "Incursion drafted %s with %d players averaging %dms and %s with %d players averaging %dms",
                team1.getName(), sizes[0], sizes[0] == 0 ? 0 : totals[0] / sizes[0],
                team2.getName(), sizes[1], sizes[1] == 0 ? 0 : totals[1] / sizes[1]));

        return drafted;
    }

    private Map<UUID, Integer> measurePings(List<EventPlayer> drafted) {
        Map<UUID, Integer> pings = new HashMap<>();
        long total = 0;

        for (EventPlayer participant : drafted) {
            Player player = participant.getPlayer();
            if (player == null) continue;

            int ping = latency.pingMillis(player);
            pings.put(participant.getUuid(), ping);
            total += ping;
        }

        int average = pings.isEmpty() ? 0 : (int) (total / pings.size());
        for (EventPlayer participant : drafted) {
            pings.putIfAbsent(participant.getUuid(), average);
        }
        return pings;
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (team1 == null || team2 == null) return;

        tickImbalanceCompensation();

        if (!secondHalf) {
            long untilSwap = (this.getDuration() / 2) - (this.getDuration() - timeLeft);
            if (untilSwap <= 0) halfTime();
            else warn(untilSwap, announcedSwapWarnings, "Sides swap in <yellow>%s<reset>!");
        } else {
            warn(timeLeft, announcedEndWarnings, "The game ends in <yellow>%s<reset>!");
        }

        broadcastScoreActionBar();
    }

    @Override
    protected void handleStop() {
        this.kickoffInProgress = false;
        if (this.countdownBossBar != null) this.countdownBossBar.stop(false);
        if (this.auraTask != null) this.auraTask.cancel();
        this.latency.stop();
        this.stopRecording();
        this.orbTasks.forEach(ScheduledTask::cancel);
        this.orbTasks.clear();
        this.orbs.clear();
        this.minibosses.forEach(Miniboss::despawn);
        this.minibossesByEntity.clear();

        for (EventPlayer participant : this.participants) {
            clearPlayerState(participant);
            participant.operatePlayer(Incursion::restoreVitals);
        }

        this.participantsByUuid.clear();
        this.teamsByUuid.clear();

        if (team1 != null && team2 != null) {
            scoreboard.addScore(team1, team1.getScore().get());
            scoreboard.addScore(team2, team2.getScore().get());
        }

        this.scoreboard.handleGameEnd(this.audience, () -> {
            Executors.teleportGroupAsync(this.participants, definition.getLobbyLocation());
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
                    .seconds(10)
                    .callback(() -> this.sendAudienceMessage("This minigame has concluded."))
                    .build()
                    .start();
        });
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = points.getOrDefault(participant.getUuid(), 0);
        tokenFormula.giveTokens(participant, score);
        participant.addPermanentScore(MinigameConstant.INCURSION, score);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        UUID uuid = participant.getUuid();
        participantsByUuid.remove(uuid);
        teamsByUuid.remove(uuid);
        clearPlayerState(participant);

        Player player = participant.getPlayer();
        if (player != null && this.countdownBossBar != null) {
            this.countdownBossBar.getBossBar().removeViewer(player);
        }

        return super.removeParticipant(participant, doTeleport);
    }

    private synchronized void halfTime() {
        if (secondHalf || !this.active || this.stopping) return;
        this.secondHalf = true;

        MapSide previous = team1.getSide();
        team1.setSide(team2.getSide());
        team2.setSide(previous);

        this.orbs.forEach(Orb::refill);
        this.minibosses.forEach(Miniboss::respawn);

        this.sendAudienceMessage("<gold><b>Half time!</b> <reset>Both teams have swapped sides.");
        this.playAudienceSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.4f);
        this.kickoff("<gold><b>Second Half");
    }

    // Sends everyone to their spawn area, locks them in place and starts a start-countdown
    private void kickoff(String title) {
        this.kickoffInProgress = true;
        int cooldownTicks = Math.max(0, definition.getStartCooldownTicks());

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue; // Shouldn't happen
            sendToSpawn(participant, team, cooldownTicks);
        }

        if (cooldownTicks == 0) {
            releaseKickoff();
            return;
        }

        long endsAt = System.currentTimeMillis() + (cooldownTicks * 50L);
        AtomicInteger lastShown = new AtomicInteger(-1);
        Executors.runRepeatingAsync(TimeUnit.MILLISECONDS, 0, 200, task -> {
            if (!this.active || this.stopping) {
                task.cancel();
                return;
            }

            long remaining = endsAt - System.currentTimeMillis();
            if (remaining <= 0) {
                task.cancel();
                releaseKickoff();
                return;
            }

            int seconds = (int) Math.ceil(remaining / 1000.0);
            if (lastShown.getAndSet(seconds) == seconds) return;
            this.audience.showTitle(Title.title(
                    Util.color(title),
                    Util.color("<yellow>Get ready<gray>...</gray> <b>" + seconds + "</b>"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
            ));
            this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
        });
    }

    private void releaseKickoff() {
        this.kickoffInProgress = false;

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            invincibleUntil.remove(participant.getUuid());
            participant.operatePlayer(this::unfreeze);
        }

        this.audience.showTitle(Title.title(
                Util.color("<green><b>GO!"),
                Util.color("<gray>Dive into the enemy hole to score!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(300))
        ));
        this.playAudienceSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
    }

    private void warn(long millisRemaining, Set<Integer> announced, String format) {
        int seconds = (int) Math.round(millisRemaining / 1000.0);
        if (!WARNING_SECONDS.contains(seconds) || !announced.add(seconds)) return;
        this.sendAudienceMessage(String.format(format, formatCountdown(seconds)));
        this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, seconds <= 3 ? 1.8f : 1.0f);
    }

    private static String formatCountdown(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return seconds + (seconds == 1 ? " second" : " seconds");
    }

    private void broadcastScoreActionBar() {
        Component separator = Component.text(" | ", NamedTextColor.DARK_GRAY);

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue;
            IncursionTeam enemy = enemyOf(team);

            Component actionBar = Component.text(team.getName() + " " + team.getScore().get(), team.getColor())
                    .append(separator)
                    .append(Component.text(enemy.getName() + " " + enemy.getScore().get(), enemy.getColor()))
                    .append(separator)
                    .append(Component.text("You: " + points.getOrDefault(participant.getUuid(), 0), NamedTextColor.GRAY));

            participant.sendActionBar(actionBar.decorate(TextDecoration.BOLD));
        }
    }

    private void tickInvincibilityAura() {
        if (!this.active || this.stopping || invincibleUntil.isEmpty()) return;
        if (kickoffInProgress) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : invincibleUntil.entrySet()) {
            UUID uuid = entry.getKey();
            EventPlayer participant = participantOf(uuid);
            IncursionTeam team = teamOf(uuid);
            if (participant == null || team == null) {
                invincibleUntil.remove(uuid);
                continue;
            }

            if (entry.getValue() <= now) {
                invincibleUntil.remove(uuid);
                participant.operatePlayer(player -> {
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.6f, 1.6f);
                    player.clearTitle();
                });
                continue;
            }

            long remaining = entry.getValue() - now;
            participant.operatePlayer(player -> {
                drawInvincibilityAura(player, team);
                player.showTitle(invincibilityTitle(remaining));
            });
        }
    }

    private void handleMinibossRespawn() {
        if (!this.active || this.stopping || this.minibosses.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (Miniboss miniboss : this.minibosses) {
            if (miniboss.hasStalled(now)) miniboss.respawn();
            else if (miniboss.dueForRespawn(now)) miniboss.spawn();
        }
    }

    private static Title invincibilityTitle(long remainingMillis) {
        return Title.title(
                Util.color("<gray>⛨"),
                Util.color("<gray>" + String.format("%.1f", remainingMillis / 1000.0) + "s"),
                Title.Times.times(Duration.ZERO, INVINCIBILITY_TITLE_STAY, Duration.ZERO)
        );
    }

    private void drawInvincibilityAura(Player player, IncursionTeam team) {
        Location center = player.getLocation();
        double phase = (System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2;

        for (int i = 0; i < INVINCIBILITY_AURA_POINTS; i++) {
            double angle = phase + (Math.PI * 2 * i / INVINCIBILITY_AURA_POINTS);
            double x = center.getX() + (Math.cos(angle) * INVINCIBILITY_AURA_RADIUS);
            double z = center.getZ() + (Math.sin(angle) * INVINCIBILITY_AURA_RADIUS);
            center.getWorld().spawnParticle(Particle.DUST, x, center.getY() + 0.1, z, 1, 0, 0, 0, 0, team.getDustOptions());
            center.getWorld().spawnParticle(Particle.DUST, x, center.getY() + 1.1, z, 1, 0, 0, 0, 0, team.getDustOptions());
        }
    }

    private void playScoreEffects(Location where, IncursionTeam team) {
        Executors.runSync(where, () -> {
            Location burst = where.clone().add(0, 1, 0);

            Particles.spikeSphere(2.5, 18.0, 3, 0.2, 0.8, ParticleDisplay.of(Particle.DUST)
                    .withColor(Util.bukkitToAwtColor(team.getArmorColor()))
                    .withLocation(burst));
            burst.getWorld().spawnParticle(Particle.FIREWORK, burst, 40, 0.4, 0.6, 0.4, 0.15);
            burst.getWorld().playSound(burst, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 1.2f);

            burst.getWorld().spawn(burst, Firework.class, firework -> {
                Util.setPersistentKey(firework, FIREWORK_KEY, PersistentDataType.BYTE, (byte) 1);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(team.getArmorColor())
                        .withFade(team.getArmorColor())
                        .with(Util.getRandom(FIREWORK_TYPES))
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            });
        });
    }

    // Teleports a participant into their team's spawn area AND FREEZES THEM THERE
    private void sendToSpawn(EventPlayer participant, IncursionTeam team, int invincibilityTicks) {
        if (!this.active || this.stopping) return;

        Player player = participant.getPlayer();
        if (player == null) return;

        UUID uuid = participant.getUuid();
        respawning.add(uuid);
        if (invincibilityTicks > 0) {
            invincibleUntil.put(uuid, System.currentTimeMillis() + (invincibilityTicks * 50L));
        }

        Location spawn = team.getSide().spawnArea().randomSpawn();
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            teleportToSpawn(player, team, spawn, uuid);
            return;
        }
        boolean scheduled = player.getScheduler().execute(EventMain.getInstance(),
                () -> teleportToSpawn(player, team, spawn, uuid),
                () -> respawning.remove(uuid),
                1L);
        if (!scheduled) respawning.remove(uuid);
    }

    private void teleportToSpawn(Player player, IncursionTeam team, Location spawn, UUID uuid) {
        Executors.teleportSafely(player, spawn).whenComplete((_, _) -> Executors.delayedSync(player, 1, () -> {
            // The game can end while we're in flight, and handleStop has already cleaned this player
            // up and sent them to the lobby by now. Re-freezing them there would strand them.
            if (!this.active || this.stopping) {
                respawning.remove(uuid);
                return;
            }

            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0f);
            restoreVitals(player);

            equipKit(player, team);
            freeze(player, spawn);
            respawning.remove(uuid);
        }));
    }

    private void respawnFlow(EventPlayer participant, IncursionTeam team) {
        if (respawning.contains(participant.getUuid())) return;

        int freezeTicks = Math.max(0, definition.getRespawnFreezeTicks());
        int invincibilityTicks = Math.max(freezeTicks, definition.getRespawnInvincibilityTicks());
        sendToSpawn(participant, team, invincibilityTicks);

        Player player = participant.getPlayer();
        if (player == null) return;
        if (freezeTicks == 0) {
            Executors.runSync(player, () -> unfreeze(player));
            return;
        }
        Executors.delayedSync(player, freezeTicks, () -> {
            if (!kickoffInProgress) unfreeze(player);
        });
    }

    private void scoreHole(EventPlayer participant, IncursionTeam team, Location where) {
        int awarded = definition.getHolePoints();
        addPoints(participant, team, awarded);

        String name = participant.getName() != null ? participant.getName() : "Someone";
        this.sendAudienceMessage(Component.text(name, team.getColor())
                .append(Component.text(" dove into the enemy hole! ", NamedTextColor.WHITE))
                .append(Component.text("(+" + awarded + ")", NamedTextColor.GRAY)));
        this.playAudienceSound(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        playScoreEffects(where, team);
        respawnFlow(participant, team);
    }

    private void addPoints(EventPlayer participant, IncursionTeam team, int amount) {
        points.merge(participant.getUuid(), amount, Integer::sum);
        team.getScore().addAndGet(amount);
    }

    private void freeze(Player player, Location at) {
        if (frozenAt.put(player.getUniqueId(), at.clone()) != null) return; // already frozen
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            if (instance.getModifiers().stream().noneMatch(modifier -> FREEZE_KEY.equals(modifier.getKey()))) {
                instance.addTransientModifier(FREEZE_MODIFIER);
            }
        }
    }

    private void unfreeze(Player player) {
        frozenAt.remove(player.getUniqueId());
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            instance.getModifiers().stream()
                    .filter(modifier -> FREEZE_KEY.equals(modifier.getKey()))
                    .toList()
                    .forEach(instance::removeModifier);
        }
    }

    private void tickImbalanceCompensation() {
        if (!imbalanceEnabled) return;

        int size1 = teamSize(team1);
        int size2 = teamSize(team2);

        IncursionTeam bigger = null;
        double penalty = 0.0;

        if (size1 > 0 && size2 > 0 && size1 != size2) {
            bigger = size1 > size2 ? team1 : team2;
            penalty = slowdownFor((double) Math.max(size1, size2) / Math.min(size1, size2));
            if (penalty <= 0.0) bigger = null;
        }

        IncursionTeam previousTeam = this.slowedTeam;
        double previousSlowdown = this.slowdown;
        if (bigger == previousTeam && Math.abs(penalty - previousSlowdown) < 1.0e-6) return;

        this.slowedTeam = bigger;
        this.slowdown = penalty;

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue;

            double before = team == previousTeam ? previousSlowdown : 0.0;
            double after = team == bigger ? penalty : 0.0;
            if (Math.abs(before - after) < 1.0e-6) continue;

            participant.operatePlayer(this::applySpeed);
        }

        announceSpeedChange(previousTeam, bigger, penalty, size1, size2);
        logSpeedState(bigger == null ? "imbalance cleared" : "imbalance updated", size1, size2);
    }

    private double slowdownFor(double ratio) {
        double penalty = Math.min(imbalanceMaxSlowdown, imbalanceSlowdownAtDouble * (Math.log(ratio) / LN_2));
        return penalty < imbalanceMinSlowdown ? 0.0 : penalty;
    }

    private void announceSpeedChange(@Nullable IncursionTeam previouslySlowed, @Nullable IncursionTeam slowed,
                                     double slowdown, int size1, int size2) {
        if (slowed == null || slowdown <= 0.0) {
            if (previouslySlowed == null) return;
            IncursionTeam evened = enemyOf(previouslySlowed);
            sendTeamMessage(previouslySlowed, "<green>You no longer outnumber " + evened.getName()
                    + "<reset> - your movement speed is back to normal.");
            sendTeamMessage(evened, "<green>" + previouslySlowed.getName()
                    + " no longer outnumbers you<reset> - their movement speed is back to normal.");
            return;
        }

        IncursionTeam outnumbered = enemyOf(slowed);
        int biggerSize = slowed == team1 ? size1 : size2;
        int smallerSize = slowed == team1 ? size2 : size1;
        long percent = Math.round(slowdown * 100.0);

        sendTeamMessage(slowed, "<yellow>Your team outnumbers " + outnumbered.getName()
                + " " + biggerSize + " to " + smallerSize
                + "<reset> - you move <yellow>" + percent + "%<reset> slower until that evens out.");
        sendTeamMessage(outnumbered, "<aqua>" + slowed.getName() + "<reset> outnumbers your team "
                + biggerSize + " to " + smallerSize + " - they move <yellow>" + percent
                + "%<reset> slower until that evens out.");
    }

    private void sendTeamMessage(IncursionTeam team, String message) {
        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            if (team == teamOf(participant.getUuid())) participant.sendMessage(message);
        }
    }

    private void logSpeedState(String reason) {
        logSpeedState(reason, teamSize(team1), teamSize(team2));
    }

    private void logSpeedState(String reason, int size1, int size2) {
        if (team1 == null || team2 == null) return;
        EventMain.getInstance().getLogger().info(String.format(Locale.ROOT,
                "Incursion speed (%s): configured multiplier x%.3f | %s | %s",
                reason, baseSpeedMultiplier, describeSpeed(team1, size1), describeSpeed(team2, size2)));
    }

    private String describeSpeed(IncursionTeam team, int size) {
        double multiplier = speedMultiplierFor(team);
        String note = (this.slowedTeam == team && this.slowdown > 0.0)
                ? String.format(Locale.ROOT, "slowed %.1f%%", this.slowdown * 100.0)
                : "not slowed";
        return String.format(Locale.ROOT, "%s (%d players): speed x%.3f, attribute modifier %+.3f (%s)",
                team.getName(), size, multiplier, multiplier - 1.0, note);
    }

    private int teamSize(@Nullable IncursionTeam team) {
        if (team == null) return 0;
        int size = 0;
        for (EventPlayer participant : this.participants) {
            if (team == teamOf(participant.getUuid()) && participant.getPlayer() != null) size++;
        }
        return size;
    }

    private void applySpeed(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance == null) return;

        double amount = speedMultiplierFor(teamOf(player.getUniqueId())) - 1.0;
        AttributeModifier current = instance.getModifiers().stream()
                .filter(modifier -> SPEED_KEY.equals(modifier.getKey()))
                .findFirst()
                .orElse(null);

        if (current != null) {
            if (Math.abs(current.getAmount() - amount) < 1.0e-6) return; // already at the right speed
            clearSpeed(player);
        }
        if (Math.abs(amount) < 1.0e-6) return;
        instance.addTransientModifier(new AttributeModifier(SPEED_KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private double speedMultiplierFor(@Nullable IncursionTeam team) {
        IncursionTeam slowed = this.slowedTeam;
        if (slowed == null || slowed != team) return baseSpeedMultiplier;
        return baseSpeedMultiplier * (1.0 - this.slowdown);
    }

    private static void clearSpeed(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance == null) return;
        instance.getModifiers().stream()
                .filter(modifier -> SPEED_KEY.equals(modifier.getKey()))
                .toList()
                .forEach(instance::removeModifier);
    }

    private static void restoreVitals(Player player) {
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.clearActivePotionEffects();
        player.setFoodLevel(20);
        player.setSaturation(20f);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
    }

    private void clearPlayerState(EventPlayer participant) {
        UUID uuid = participant.getUuid();
        invincibleUntil.remove(uuid);
        respawning.remove(uuid);
        frozenAt.remove(uuid);
        charging.remove(uuid);
        chargeReady.remove(uuid);
        lastDamageCredit.remove(uuid);
        participant.operatePlayer(player -> {
            unfreeze(player);
            clearSpeed(player);
        });
    }

    private void equipKit(Player player, IncursionTeam team) {
        PlayerInventory inventory = player.getInventory();
        inventory.setChestplate(teamChestplate(team));
        inventory.setItem(Weapon.MELEE.slot, Weapon.MELEE.item());
        inventory.setItem(Weapon.SNIPER.slot, Weapon.SNIPER.item());
        inventory.setItem(Weapon.SHOTGUN.slot, Weapon.SHOTGUN.item());
        inventory.setItem(Weapon.SIDEARM.slot, Weapon.SIDEARM.item());
        inventory.setHeldItemSlot(Weapon.MELEE.slot);
    }

    private ItemStack teamChestplate(IncursionTeam team) {
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        chestplate.editMeta(LeatherArmorMeta.class, meta -> {
            meta.setColor(team.getArmorColor());
            meta.displayName(Component.text(team.getName(), team.getColor()).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            meta.getPersistentDataContainer().set(KIT_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
        });
        return chestplate;
    }

    private static boolean isTeamArmor(@Nullable ItemStack item) {
        return item != null && Util.hasPersistentKey(item, KIT_KEY);
    }

    @Nullable
    private static Weapon weaponOf(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(WEAPON_KEY, PersistentDataType.STRING);
        return raw == null ? null : Util.getEnumFromString(Weapon.class, raw);
    }

    private static boolean readyToFire(Player player, Material material) {
        if (!player.hasCooldown(material)) return true;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 0.5f);
        return false;
    }

    private static boolean stillAiming(Player player, int startSlot) {
        return player.getInventory().getHeldItemSlot() == startSlot
                && weaponOf(player.getInventory().getItemInMainHand()) == Weapon.SNIPER;
    }

    private void tickSniperCharge() {
        for (UUID uuid : charging.keySet()) {
            EventPlayer participant = participantOf(uuid);
            if (participant == null) {
                charging.remove(uuid);
                chargeReady.remove(uuid);
                continue;
            }

            participant.operatePlayer(player -> {
                if (!player.hasActiveItem() || weaponOf(player.getActiveItem()) != Weapon.SNIPER) {
                    Integer startSlot = charging.remove(uuid);
                    boolean wasCharged = chargeReady.remove(uuid);
                    if (startSlot == null) return;

                    player.clearTitle();
                    if (!wasCharged || isOutOfPlay(uuid) || !stillAiming(player, startSlot)) return;

                    IncursionTeam team = teamOf(uuid);
                    if (team != null) fireOverwatch(player, team);
                    return;
                }

                int chargeTicks = Math.max(1, definition.getSniper().getChargeTicks());
                int held = player.getActiveItemUsedTime();
                if (held >= chargeTicks) {
                    if (chargeReady.add(uuid)) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                    }
                    player.showTitle(Title.title(
                            Util.color("<green><b>⌄"),
                            Util.color("<green><b>READY"),
                            CHARGE_TITLE_TIMES));
                    return;
                }

                int filled = (int) Math.round(((double) held / chargeTicks) * CHARGE_BAR_SEGMENTS);
                player.showTitle(Title.title(
                        Util.color("<yellow><b>⌄"),
                        Util.color("<yellow>" + "|".repeat(filled) + "<dark_gray>" + "|".repeat(CHARGE_BAR_SEGMENTS - filled)),
                        CHARGE_TITLE_TIMES));
            });
        }
    }

    private void fireOverwatch(Player shooter, IncursionTeam team) {
        IncursionDefinition.SniperSettings settings = definition.getSniper();
        if (settings.getCooldownTicks() > 0) {
            shooter.setCooldown(Material.SPYGLASS, settings.getCooldownTicks());
        }

        Location eye = shooter.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        eye.getWorld().playSound(eye, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
        eye.getWorld().playSound(eye, Sound.ITEM_TRIDENT_THROW, 1.1f, 0.5f);
        eye.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.35f, 1.4f);

        long firedAt = shotTime(shooter);

        advanceBeam(team, eye, direction, 0.0, Math.max(1.0, settings.getRange()),
                stoppedAt -> applyBeamDamage(shooter, team, eye, direction, stoppedAt, firedAt));
    }

    private void advanceBeam(IncursionTeam team, Location eye, Vector direction,
                             double startDistance, double maxRange, DoubleConsumer onStopped) {
        World world = eye.getWorld();
        Vector origin = eye.toVector();
        int step = 0;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        double drawnTo = startDistance;

        for (double distance = startDistance; distance <= maxRange; distance += BEAM_STEP, step++) {
            double x = eye.getX() + (direction.getX() * distance);
            double y = eye.getY() + (direction.getY() * distance);
            double z = eye.getZ() + (direction.getZ() * distance);

            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);

            if (!Bukkit.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) {
                double resumeAt = distance;
                Executors.sync(world, blockX >> 4, blockZ >> 4,
                        () -> advanceBeam(team, eye, direction, resumeAt, maxRange, onStopped));
                return;
            }

            // Steps can land in the same block more than once
            boolean sameBlock = blockX == lastX && blockY == lastY && blockZ == lastZ;
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;

            double stoppedAt = sameBlock
                    ? -1
                    : solidHitDistance(world.getBlockAt(blockX, blockY, blockZ), origin, direction, maxRange);

            if (stoppedAt >= 0) {
                // Fences and walls collide above their own block
                double impactAt = Math.max(stoppedAt, drawnTo);
                Location impact = new Location(world,
                        eye.getX() + (direction.getX() * impactAt),
                        eye.getY() + (direction.getY() * impactAt),
                        eye.getZ() + (direction.getZ() * impactAt));

                world.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CRIT, impact, 8, 0.1, 0.1, 0.1, 0.25);
                world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.9f);
                onStopped.accept(impactAt);
                return;
            }

            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, team.getBeamDust());
            if (step % 4 == 0) {
                world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
            drawnTo = distance;
        }

        onStopped.accept(maxRange);
    }

    // Projects every enemy onto the beam and hits everyone within the configured radius of it
    private void applyBeamDamage(Player shooter, IncursionTeam shooterTeam, Location eye, Vector direction,
                                 double maxDistance, long firedAt) {
        IncursionDefinition.SniperSettings settings = definition.getSniper();
        int hits = 0;
        List<Double> headshots = new ArrayList<>();

        for (Target target : targetsOf(shooterTeam, firedAt)) {
            BoundingBox hitbox = target.expandedHitbox(settings.getHitRadius());
            double maxReach = maxDistance + hitbox.getHeight();
            if (eye.toVector().distanceSquared(hitbox.getCenter()) > maxReach * maxReach) continue;

            if (!beamTouches(hitbox, eye, direction, maxDistance)) continue;

            boolean headshot = beamTouches(target.headHitbox(settings.getHitRadius()), eye, direction, maxDistance);
            double damage = headshot ? settings.getDamage() * settings.getHeadshotMultiplier() : settings.getDamage();

            dealTrueDamage(target.entity(), shooter, eye, damage, settings.getKnockback(), Weapon.SNIPER.getPlainName(), headshot);
            if (headshot) {
                headshotEffect(target);
                headshots.add(target.headHitbox(0.0).getCenter().distance(eye.toVector()));
            }
            hits++;
        }

        if (hits > 0) {
            Executors.runSync(shooter, () -> {
                if (headshots.isEmpty()) hitFeedback(shooter);
                else headshotFeedback(shooter, headshots);
            });
        }
    }

    private static boolean beamTouches(BoundingBox box, Location eye, Vector direction, double maxDistance) {
        Vector origin = eye.toVector();
        return box.contains(origin) || box.rayTrace(origin, direction, maxDistance) != null;
    }

    private static void headshotEffect(Target target) {
        LivingEntity entity = target.entity();
        Vector center = target.headHitbox(0.0).getCenter();

        Executors.runSync(entity, () -> {
            World world = entity.getWorld();
            Location at = center.toLocation(world);
            world.spawnParticle(Particle.CRIT, at, 12, 0.15, 0.15, 0.15, 0.35);
            world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
        });
    }

    private void fireBlunderhorn(Player shooter, IncursionTeam team) {
        IncursionDefinition.ShotgunSettings settings = definition.getShotgun();
        if (settings.getCooldownTicks() > 0) shooter.setCooldown(Material.GOAT_HORN, settings.getCooldownTicks());

        Location muzzle = shooter.getEyeLocation();
        Vector direction = muzzle.getDirection().normalize();
        double range = Math.max(0.5, settings.getRange());
        double falloff = Math.clamp(settings.getDamageFalloff(), 0.0, 1.0);

        muzzle.getWorld().playSound(muzzle, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
        muzzle.getWorld().playSound(muzzle, Sound.ITEM_FIRECHARGE_USE, 0.9f, 0.7f);
        sprayCone(muzzle, direction, range, settings.getConeDegrees(), team);

        int hits = 0;
        double halfAngle = Math.toRadians(Math.max(1.0, settings.getConeDegrees()) / 2.0);
        double tanHalfAngle = Math.tan(halfAngle);
        double cosHalfAngle = Math.cos(halfAngle);
        Vector apex = muzzle.toVector();

        for (Target target : targetsOf(team, shotTime(shooter))) {
            BoundingBox hitbox = target.hitbox();
            double distance = coneHitDistance(apex, direction, range, tanHalfAngle, cosHalfAngle, hitbox);
            if (distance < 0) continue;

            // Occlusion has to be checked towards the target, not down the middle of the cone
            Vector toTarget = hitbox.getCenter().subtract(apex);
            double length = toTarget.length();
            if (length > 1.0E-4 && !hasClearShot(muzzle, toTarget.multiply(1.0 / length), length)) continue;

            dealTrueDamage(target.entity(), shooter, muzzle,
                    settings.getDamage() * (1.0 - (falloff * (distance / range))), settings.getKnockback(),
                    Weapon.SHOTGUN.getPlainName());
            hits++;
        }

        if (hits > 0) hitFeedback(shooter);
    }

    // Distance from the muzzle to the nearest part of a hitbox if it falls inside the cone, or -1 if it doesn't (inexpensive approximation)
    private static double coneHitDistance(Vector apex, Vector axis, double range, double tanHalfAngle, double cosHalfAngle, BoundingBox hitbox) {
        double height = hitbox.getHeight();
        double radius = Math.max(Math.max(hitbox.getWidthX(), hitbox.getWidthZ()) / 2.0, height / 6.0);

        // Nothing beyond this can reach the cone
        double maxReach = range + (height / 2.0) + radius;
        if (apex.distanceSquared(hitbox.getCenter()) > maxReach * maxReach) return -1;

        double best = -1;
        for (double fraction : BODY_SAMPLE_FRACTIONS) {
            double dx = hitbox.getCenterX() - apex.getX();
            double dy = (hitbox.getMinY() + (height * fraction)) - apex.getY();
            double dz = hitbox.getCenterZ() - apex.getZ();

            double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
            if (distance <= radius) return 0; // muzzle is inside target

            double along = (dx * axis.getX()) + (dy * axis.getY()) + (dz * axis.getZ());
            if (along < 0 || along - radius > range) continue;

            double offX = dx - (axis.getX() * along);
            double offY = dy - (axis.getY() * along);
            double offZ = dz - (axis.getZ() * along);
            double fromAxis = Math.sqrt((offX * offX) + (offY * offY) + (offZ * offZ));

            // The cone widens with distance, and a sphere of <radius> widens what counts as touching it
            if (fromAxis > (along * tanHalfAngle) + (radius / cosHalfAngle)) continue;

            double toSurface = Math.max(0, distance - radius);
            if (toSurface <= range && (best < 0 || toSurface < best)) best = toSurface;
        }
        return best;
    }

    private void sprayCone(Location muzzle, Vector direction, double range, double coneDegrees, IncursionTeam team) {
        World world = muzzle.getWorld();
        spawnParticleIfOwned(world, muzzle, Particle.SMOKE, 16, 0.1, 0.6);

        Vector reference = Math.abs(direction.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector right = direction.clone().crossProduct(reference).normalize();
        Vector up = right.clone().crossProduct(direction).normalize();
        double spread = Math.tan(Math.toRadians(coneDegrees / 2.0));

        for (int pellet = 0; pellet < SHOTGUN_PELLETS; pellet++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double offset = spread * Math.sqrt(RANDOM.nextDouble());
            Vector pelletDirection = direction.clone()
                    .add(right.clone().multiply(Math.cos(angle) * offset))
                    .add(up.clone().multiply(Math.sin(angle) * offset))
                    .normalize();

            for (double distance = 0.6; distance <= range; distance += 0.7) {
                Location point = muzzle.clone().add(pelletDirection.clone().multiply(distance));
                spawnParticleIfOwned(world, point, Particle.DUST, 1, 0, 0, team.getPelletDust());
            }
        }
    }

    private void fireSpitter(Player shooter, IncursionTeam team) {
        IncursionDefinition.SpitterSettings settings = definition.getSpitter();
        if (settings.getCooldownTicks() > 0) shooter.setCooldown(Material.PUFFERFISH, settings.getCooldownTicks());

        Location muzzle = mainHandLocation(shooter);
        Location aim = shooter.getEyeLocation();
        aim.setPitch((float) Math.max(-90.0, aim.getPitch() - settings.getLaunchAngleDegrees()));
        Vector velocity = aim.getDirection().normalize().multiply(Math.max(0.05, settings.getSpeed()));

        muzzle.getWorld().playSound(muzzle, Sound.ENCHANT_THORNS_HIT, 0.9f, 1.5f);
        muzzle.getWorld().playSound(muzzle, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 0.5f, 1.3f);
        muzzle.getWorld().spawnParticle(Particle.SPLASH, muzzle, 6, 0.05, 0.05, 0.05, 0.02);
        muzzle.getWorld().spawnParticle(Particle.DUST, muzzle, 6, 0.06, 0.06, 0.06, 0, SPIT_DUST);

        advanceSpit(shooter, team, muzzle, velocity, 0.0, rewindOf(shooter));
    }

    // Advances the stream by one tick, then reschedules itself onto whichever region owns the position it ended up in
    private void advanceSpit(Player shooter, IncursionTeam team, Location position, Vector velocity,
                             double travelled, long rewind) {
        IncursionDefinition.SpitterSettings settings = definition.getSpitter();
        double range = Math.max(0.01, settings.getRange());
        if (!this.active || this.stopping || travelled >= range) return;

        World world = position.getWorld();
        int steps = Math.max(1, settings.getTrailSteps());
        Vector stepVector = velocity.clone().multiply(1.0 / steps);
        double stepLength = stepVector.length();

        List<Target> targets = targetsOf(team, System.nanoTime() - rewind);
        double hitRadius = settings.getHitRadius();

        Vector stepDirection = stepLength > 1.0E-6 ? stepVector.clone().multiply(1.0 / stepLength) : null;

        Location point = position.clone();
        for (int step = 0; step < steps; step++) {
            Vector from = point.toVector();
            point.add(stepVector);
            travelled += stepLength;

            if (!Bukkit.isOwnedByCurrentRegion(point)) {
                Location resumeAt = point.clone();
                double resumeTravelled = travelled;
                Executors.sync(resumeAt, () -> advanceSpit(shooter, team, resumeAt, velocity, resumeTravelled, rewind));
                return;
            }

            if (stepDirection != null
                    && solidHitDistance(world.getBlockAt(point), from, stepDirection, stepLength) >= 0) {
                splash(world, point);
                return;
            }

            for (Target target : targets) {
                if (!target.containsWithin(hitRadius, point.getX(), point.getY(), point.getZ())) continue;

                double falloff = Math.clamp(settings.getDamageFalloff(), 0.0, 1.0);
                dealTrueDamage(target.entity(), shooter, position,
                        settings.getDamage() * (1.0 - (falloff * Math.min(1.0, travelled / range))),
                        settings.getKnockback(), Weapon.SIDEARM.getPlainName());

                splash(world, point);
                Executors.runSync(shooter, () -> hitFeedback(shooter));
                return;
            }

            world.spawnParticle(Particle.SPLASH, point, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0, SPIT_DUST);
            world.spawnParticle(Particle.DUST, point, 3, 0.07, 0.07, 0.07, 0, new Particle.DustOptions(team.armorColor, 0.35f));
            if (step % 2 == 0) {
                world.spawnParticle(Particle.FALLING_WATER, point, 1, 0.06, 0.06, 0.06, 0);
            }

            if (travelled >= range) {
                splash(world, point);
                return;
            }
        }

        Vector nextVelocity = velocity.clone().subtract(new Vector(0, settings.getGravity(), 0));
        Location next = point.clone();
        double nextTravelled = travelled;
        Executors.delayedSync(next, 1, () -> advanceSpit(shooter, team, next, nextVelocity, nextTravelled, rewind));
    }

    private static void splash(World world, Location at) {
        world.spawnParticle(Particle.SPLASH, at, 14, 0.15, 0.15, 0.15, 0.08);
        world.spawnParticle(Particle.DUST, at, 10, 0.18, 0.18, 0.18, 0, SPIT_DUST);
        world.spawnParticle(Particle.FALLING_WATER, at, 6, 0.15, 0.15, 0.15, 0);
        world.playSound(at, Sound.ENTITY_GENERIC_SPLASH, 0.35f, 1.6f);
    }

    // Roughly where the held item sits
    private static Location mainHandLocation(Player player) {
        Location eye = player.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        if (player.getMainHand() == MainHand.LEFT) right.multiply(-1);

        return eye.clone()
                .add(right.multiply(0.4))
                .add(eye.getDirection().multiply(0.4))
                .subtract(0, 1.0, 0);
    }

    private static void hitFeedback(Player shooter) {
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.4f);
    }

    private static void headshotFeedback(Player shooter, List<Double> headshots) {
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.9f);
        shooter.playSound(shooter.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.8f);
        headshots.sort(Double::compareTo);
        DecimalFormat format = new DecimalFormat("0.#");
        String subtitle = headshots.stream()
                .map(d -> format.format(d) + "m")
                .collect(Collectors.joining(", "));
        shooter.showTitle(Title.title(
                Util.color("<red>HEADSHOT"),
                Util.color("<gray>" + subtitle),
                HEADSHOT_TITLE_TIMES));
    }

    private void startOrbs() {
        for (Location spawn : definition.getOrbSpawns()) {
            if (spawn == null || spawn.getWorld() == null) continue;

            Orb orb = new Orb(spawn.clone().add(0.5, 0, 0.5));
            orbs.add(orb);
            orbTasks.add(Executors.repeatingSync(orb.marker, ORB_RENDER_PERIOD_TICKS, task -> {
                if (!this.active || this.stopping) {
                    task.cancel();
                    return;
                }
                orb.render();
            }));
        }
    }

    private void tryCollectOrb(EventPlayer participant, Player player, Location at) {
        if (orbs.isEmpty()) return;

        // Middle of the player (where this move is taking them)
        double centreY = at.getY() + PLAYER_HALF_HEIGHT;

        for (Orb orb : orbs) {
            if (!orb.marker.getWorld().equals(at.getWorld())) continue;
            if (!orb.available.get() || !orb.pickupBox.contains(at.getX(), centreY, at.getZ())) continue;
            if (!orb.collect(definition.getOrbRespawnTicks())) continue; // someone else got there first

            Grenade grenade = Util.getRandom(GRENADES);
            player.getInventory().addItem(grenade.item());

            player.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            player.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            spawnParticleIfOwned(player.getWorld(), orb.centre, Particle.END_ROD, 18, 0.2, 0.08);

            participant.sendNoPrefixedMessage(Util.prefixed("You picked up ")
                    .append(Util.color(grenade.getDisplayName()))
                    .append(Util.color("<gray>!")));
            return;
        }
    }

    @Nullable
    private static Grenade grenadeOf(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(GRENADE_KEY, PersistentDataType.STRING);
        return raw == null ? null : Util.getEnumFromString(Grenade.class, raw);
    }

    private void detonate(Grenade grenade, Player thrower, Location at) {
        IncursionDefinition.GrenadeSettings settings = definition.getGrenade();
        World world = at.getWorld();

        world.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        world.spawnParticle(Particle.DUST, at, 45, 0.7, 0.7, 0.7, 0.1, grenade.getBurstDust());
        world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);
        world.playSound(at, Sound.ENTITY_CHICKEN_HURT, 1.4f, 0.9f);

        IncursionTeam throwerTeam = teamOf(thrower.getUniqueId());
        if (throwerTeam == null) return;

        double radius = Math.max(0.5, settings.getRadius());
        double falloff = Math.clamp(settings.getDamageFalloff(), 0.0, 1.0);
        boolean connected = false;

        for (Target target : targetsOf(throwerTeam, System.nanoTime())) {
            Vector toTarget = target.hitbox().getCenter().subtract(at.toVector());
            double distance = toTarget.length();
            if (distance > radius) continue;

            if (settings.isRequireLineOfSight() && distance > 1.0E-4
                    && !hasClearShot(at, toTarget.clone().multiply(1.0 / distance), distance)) {
                continue;
            }

            dealTrueDamage(target.entity(), thrower, at,
                    settings.getDamage() * (1.0 - (falloff * (distance / radius))), settings.getKnockback(),
                    grenade.getPlainName());
            applyGrenadeEffect(grenade, target.entity(), settings);
            connected = true;
        }

        if (connected) Executors.runSync(thrower, () -> hitFeedback(thrower));
    }

    private void applyGrenadeEffect(Grenade grenade, LivingEntity target, IncursionDefinition.GrenadeSettings settings) {
        if (grenade == Grenade.INCENDIARY) {
            Miniboss miniboss = minibossesByEntity.get(target.getUniqueId());
            if (miniboss != null) miniboss.burnFor(settings.getFireTicks());
        }

        switch (grenade) {
            case CRYO -> Executors.runSync(target, () -> {
                target.setFreezeTicks(Math.max(target.getFreezeTicks(), settings.getFreezeTicks()));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        settings.getSlownessTicks(), Math.max(0, settings.getSlownessAmplifier()), false, true, true));
            });
            case INCENDIARY -> Executors.runSync(target,
                    () -> target.setFireTicks(Math.max(target.getFireTicks(), settings.getFireTicks())));
            case NORMAL -> {
                // Blast only
            }
        }
    }

    private void dealTrueDamage(LivingEntity target, Player attacker, Location source, double damage, double knockback, String sourceName) {
        dealTrueDamage(target, attacker, source, damage, knockback, sourceName, false);
    }

    @SuppressWarnings("removal")
    private void dealTrueDamage(LivingEntity target, Player attacker, Location source, double damage, double knockback,
                                String sourceName, boolean headshot) {
        if (damage <= 0) return;

        boolean isPlayer = target instanceof Player;
        if (isPlayer) {
            lastDamageCredit.put(target.getUniqueId(),
                    new DamageCredit(attacker.getUniqueId(), System.currentTimeMillis(), sourceName, damage, headshot));
        }

        Executors.runSync(target, () -> {
            Location targetLocation = target.getLocation();
            double dx = source.getX() - targetLocation.getX();
            double dz = source.getZ() - targetLocation.getZ();

            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, targetLocation.clone().add(0, 1, 0), 6, 0.2, 0.3, 0.2, 0.1);
            if (target instanceof Player player) player.playSound(targetLocation, Sound.ENTITY_PLAYER_HURT, 1f, 1f);
            else if (target.getHurtSound() != null) target.getWorld().playSound(targetLocation, target.getHurtSound(), 1f, 1f);
            target.playHurtAnimation(hurtDirection(targetLocation.getYaw(), dx, dz));

            if (knockback > 0) {
                if (dx == 0 && dz == 0) {
                    // Hit from directly above or below, so there is no direction to be pushed in
                    target.setVelocity(target.getVelocity().add(new Vector(0, knockback * 0.5, 0)));
                } else {
                    target.knockback(knockback, dx, dz);
                }
            }

            double remaining = target.getHealth() - damage;
            if (!isPlayer && remaining <= 0) {
                Miniboss miniboss = minibossesByEntity.get(target.getUniqueId());
                if (miniboss != null) {
                    miniboss.slay(attacker);
                    return;
                }
            }

            // Our damage has no source of its own, so a lethal hit stores whatever last hit the target.
            // Death listeners read that (InventoryRollbackPlus) and would otherwise follow it to a
            // grenade egg the server has already discarded, which fails the region thread check:
            if (isPlayer && remaining <= 0) unsafe(() -> target.setLastDamageCause(null));

            target.setHealth(Math.max(0.0, remaining));
        });
    }

    // target -> damage source (0 = front, 90 = right, 180 = behind)
    private static float hurtDirection(float targetYaw, double dx, double dz) {
        if (dx == 0 && dz == 0) return 0f;
        double towardsSource = Math.toDegrees(Math.atan2(-dx, dz));
        return (float) (((towardsSource - targetYaw) % 360.0 + 360.0) % 360.0);
    }

    @Nullable
    private DamageCredit recentDamageCredit(Player victim) {
        DamageCredit credit = lastDamageCredit.remove(victim.getUniqueId());
        if (credit == null || System.currentTimeMillis() - credit.at() > DAMAGE_CREDIT_MILLIS) return null;

        return credit.attacker().equals(victim.getUniqueId()) ? null : credit;
    }

    private static void sendDeathMessage(Player victim, Player killer, DamageCredit credit) {
        Executors.runSync(killer, () -> {
            String hearts = trimmed(killer.getHealth() / 2.0);
            victim.sendMessage(Util.color("<red>You were killed by " + killer.getName() + " (" + hearts + "❤) using "
                    + credit.sourceName() + " with " + trimmed(credit.damage()) + "dmg"
                    + (credit.headshot() ? " (headshot)" : "")));
        });
    }

    private static String trimmed(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.floor(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }

    private static String stripTags(String display) {
        return PlainTextComponentSerializer.plainText().serialize(Util.color(display));
    }

    // Everything <team> may shoot, as it stood at <at> (System.nanoTime() output)
    // Who is in play is always checked for right now, so a rewound shot can
    // never hit someone who has since respawned, gone invincible or left
    private List<Target> targetsOf(IncursionTeam team, long at) {
        List<Target> targets = new ArrayList<>();

        for (EventPlayer participant : this.participants) {
            UUID uuid = participant.getUuid();
            IncursionTeam theirs = teamOf(uuid);
            if (theirs == null || theirs == team || isOutOfPlay(uuid)) continue;

            Player player = participant.getPlayer();
            if (player == null) continue;

            Target target = targetAt(player, at);
            if (target != null) targets.add(target);
        }

        for (Miniboss miniboss : this.minibosses) {
            Target target = miniboss.targetAt(at);
            if (target != null) targets.add(target);
        }

        return targets;
    }

    // Where this entity was at <at>, or null if nothing has been recorded
    // for it and we are on the wrong thread to look its hitbox up live
    @Nullable
    private Target targetAt(LivingEntity entity, long at) {
        Trail trail = trails.get(entity.getUniqueId());
        Snapshot snapshot = trail == null ? null : trail.at(at);
        if (snapshot != null) return new Target(entity, snapshot.hitbox(), snapshot.eyeHeight());

        return Bukkit.isOwnedByCurrentRegion(entity)
                ? new Target(entity, entity.getBoundingBox(), entity.getEyeHeight())
                : null;
    }

    // How far behind live a shot from this player is judged: ping plus render delay
    // (capped so victims are not shot long after they reached cover)
    private long rewindOf(Player shooter) {
        IncursionDefinition.LagCompensationSettings settings = definition.getLagCompensation();
        if (!settings.isEnabled()) return 0L;

        long cap = Math.min(MAX_REWIND_NANOS,
                TimeUnit.MILLISECONDS.toNanos(Math.max(0, settings.getMaxRewindMillis())));
        long asked = TimeUnit.MILLISECONDS.toNanos(
                latency.pingMillis(shooter) + Math.max(0, settings.getInterpolationMillis()));

        return Math.clamp(asked, 0L, cap);
    }

    // The moment a shot fired by this player right now should be judged at
    private long shotTime(Player shooter) {
        return System.nanoTime() - rewindOf(shooter);
    }

    private List<Player> onlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player != null) players.add(player);
        }
        return players;
    }

    // Keeps a recorder ticking on every participant and living miniboss (and forgets whatever has left the game)
    private void tickRecorders() {
        Set<UUID> live = new HashSet<>();

        for (Player player : onlineParticipants()) {
            live.add(player.getUniqueId());
            startRecording(player);
        }

        for (Miniboss miniboss : this.minibosses) {
            Mob mob = miniboss.entity;
            if (mob == null || !miniboss.alive) continue;
            live.add(mob.getUniqueId());
            startRecording(mob);
        }

        recorders.entrySet().removeIf(entry -> {
            if (live.contains(entry.getKey())) return false;
            entry.getValue().task().cancel();
            return true;
        });
        trails.keySet().removeIf(uuid -> !live.contains(uuid));
    }

    private void startRecording(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        Recorder running = recorders.get(uuid);
        if (running != null && running.entity() == entity && !running.task().isCancelled()) return;

        if (running != null) running.task().cancel();

        Trail trail = new Trail();
        ScheduledTask task = Executors.repeatingSync(entity, 1L, _ -> trail.record(entity));
        if (task == null) return; // Already gone, the next sweep can try again

        trails.put(uuid, trail);
        recorders.put(uuid, new Recorder(entity, task));
    }

    private void stopRecording() {
        recorders.values().forEach(recorder -> recorder.task().cancel());
        recorders.clear();
        trails.clear();
    }

    private void forgetTrail(UUID uuid) {
        Trail trail = trails.get(uuid);
        if (trail != null) trail.clear();
    }

    // What an entity's hitbox looked like on one tick
    private record Snapshot(long takenAt, BoundingBox hitbox, double eyeHeight) {}

    private record Recorder(LivingEntity entity, ScheduledTask task) {}

    // One entity's last HISTORY_TICKS hitboxes
    private static final class Trail {

        private final Snapshot[] snapshots = new Snapshot[HISTORY_TICKS];
        private volatile int written;

        private void record(LivingEntity entity) {
            int index = this.written;
            snapshots[Math.floorMod(index, HISTORY_TICKS)] =
                    new Snapshot(System.nanoTime(), entity.getBoundingBox(), entity.getEyeHeight());
            this.written = index + 1;
        }

        private void clear() {
            Arrays.fill(snapshots, null);
            this.written = 0;
        }

        // The hitbox this entity had at <when>, interpolated between the ticks either side of it
        @Nullable
        private Snapshot at(long when) {
            int count = this.written; // Reading this first "publishes" every snapshot below it

            Snapshot newer = null;
            for (int age = 0; age < Math.min(count, HISTORY_TICKS); age++) {
                Snapshot snapshot = snapshots[Math.floorMod(count - 1 - age, HISTORY_TICKS)];
                if (snapshot == null) break;
                if (snapshot.takenAt() <= when) {
                    return newer == null ? snapshot : between(snapshot, newer, when);
                }
                newer = snapshot;
            }
            return newer;
        }

        private static Snapshot between(Snapshot older, Snapshot newer, long when) {
            long span = newer.takenAt() - older.takenAt();
            if (span <= 0) return newer;

            double progress = Math.clamp((double) (when - older.takenAt()) / span, 0.0, 1.0);
            BoundingBox from = older.hitbox();
            BoundingBox to = newer.hitbox();

            return new Snapshot(when, new BoundingBox(
                    lerp(from.getMinX(), to.getMinX(), progress),
                    lerp(from.getMinY(), to.getMinY(), progress),
                    lerp(from.getMinZ(), to.getMinZ(), progress),
                    lerp(from.getMaxX(), to.getMaxX(), progress),
                    lerp(from.getMaxY(), to.getMaxY(), progress),
                    lerp(from.getMaxZ(), to.getMaxZ(), progress)),
                    lerp(older.eyeHeight(), newer.eyeHeight(), progress));
        }

        private static double lerp(double from, double to, double progress) {
            return from + ((to - from) * progress);
        }
    }

    // A shootable thing, its last known hitbox and the eye height that hitbox was taken at
    private record Target(LivingEntity entity, BoundingBox hitbox, double eyeHeight) {
        private BoundingBox expandedHitbox(double radius) {
            return hitbox.clone().expand(radius);
        }

        private BoundingBox headHitbox(double radius) {
            double chin = hitbox.getMinY() + eyeHeight - HEAD_DROP_BELOW_EYE;

            double lowest = hitbox.getMaxY() - (hitbox.getHeight() * MAX_HEAD_FRACTION);

            return hitbox.clone()
                    .resize(hitbox.getMinX(), Math.max(chin, lowest), hitbox.getMinZ(),
                            hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ())
                    .expand(radius);
        }

        private boolean containsWithin(double radius, double x, double y, double z) {
            return x >= hitbox.getMinX() - radius && x < hitbox.getMaxX() + radius
                    && y >= hitbox.getMinY() - radius && y < hitbox.getMaxY() + radius
                    && z >= hitbox.getMinZ() - radius && z < hitbox.getMaxZ() + radius;
        }
    }

    // Cheap-ish line of sight check to make sure shots don't travel through walls
    private static boolean hasClearShot(Location from, Vector direction, double distance) {
        World world = from.getWorld();
        Vector origin = from.toVector();
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;

        for (double travelled = BEAM_STEP; travelled < distance; travelled += BEAM_STEP) {
            int blockX = (int) Math.floor(from.getX() + (direction.getX() * travelled));
            int blockY = (int) Math.floor(from.getY() + (direction.getY() * travelled));
            int blockZ = (int) Math.floor(from.getZ() + (direction.getZ() * travelled));

            if (blockX == lastX && blockY == lastY && blockZ == lastZ) continue; // Same block as the last step
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;

            if (!Bukkit.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) return true;
            if (solidHitDistance(world.getBlockAt(blockX, blockY, blockZ), origin, direction, distance) >= 0) {
                return false;
            }
        }
        return true;
    }

    // How far along the ray it first meets something solid inside this block, or -1 if it doesn't
    private static double solidHitDistance(Block block, Vector origin, Vector direction, double maxDistance) {
        if (!couldStopShots(block)) return -1;

        BoundingBox bounds = block.getBoundingBox();
        if (bounds.getVolume() <= 0) return -1;
        if (bounds.contains(origin)) return 0; // Fired from inside the block itself

        // <direction> is normalized, so this is the distance along the ray
        RayTraceResult hit = bounds.rayTrace(origin, direction, maxDistance);
        return hit == null ? -1 : hit.getHitPosition().distance(origin);
    }

    private static boolean couldStopShots(Block block) {
        return !block.isPassable() && block.getType() != Material.BARRIER;
    }

    // <base> pulled <amount> of the way towards <tint>, 0 leaving it alone and 1 replacing it
    private static Color blend(Color base, Color tint, double amount) {
        double towards = Math.clamp(amount, 0.0, 1.0);
        double keep = 1.0 - towards;

        return Color.fromRGB(
                (int) Math.round((base.getRed() * keep) + (tint.getRed() * towards)),
                (int) Math.round((base.getGreen() * keep) + (tint.getGreen() * towards)),
                (int) Math.round((base.getBlue() * keep) + (tint.getBlue() * towards)));
    }

    // Drop particles rather than rescheduling them when they'd land in another region (shouldn't happen anyway)
    private static void spawnParticleIfOwned(World world, Location at, Particle particle, int count, double offset, double extra) {
        if (!Bukkit.isOwnedByCurrentRegion(at)) return;
        world.spawnParticle(particle, at, count, offset, offset, offset, extra);
    }
    private static <T> void spawnParticleIfOwned(World world, Location at, Particle particle, int count, double offset, double extra, T data) {
        if (!Bukkit.isOwnedByCurrentRegion(at)) return;
        world.spawnParticle(particle, at, count, offset, offset, offset, extra, data);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        IncursionTeam team = teamOf(uuid);
        if (team == null) return;

        Location to = event.getTo();
        Location frozen = frozenAt.get(uuid);
        if (frozen != null) {
            if (event instanceof PlayerTeleportEvent) return;
            if (to.getX() != event.getFrom().getX() || to.getZ() != event.getFrom().getZ()) {
                Location back = frozen.clone();
                back.setY(to.getY()); // don't fight gravity
                back.setYaw(to.getYaw());
                back.setPitch(to.getPitch());
                event.setTo(back);
            }
            return;
        }

        if (respawning.contains(uuid) || kickoffInProgress) return;
        if (event.getFrom().getBlockX() == to.getBlockX()
                && event.getFrom().getBlockY() == to.getBlockY()
                && event.getFrom().getBlockZ() == to.getBlockZ()) {
            return; // Optimization
        }

        EventPlayer participant = participantOf(uuid);
        if (participant == null) return;

        tryCollectOrb(participant, player, to);

        if (enemyOf(team).getSide().hole().contains(to)) {
            scoreHole(participant, team, to);
        } else if (team.getSide().hole().contains(to)) {
            participant.sendMessage("<red>That's your own hole! No points for you.");
            respawnFlow(participant, team);
        } else if (!this.boundingBox.contains(to)) {
            participant.sendMessage("<red>Don't leave the arena!");
            respawnFlow(participant, team);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!this.active || this.stopping) return;
        forgetTrail(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!this.active || this.stopping) return;

        Player victim = event.getEntity();
        IncursionTeam victimTeam = teamOf(victim.getUniqueId());
        if (victimTeam == null) return;

        event.setCancelled(true);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.deathMessage(null);
        unsafe(() -> event.setDeathSound(Sound.ITEM_TOTEM_USE));

        AttributeInstance maxHealth = victim.getAttribute(Attribute.MAX_HEALTH);
        event.setReviveHealth(maxHealth != null ? maxHealth.getValue() : 20.0);

        DamageCredit credit = recentDamageCredit(victim);
        Player killer = resolveKiller(event.getDamageSource().getCausingEntity(), victim);
        // True damage bypasses the damage source, so fall back to who last shot them
        if (killer == null && credit != null) killer = Bukkit.getPlayer(credit.attacker());
        if (killer != null) {
            if (credit != null && credit.attacker().equals(killer.getUniqueId())) sendDeathMessage(victim, killer, credit);

            IncursionTeam killerTeam = teamOf(killer.getUniqueId());
            EventPlayer killerParticipant = participantOf(killer.getUniqueId());
            if (killerTeam != null && killerParticipant != null && killerTeam != victimTeam) {
                addPoints(killerParticipant, killerTeam, definition.getKillPoints());
                killerParticipant.sendMessage("<green>+" + definition.getKillPoints() + " <gray>for eliminating " + victim.getName());
            }
        }

        EventPlayer participant = participantOf(victim.getUniqueId());
        if (participant != null) Executors.delayedSync(victim, 1, () -> respawnFlow(participant, victimTeam));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!this.active || this.stopping || minibossesByEntity.isEmpty()) return;

        Miniboss miniboss = minibossesByEntity.get(event.getEntity().getUniqueId());
        if (miniboss == null) return;

        event.getDrops().clear();
        event.setDroppedExp(0);
        miniboss.slay(event.getEntity().getKiller());
    }

    // Might not be needed, but better safe than sorry
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        IncursionTeam team = teamOf(player.getUniqueId());
        EventPlayer participant = participantOf(player.getUniqueId());
        if (team == null || participant == null) return;

        event.setRespawnLocation(team.getSide().spawnArea().randomSpawn());
        Executors.delayedSync(player, 1, () -> respawnFlow(participant, team));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!this.active || this.stopping) return;

        Entity damager = damagerOf(event);
        if (damager instanceof Firework firework && Util.hasPersistentKey(firework, FIREWORK_KEY)) {
            event.setCancelled(true);
            return;
        }

        if (FIRE_DAMAGE_CAUSES.contains(event.getCause())) {
            Miniboss miniboss = minibossesByEntity.get(event.getEntity().getUniqueId());
            if (miniboss != null && !miniboss.isBurning()) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(event.getEntity() instanceof Player victim)) return;
        IncursionTeam victimTeam = teamOf(victim.getUniqueId());
        if (victimTeam == null) return;

        if (isOutOfPlay(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && isLethalFall(victim, event)) {
            event.setDamage(LETHAL_FALL_DAMAGE);
            return;
        }

        Player attacker = resolveKiller(damager, victim);
        if (attacker == null) return;

        IncursionTeam attackerTeam = teamOf(attacker.getUniqueId());
        if (attackerTeam == null) return;

        if (attackerTeam == victimTeam || isOutOfPlay(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (damager instanceof Player) {
            lastDamageCredit.put(victim.getUniqueId(), new DamageCredit(attacker.getUniqueId(),
                    System.currentTimeMillis(), Weapon.MELEE.getPlainName(), event.getFinalDamage(), false));
        }
    }

    // TODO: Is this a good idea?
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!this.active || this.stopping) return;
        if (!(event.getWhoClicked() instanceof Player player) || !isParticipant(player)) return;

        event.setCancelled(true);
        Util.sendMsg(player, "<red>You can't rearrange your inventory in this minigame.");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!this.active || this.stopping) return;
        if (!(event.getWhoClicked() instanceof Player player) || !isParticipant(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (!this.active || this.stopping) return;
        if (!isParticipant(event.getPlayer())) return;

        event.setCancelled(true);
        Util.sendMsg(event.getPlayer(), "<red>You can't drop your gear in this minigame.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        if (charging.remove(player.getUniqueId()) == null) return;
        chargeReady.remove(player.getUniqueId());
        player.clearTitle();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!this.active || this.stopping) return;
        if (!isParticipant(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponUse(PlayerInteractEvent event) {
        if (!this.active || this.stopping) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Weapon weapon = weaponOf(event.getItem());
        IncursionTeam team = teamOf(player.getUniqueId());
        if (team == null) return;

        if (weapon == null) {
            if (grenadeOf(event.getItem()) != null && isOutOfPlay(player.getUniqueId())) {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 0.5f);
            }
            return;
        }

        switch (weapon) {
            case SNIPER -> {
                if (isOutOfPlay(player.getUniqueId()) || !readyToFire(player, weapon.getMaterial())) {
                    event.setCancelled(true);
                    return;
                }
                charging.put(player.getUniqueId(), player.getInventory().getHeldItemSlot());
            }
            case SHOTGUN -> {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
                if (isOutOfPlay(player.getUniqueId()) || !readyToFire(player, weapon.getMaterial())) return;
                fireBlunderhorn(player, team);
            }
            case SIDEARM -> {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
                if (isOutOfPlay(player.getUniqueId()) || !readyToFire(player, weapon.getMaterial())) return;
                fireSpitter(player, team);
            }
            case MELEE -> {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (!this.active || this.stopping) return;
        if (grenadeOf(event.getEgg().getItem()) == null) return;
        event.setHatching(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!this.active || this.stopping) return;
        if (!(event.getEntity() instanceof Egg egg)) return;

        Grenade grenade = grenadeOf(egg.getItem());
        if (grenade == null) return;
        Util.setPersistentKey(egg, GRENADE_KEY.getKey(), PersistentDataType.STRING, grenade.name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!this.active || this.stopping) return;
        if (!(event.getEntity() instanceof Egg egg)) return;

        String raw = Util.getPersistentKey(egg, GRENADE_KEY.getKey(), PersistentDataType.STRING);
        Grenade grenade = raw != null ? Util.getEnumFromString(Grenade.class, raw) : grenadeOf(egg.getItem());
        if (grenade == null) return;

        if (!(egg.getShooter() instanceof Player thrower) || teamOf(thrower.getUniqueId()) == null) return;

        Location at = event.getHitBlock() != null
                ? egg.getLocation()
                : (event.getHitEntity() != null ? event.getHitEntity().getLocation() : egg.getLocation());

        detonate(grenade, thrower, at.clone());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStopUsingItem(PlayerStopUsingItemEvent event) {
        if (!this.active || this.stopping) return;
        if (weaponOf(event.getItem()) != Weapon.SNIPER) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Integer startSlot = charging.remove(uuid);
        chargeReady.remove(uuid);

        IncursionTeam team = teamOf(uuid);
        if (startSlot == null || team == null) return;

        player.clearTitle();
        if (!stillAiming(player, startSlot)) return;
        if (event.getTicksHeldFor() < definition.getSniper().getChargeTicks()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            Util.sendMsg(player, "<red>The shot wasn't fully charged!");
            return;
        }
        if (isOutOfPlay(uuid)) return;

        fireOverwatch(player, team);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        IncursionTeam team = teamOf(player.getUniqueId());
        if (team == null || isTeamArmor(event.getNewItem())) return;

        Executors.delayedSync(player, 1, () -> {
            player.getInventory().setChestplate(teamChestplate(team));
            Util.sendMsg(player, "You can't take off your armor!");
        });
    }

    private static boolean isLethalFall(Player victim, EntityDamageEvent event) {
        float fallen = victim.getFallDistance();
        if (fallen <= 0f) return event.getDamage() + VANILLA_FALL_GRACE_BLOCKS >= LETHAL_FALL_BLOCKS;
        return fallen >= LETHAL_FALL_BLOCKS;
    }

    private boolean isOutOfPlay(UUID uuid) {
        return kickoffInProgress
                || frozenAt.containsKey(uuid)
                || respawning.contains(uuid)
                || invincibilityMillisLeft(uuid) > 0;
    }

    private long invincibilityMillisLeft(UUID uuid) {
        Long until = invincibleUntil.get(uuid);
        return until == null ? 0L : Math.max(0L, until - System.currentTimeMillis());
    }

    @Nullable
    private static Entity damagerOf(EntityDamageEvent event) {
        Entity causing = event.getDamageSource().getCausingEntity();
        if (causing != null) return causing;
        return event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
    }

    @Nullable
    private static Player resolveKiller(@Nullable Entity causingEntity, Player victim) {
        if (!(causingEntity instanceof Player killer) || killer.getUniqueId().equals(victim.getUniqueId())) return null;
        return killer;
    }

    @Nullable
    private IncursionTeam teamOf(UUID uuid) {
        return teamsByUuid.get(uuid);
    }

    private IncursionTeam enemyOf(IncursionTeam team) {
        return team == team1 ? team2 : team1;
    }

    @Nullable
    private EventPlayer participantOf(UUID uuid) {
        return participantsByUuid.get(uuid);
    }

    public record MapSide(IncursionDefinition.SpawnArea spawnArea, WorldTiedBoundingBox hole) {}

    private static final class Orb {

        private final Location marker;
        private final Location centre;
        private final BoundingBox pickupBox;
        private final AtomicBoolean available = new AtomicBoolean(true);
        private volatile long respawnAt;

        private Orb(Location marker) {
            this.marker = marker.clone();
            this.centre = marker.clone().add(0, ORB_FLOAT_HEIGHT, 0);
            this.pickupBox = BoundingBox.of(centre.toVector(),
                    ORB_PICKUP_RADIUS + PLAYER_HALF_WIDTH,
                    ORB_PICKUP_RADIUS + PLAYER_HALF_HEIGHT,
                    ORB_PICKUP_RADIUS + PLAYER_HALF_WIDTH);
        }

        private boolean collect(int respawnTicks) {
            if (!available.compareAndSet(true, false)) return false;
            respawnAt = System.currentTimeMillis() + (respawnTicks * 50L);
            return true;
        }

        private void refill() {
            respawnAt = 0;
            available.set(true);
        }

        private void render() {
            World world = marker.getWorld();
            world.spawnParticle(Particle.END_ROD, marker, 1, 0, 0, 0, 0);

            if (!available.get()) {
                if (System.currentTimeMillis() < respawnAt) return;
                available.set(true);
                world.playSound(centre, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
            }

            double spin = (System.currentTimeMillis() % 4000L) / 4000.0 * Math.PI * 2;
            double bounce = Math.sin(spin * 2) * 0.07;

            for (int i = 0; i < ORB_SHELL_POINTS; i++) {
                double height = 1.0 - (2.0 * ((i + 0.5) / ORB_SHELL_POINTS));
                double ring = Math.sqrt(Math.max(0.0, 1.0 - (height * height)));
                double angle = spin + (i * GOLDEN_ANGLE);

                world.spawnParticle(Particle.DUST,
                        centre.getX() + (Math.cos(angle) * ring * ORB_RADIUS),
                        centre.getY() + bounce + (height * ORB_RADIUS),
                        centre.getZ() + (Math.sin(angle) * ring * ORB_RADIUS),
                        1, 0, 0, 0, 0, ORB_SHELL_DUST);
            }

            world.spawnParticle(Particle.DUST, centre.getX(), centre.getY() + bounce, centre.getZ(),
                    ORB_CORE_POINTS, ORB_CORE_SPREAD, ORB_CORE_SPREAD, ORB_CORE_SPREAD, 0, ORB_CORE_DUST);
            world.spawnParticle(Particle.END_ROD, centre.getX(), centre.getY() + bounce, centre.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private final class Miniboss {
        private final WorldTiedBoundingBox room;
        private final WorldTiedBoundingBox reach;

        private volatile boolean alive;
        private volatile Mob entity;
        private volatile Entity mount;

        private volatile Location home;
        private volatile ScheduledTask task;
        private volatile int shownHealth = -1;
        private volatile long lastTickAt;
        private volatile long respawnAt;
        private volatile long nextScanAt;
        private volatile long burnUntil;
        private volatile boolean fireShown;

        private Miniboss(WorldTiedBoundingBox room) {
            this.room = room;
            this.reach = room.move(MINIBOSS_ROOM_TARGET_MARGIN, MINIBOSS_ROOM_TARGET_MARGIN, MINIBOSS_ROOM_TARGET_MARGIN);
        }

        // Stalled = mob is gone without anyone having slain it
        private boolean hasStalled(long now) {
            return alive && now - lastTickAt > MINIBOSS_STALL_MILLIS;
        }

        @Nullable
        private Target targetAt(long at) {
            Mob mob = this.entity;
            return mob == null || !alive ? null : Incursion.this.targetAt(mob, at);
        }

        private void burnFor(int ticks) {
            this.burnUntil = System.currentTimeMillis() + (ticks * 50L);
        }

        private boolean isBurning() {
            return System.currentTimeMillis() < burnUntil;
        }

        private synchronized boolean dueForRespawn(long now) {
            if (alive || respawnAt == 0 || now < respawnAt) return false;
            this.respawnAt = 0;
            return true;
        }

        @Nullable
        private synchronized Claimed claim() {
            Mob mob = this.entity;
            Entity mount = this.mount;
            this.alive = false;
            this.entity = null;
            this.mount = null;
            this.respawnAt = 0;
            this.burnUntil = 0;
            this.fireShown = false;
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            if (mob != null) minibossesByEntity.remove(mob.getUniqueId());
            return mob == null ? null : new Claimed(mob, mount);
        }

        private void spawn() {
            attemptSpawn(MINIBOSS_SPAWN_ATTEMPTS);
        }

        private void respawn() {
            despawn();
            spawn();
        }

        private void attemptSpawn(int attemptsLeft) {
            if (!Incursion.this.active || Incursion.this.stopping || entity != null) return;

            World world = room.getWorld();
            int minX = (int) room.getMinX();
            int minZ = (int) room.getMinZ();
            int x = minX + RANDOM.nextInt(Math.max(1, (int) room.getMaxX() - minX));
            int z = minZ + RANDOM.nextInt(Math.max(1, (int) room.getMaxZ() - minZ));

            Executors.runSync(world, x >> 4, z >> 4, () -> {
                boolean lastFewTries = attemptsLeft <= 4;
                Location spot = standingSpotIn(world, x, z, !lastFewTries);
                if (spot != null) spawnAt(spot);
                else if (!lastFewTries) attemptSpawn(attemptsLeft - 1);
                else spawnAt(room.getCenterLocation());
            });
        }

        // The first spot in this column the mob fits in, or null if there is none
        @Nullable
        private Location standingSpotIn(World world, int x, int z, boolean requireGround) {
            int lowest = (int) room.getMinY();
            int highest = (int) room.getMaxY() - MINIBOSS_HEIGHT_BLOCKS;

            for (int y = highest; y >= lowest; y--) {
                if (requireGround && world.getBlockAt(x, y - 1, z).isPassable()) continue;

                boolean fits = true;
                for (int offset = 0; offset < MINIBOSS_HEIGHT_BLOCKS; offset++) {
                    if (world.getBlockAt(x, y + offset, z).isPassable()) continue;
                    fits = false;
                    break;
                }
                if (fits) return new Location(world, x + 0.5, y, z + 0.5);
            }
            return null;
        }

        private void spawnAt(Location at) {
            Executors.runSync(at, () -> {
                if (!Incursion.this.active || Incursion.this.stopping || entity != null) return;

                Mob mob = createMiniboss(at, definition.getMiniboss());
                unsafe(() -> stripWanderGoals(mob));

                synchronized (this) {
                    if (Incursion.this.stopping || this.entity != null) {
                        removeWithMount(mob, null);
                        return;
                    }

                    this.home = at.clone();
                    this.entity = mob;
                    this.mount = adoptMount(mob);
                    this.shownHealth = -1;
                    this.lastTickAt = System.currentTimeMillis();
                    this.alive = true;
                    minibossesByEntity.put(mob.getUniqueId(), this);
                    this.task = Executors.repeatingSync(mob, MINIBOSS_TICK_PERIOD_TICKS, this::tick);
                }

                at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                at.getWorld().playSound(at, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.7f);
            });
        }

        // Runs on the mob's thread
        private void tick(ScheduledTask task) {
            Mob mob = this.entity;
            if (mob == null || !mob.isValid()) {
                task.cancel();
                return;
            }

            BoundingBox box = mob.getBoundingBox();
            long now = System.currentTimeMillis();
            this.lastTickAt = now;

            // Only burn when hit by an incendiary egg, not when exposed to sunlight
            boolean burning = now < burnUntil;
            if (burning != fireShown) {
                this.fireShown = burning;
                mob.setVisualFire(burning ? TriState.NOT_SET : TriState.FALSE);
            }
            if (!burning && mob.getFireTicks() > 0) mob.setFireTicks(0);

            int health = (int) Math.ceil(mob.getHealth());
            if (health != shownHealth) {
                this.shownHealth = health;
                mob.customName(minibossName(health));
            }

            if (!room.contains(box.getCenterX(), box.getMinY(), box.getCenterZ())) {
                sendBackInside(mob);
                return;
            }

            keepPathInsideRoom(mob);
            retarget(mob, now);
        }

        private void retarget(Mob mob, long now) {
            LivingEntity chasing = mob.getTarget();
            if (chasing != null) {
                if (!Bukkit.isOwnedByCurrentRegion(chasing)) return;
                if (chasing.isValid() && isWithinReach(mob, chasing)) return;
            } else if (now < nextScanAt) return;
            this.nextScanAt = now + MINIBOSS_TARGET_SCAN_MILLIS;

            Player closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (EventPlayer participant : participants) {
                if (isOutOfPlay(participant.getUuid())) continue;

                Player player = participant.getPlayer();
                if (player == null || !Bukkit.isOwnedByCurrentRegion(player)) continue;

                double distance = squaredDistanceTo(mob, player);
                if (distance >= closestDistance || !isWithinReach(mob, player, distance)) continue;

                closestDistance = distance;
                closest = player;
            }

            if (closest != null) mob.setTarget(closest);
            else if (chasing != null) mob.setTarget(null);
        }

        private boolean isWithinReach(Mob mob, Entity candidate) {
            return isWithinReach(mob, candidate, squaredDistanceTo(mob, candidate));
        }

        private boolean isWithinReach(Mob mob, Entity candidate, double squaredDistance) {
            if (candidate.getWorld() != room.getWorld()) return false;
            return squaredDistance <= MINIBOSS_TARGET_RANGE_SQUARED
                    || reach.contains(candidate.getX(), candidate.getY(), candidate.getZ());
        }

        private static double squaredDistanceTo(Mob mob, Entity other) {
            double dx = other.getX() - mob.getX();
            double dy = other.getY() - mob.getY();
            double dz = other.getZ() - mob.getZ();
            return (dx * dx) + (dy * dy) + (dz * dz);
        }

        private void keepPathInsideRoom(Mob mob) {
            Pathfinder pathfinder = mob.getPathfinder();
            if (!pathfinder.hasPath()) return;

            Pathfinder.PathResult path = pathfinder.getCurrentPath();
            if (path == null) return;

            if (leavesRoom(path.getNextPoint()) || leavesRoom(path.getFinalPoint())) {
                pathfinder.stopPathfinding();
            }
        }

        private boolean leavesRoom(@Nullable Location point) {
            return point != null && !room.contains(point.getX(), point.getY(), point.getZ());
        }

        private void sendBackInside(Mob mob) {
            Location back = nearestSpotInside(mob);
            if (back == null) back = this.home; // the shortest way back is inside a wall
            if (back == null) return;

            Entity moving = mob;
            while (moving.getVehicle() != null) moving = moving.getVehicle();

            moving.setVelocity(new Vector(0, 0, 0));
            moving.setFallDistance(0f);
            forgetTrail(mob.getUniqueId());
            Executors.teleportSafely(moving, back);
        }

        // The closest spot just inside the room, or null if the mob wouldn't fit there
        @Nullable
        private Location nearestSpotInside(Mob mob) {
            World world = room.getWorld();
            Location spot = new Location(world,
                    Math.clamp(mob.getX(), room.getMinX() + MINIBOSS_ROOM_INSET, room.getMaxX() - MINIBOSS_ROOM_INSET),
                    Math.clamp(mob.getY(), room.getMinY(), room.getMaxY() - MINIBOSS_HEIGHT_BLOCKS),
                    Math.clamp(mob.getZ(), room.getMinZ() + MINIBOSS_ROOM_INSET, room.getMaxZ() - MINIBOSS_ROOM_INSET),
                    mob.getYaw(), 0f);
            if (!Bukkit.isOwnedByCurrentRegion(spot)) return null;

            for (int offset = 0; offset < MINIBOSS_HEIGHT_BLOCKS; offset++) {
                if (!world.getBlockAt(spot.getBlockX(), spot.getBlockY() + offset, spot.getBlockZ()).isPassable()) {
                    return null;
                }
            }
            return spot;
        }

        // Called on the mob's thread
        private void slay(@Nullable Player killer) {
            Claimed claimed = claim();
            if (claimed == null) return; // half-time or end of game

            int respawnTicks = definition.getMiniboss().getRespawnTicks();
            if (respawnTicks > 0) this.respawnAt = System.currentTimeMillis() + (respawnTicks * 50L);

            Location at = claimed.mob().getLocation();
            removeWithMount(claimed.mob(), claimed.mount());

            World world = at.getWorld();
            world.spawnParticle(Particle.EXPLOSION_EMITTER, at.clone().add(0, 1, 0), 1);
            world.spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
            world.playSound(at, Sound.ENTITY_WITHER_DEATH, 0.35f, 1.4f);

            dropGrenades(world, at);
            rewardSlayer(killer);
        }

        private void dropGrenades(World world, Location at) {
            int amount = minibossDropsMin + (minibossDropsMax > minibossDropsMin
                    ? RANDOM.nextInt(minibossDropsMax - minibossDropsMin + 1) : 0);

            for (int i = 0; i < amount; i++) {
                world.dropItemNaturally(at, Util.getRandom(GRENADES).item());
            }
        }

        private void rewardSlayer(@Nullable Player killer) {
            if (killer == null) return;

            IncursionTeam team = teamOf(killer.getUniqueId());
            EventPlayer participant = participantOf(killer.getUniqueId());
            if (team == null || participant == null) return;

            int awarded = definition.getMiniboss().getPoints();
            addPoints(participant, team, awarded);

            String name = participant.getName() != null ? participant.getName() : "Someone";
            sendAudienceMessage(Component.text(name, team.getColor())
                    .append(Component.text(" defeated a vanguard! ", NamedTextColor.WHITE))
                    .append(Component.text("(+" + awarded + ")", NamedTextColor.GRAY)));
            playAudienceSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.6f);
        }

        private void despawn() {
            Claimed claimed = claim();
            if (claimed == null) return;
            Executors.runSync(claimed.mob(), () -> removeWithMount(claimed.mob(), claimed.mount()));
        }

        // Vanguards occasionally spawn as jockeys
        @Nullable
        private static Entity adoptMount(Mob mob) {
            Entity mount = mob.getVehicle();
            if (mount == null) return null;

            mount.setPersistent(false);
            if (mount instanceof Mob ridden) ridden.setRemoveWhenFarAway(false);
            return mount;
        }

        private static void removeWithMount(Mob mob, @Nullable Entity spawnedOn) {
            Entity riding = mob.getVehicle();
            if (!mob.isDead()) mob.remove();

            if (riding != null && !riding.isDead()) riding.remove();
            if (spawnedOn != null && spawnedOn != riding && !spawnedOn.isDead()) spawnedOn.remove();
        }

        private record Claimed(Mob mob, @Nullable Entity mount) {}
    }

    private Component minibossName(int health) {
        return Component.text("Vanguard ", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(health, NamedTextColor.RED))
                .append(Component.text("HP", NamedTextColor.GRAY))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
    }

    private Mob createMiniboss(Location at, IncursionDefinition.MinibossSettings settings) {
        World world = at.getWorld();

        return switch (settings.getType()) {
            case DROWNED -> {
                Drowned drowned = world.spawn(at, Drowned.class);
                prepareMiniboss(drowned, settings);
                drowned.setShouldBurnInDay(false); // Doesn't seem to work on its own...
                drowned.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT), true);
                yield drowned;
            }
            case PARCHED -> {
                Parched parched = world.spawn(at, Parched.class);
                prepareMiniboss(parched, settings);
                parched.setShouldBurnInDay(false);
                parched.getEquipment().setItemInMainHand(new ItemStack(Material.BOW), true);
                yield parched;
            }
            case WITCH -> {
                Witch witch = world.spawn(at, Witch.class);
                prepareMiniboss(witch, settings);
                yield witch;
            }
        };
    }

    private static void stripWanderGoals(Mob mob) {
        if (mob instanceof Creature creature) {
            for (GoalKey<Creature> goal : MINIBOSS_DISABLED_GOALS) {
                Bukkit.getMobGoals().removeGoal(creature, goal);
            }
        }
        if (mob instanceof Drowned drowned) {
            Bukkit.getMobGoals().removeGoal(drowned, VanillaGoal.DROWNED_GO_TO_BEACH);
        }
        if (mob instanceof Raider raider) {
            Bukkit.getMobGoals().removeGoal(raider, VanillaGoal.RAIDER_MOVE_THROUGH_VILLAGE);
        }
    }

    private void prepareMiniboss(Mob mob, IncursionDefinition.MinibossSettings settings) {
        double health = Math.max(1.0, settings.getHealth());

        Util.setPersistentKey(mob, MINIBOSS_KEY.getKey(), PersistentDataType.BYTE, (byte) 1);
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);
        mob.setCustomNameVisible(true);
        mob.customName(minibossName((int) Math.ceil(health)));
        mob.setVisualFire(TriState.FALSE);

        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.getModifiers().stream().toList().forEach(maxHealth::removeModifier);
            maxHealth.setBaseValue(health);
        }
        mob.setHealth(health);

        EntityEquipment equipment = mob.getEquipment();
        equipment.clear();

        equipment.setItemInMainHandDropChance(0f);
        equipment.setItemInOffHandDropChance(0f);
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
    }

    // Who last hit someone, with what, for how much (and when)
    private record DamageCredit(UUID attacker, long at, String sourceName, double damage, boolean headshot) {}

    @Getter
    private enum Grenade {

        NORMAL(Material.EGG, "<white><b>Unstable Egg",
                List.of("<gray>Throw it. It goes off where it lands."), Color.WHITE),
        CRYO(Material.BLUE_EGG, "<aqua><b>Cryo Egg",
                List.of("<gray>Throw it. It goes off where it lands.", "<gray>Frosts & slows whoever it catches."), Color.AQUA),
        INCENDIARY(Material.BROWN_EGG, "<gold><b>Incendiary Egg",
                List.of("<gray>Throw it. It goes off where it lands.", "<gray>Sets whoever it catches alight."), Color.ORANGE);

        private final Material material;
        private final String displayName;
        private final List<String> lore;
        private final Particle.DustOptions burstDust;
        private final String plainName;

        Grenade(Material material, String displayName, List<String> lore, Color burstColor) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.burstDust = new Particle.DustOptions(burstColor, 1.4f);
            this.plainName = stripTags(displayName);
        }

        private ItemStack item() {
            ItemStack item = new ItemStack(material);
            item.editMeta(meta -> {
                meta.displayName(Util.color(displayName));
                meta.lore(Util.color(lore, NamedTextColor.GRAY));
                meta.getPersistentDataContainer().set(GRENADE_KEY, PersistentDataType.STRING, name());
            });
            return item;
        }
    }

    @Getter
    private enum Weapon {

        MELEE(0, Material.STONE_SWORD, "<white><b>Last Resort",
                List.of("<gray>Use when everything's on cooldown"), Map.of(Enchantment.SHARPNESS, 2)),
        SNIPER(1, Material.SPYGLASS, "<light_purple><b>Overwatch",
                List.of("<gray>Hold to charge, release to fire", "<gray>Pierces through enemies")),
        SHOTGUN(2, Material.GOAT_HORN, "<gold><b>Blunderhorn",
                List.of("<gray>Right click for a blast", "<gray>Hurts less at range")),
        SIDEARM(3, Material.PUFFERFISH, "<aqua><b>Spitter",
                List.of("<gray>Right click to spit water", "<gray>Weak but quick"));

        private final int slot;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        private final Map<Enchantment, Integer> enchantments;
        private final String plainName;

        Weapon(int slot, Material material, String displayName, List<String> lore) {
            this(slot, material, displayName, lore, Map.of());
        }

        Weapon(int slot, Material material, String displayName, List<String> lore, Map<Enchantment, Integer> enchantments) {
            this.slot = slot;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.enchantments = enchantments;
            this.plainName = stripTags(displayName);
        }

        private ItemStack item() {
            ItemStack item = new ItemStack(material);
            item.editMeta(meta -> {
                meta.displayName(Util.color(displayName));
                meta.lore(Util.color(lore, NamedTextColor.GRAY));
                enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                meta.getPersistentDataContainer().set(WEAPON_KEY, PersistentDataType.STRING, name());
            });
            item.unsetData(DataComponentTypes.INSTRUMENT);
            item.unsetData(DataComponentTypes.CONSUMABLE);
            item.unsetData(DataComponentTypes.FOOD);
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.INSTRUMENT, DataComponentTypes.CONSUMABLE,
                            DataComponentTypes.FOOD, DataComponentTypes.ATTRIBUTE_MODIFIERS)
                    .build());
            return item;
        }
    }

    @Getter
    public static final class IncursionTeam implements Scorer {

        private final String teamName;
        private final NamedTextColor color;
        private final Color armorColor;
        private final Particle.DustOptions dustOptions;
        private final Particle.DustOptions beamDust;
        private final Particle.DustOptions pelletDust;
        private final AtomicInteger score = new AtomicInteger();

        @Setter
        private volatile MapSide side;

        private IncursionTeam(IncursionDefinition.TeamDefinition definition, MapSide side) {
            this.teamName = definition.getName();
            this.color = definition.getNamedTextColor();
            this.armorColor = definition.getArmorColor();
            this.dustOptions = new Particle.DustOptions(this.armorColor, 1.0f);
            this.beamDust = new Particle.DustOptions(this.armorColor, 0.6f);
            this.pelletDust = new Particle.DustOptions(
                    blend(BLUNDERHORN_GUNMETAL, this.armorColor, BLUNDERHORN_TEAM_TINT), 0.6f);
            this.side = side;
        }

        @Override
        public String getName() {
            return teamName;
        }
    }
}
