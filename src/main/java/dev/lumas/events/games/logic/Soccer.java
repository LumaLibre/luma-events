package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.google.common.base.Preconditions;
import dev.lumas.core.util.ContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.SulfurSoccerDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.SoccerTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.lumaitems.particles.ParticleDisplay;
import dev.lumas.lumaitems.particles.Particles;
import io.papermc.paper.event.entity.EntityMoveEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class Soccer extends InventoryUnifiedMinigame {

    private static final String[] SPLASH = {
        "Also known as football!",
        "Run, kick, run",
        "40.0 b/s!",
        "No hands! (Sometimes)",
        "GOAAAL!",
        "Summer: Edition!",
        "Zoom zoom"
    };

    private static final String[] EXTRA_OWN_GOAL_NUANCE = {
        "what are they doing?",
        "seriously?",
        "nice job!",
        "+%2$d for the other team!",
        "good work, %1$s",
        "that was probably an accident, right?",
        "oops!",
        ">:("
    };

    private static final ContextLogger LOGGER = ContextLogger.getLogger(NamedTextColor.YELLOW, false);

    private static final AttributeModifier FREEZE_MOD = new AttributeModifier(
        new NamespacedKey(EventMain.getInstance(), "sulfur_soccer_freeze"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private static final List<Attribute> FREEZE_ATTRIBUTES = List.of(Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH);

    private static final int LEAD_TO_WIN = 30;
    private static final int POINTS_PER_GOAL = 10;
    private static final int OUT_OF_BOUNDS_PENALTY = -2;

    private static final long ROUND_LENGTH_MILLIS = 90_000L;

    private static final long FEAT_DISPLAY_MILLIS = 3000L;
    private static final long FEAT_REPEAT_COOLDOWN_MILLIS = 750L;
    // one ball up to 18 players, then another for every 8 on top of that
    private static final int BALL_BASE_PLAYERS = 10;
    private static final int PLAYERS_PER_EXTRA_BALL = 8;
    private static final int MAX_BALLS = 6;
    private static final double BALL_SPREAD_MIN = 3.0;
    private static final double BALL_SPREAD_MAX = 12.0;
    private static final double BALL_SPREAD_PITCH_FRACTION = 0.18;

    private static final double POWER_KICK_BPS = 40.0;
    private static final double POWER_KICK_BLOCKS_PER_TICK = POWER_KICK_BPS / 20.0;
    private static final long POWER_KICK_WINDOW_MILLIS = 600L;
    private static final double SAVE_RADIUS = 2.0;
    private static final double SAVE_THREAT_DISTANCE = 12.0;

    private static final Particle.DustOptions OWN_GOAL_DUST = new Particle.DustOptions(Color.fromRGB(0xE23B3B), 2.0f);
    private static final Particle.DustOptions ENEMY_GOAL_DUST = new Particle.DustOptions(Color.fromRGB(0x3BE24E), 2.0f);
    private static final int GOAL_MARKER_PARTICLES = 12;
    private static final int GOAL_MARKER_INTERVAL_TICKS = 10;

    private final Location spawnLocation;
    private final List<SoccerTeam> teams;
    private final Location ballSpawnLocation;
    private volatile List<SulfurCubeSoccerBall> balls = List.of(); // built once the roster is known
    private final MinigameRoleMap<SoccerPlayer> roleMap = new MinigameRoleMap<>();
    private final Scoreboard<SoccerTeam> scoreboard = new Scoreboard<>();
    private final Scoreboard<SoccerPlayer> playerScoreboard = new Scoreboard<>();
    private final SoccerTokenFormula tokenFormula = new SoccerTokenFormula();
    private final int goalPunchRadius;
    private final double ballSpreadRadius;

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0.0f, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);

    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    // with several balls out there, only the first one to score or go out gets to end the round
    private final AtomicBoolean roundEnding = new AtomicBoolean();
    private volatile long roundEndsAt; // wall clock the live round expires at, 0 while no round is running


    public Soccer(SulfurSoccerDefinition def) {
        super(
            "Soccer",
            Util.getRandom(SPLASH),
            1200000L,
            1,
            true,
            true,
            true, // nobody needs their inventory here, and letting them touch it duplicates the team kit
            true
        );

        this.spawnLocation = def.getSpawnLocation();
        WorldTiedBoundingBox pitch = def.getBounds().toWorldTiedBoundingBox();
        this.boundingBox = pitch;
        this.ballSpreadRadius = Math.clamp(
            Math.min(pitch.getWidthX(), pitch.getWidthZ()) * BALL_SPREAD_PITCH_FRACTION,
            BALL_SPREAD_MIN,
            BALL_SPREAD_MAX);

        TeamMatchup matchup = Util.getRandom(TeamMatchup.values());
        this.teams = List.of(
            new SoccerTeam(def.getTeam1StartLocation(), def.getTeam1Goal().toWorldTiedBoundingBox(), matchup.first()),
            new SoccerTeam(def.getTeam2StartLocation(), def.getTeam2Goal().toWorldTiedBoundingBox(), matchup.second())
        );

        this.ballSpawnLocation = def.getSoccerBallStartLocation();
        this.goalPunchRadius = def.getPunchRadius();
    }

    @Override
    protected int minimumParticipants() {
        return 1; // TODO: Change to 2
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        SoccerPlayer role = this.roleMap.as(participant.getUuid(), SoccerPlayer.class);
        if (role == null) {
            return;
        }

        int plays;
        synchronized (this.playerScoreboard) {
            plays = this.playerScoreboard.getScore(role);
        }

        this.tokenFormula.giveTokens(participant, Couple.of(plays, role.team().equals(this.leadingTeam())));
        participant.addPermanentScore(MinigameConstant.SOCCER, plays);
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.teleportOnJoin(player, this.spawnLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        this.frozenPlayers.remove(participant.getUuid());
        participant.operatePlayer(Soccer::unfreeze);
        participant.removeBossBar(this.bossBar);
        this.roleMap.remove(participant.getUuid()); // no role, no tokens: leaving forfeits them
        return super.removeParticipant(participant, doTeleport);
    }

    @Override
    protected void handleStart() {
        if (this.teams.size() != 2) {
            this.stop();
            throw new IllegalStateException("Illegal number of teams, expected 2 but got: " + this.teams.size() + " - " + this.teams);
        }

        SoccerTeam team1 = this.teams.getFirst();
        SoccerTeam team2 = this.teams.getLast();


        List<EventPlayer> shuffled = new ArrayList<>(this.getParticipants());
        Collections.shuffle(shuffled);
        for (EventPlayer eventPlayer : shuffled) {
            eventPlayer.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
            SoccerTeam team = this.roleMap.size() % 2 == 0 ? team1 : team2;
            SoccerPlayer soccerPlayer = new SoccerPlayer(eventPlayer, team);
            this.roleMap.put(soccerPlayer);
        }

        this.balls = this.buildBalls();
        if (this.balls.size() > 1) {
            sendAudienceMessage("<yellow>A big turnout - <white>" + this.balls.size() + "</white> balls are in play!");
        }

        this.refreshBossBar();
        for (SoccerPlayer soccerPlayer : this.roleMap) {
            soccerPlayer.getEventPlayer().addBossBar(this.bossBar);
        }

        this.countdownStart(5);
    }

    private int ballCount() {
        int past = this.getParticipants().size() - BALL_BASE_PLAYERS;
        return Math.min(MAX_BALLS, 1 + Math.max(0, (past - 1) / PLAYERS_PER_EXTRA_BALL));
    }

    private List<SulfurCubeSoccerBall> buildBalls() {
        int count = this.ballCount();
        List<SulfurCubeSoccerBall> built = new ArrayList<>(count);
        built.add(new SulfurCubeSoccerBall(this.ballSpawnLocation));

        for (int i = 1; i < count; i++) {
            double angle = (2 * Math.PI * (i - 1)) / (count - 1);
            Location spread = this.ballSpawnLocation.clone()
                .add(Math.cos(angle) * this.ballSpreadRadius, 0, Math.sin(angle) * this.ballSpreadRadius);
            built.add(new SulfurCubeSoccerBall(spread));
        }
        return List.copyOf(built);
    }

    private void killAllBalls() {
        for (SulfurCubeSoccerBall ball : this.balls) {
            ball.kill();
        }
    }

    private void finishRound(@Nullable SoccerTeam beneficiary, Location at) {
        this.roundEndsAt = 0L;
        if (beneficiary != null) {
            this.scoreboard.addScore(beneficiary, POINTS_PER_GOAL);
        }
        this.refreshBossBar();
        this.killAllBalls();

        Executors.runSync(at, () -> {
            if (beneficiary != null) {
                ParticleDisplay particleDisplay = ParticleDisplay.of(Particle.DUST)
                    .withColor(new java.awt.Color(beneficiary.template().color()))
                    .withLocation(at);
                Particles.spikeSphere(6.0, 25.0, 3, 0.4, 0.8, particleDisplay);
            }

            for (SoccerTeam team : this.teams) {
                Location goalLocation = team.goalBox().getCenterLocation();
                goalLocation.getWorld().playSound(goalLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 4f, 2f);
                goalLocation.getWorld().playSound(goalLocation, Sound.ITEM_GOAT_HORN_SOUND_2, 4f, 1.25f);
            }

            this.ballSpawnLocation.getWorld().playSound(this.ballSpawnLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 4f, 2f);
            this.ballSpawnLocation.getWorld().playSound(this.ballSpawnLocation, Sound.ITEM_GOAT_HORN_SOUND_2, 4f, 1.25f);

            // blast every entity within a 25 block radius away from the goal
            for (LivingEntity entity : at.getNearbyLivingEntities(35)) {
                Vector direction = entity.getLocation().toVector().subtract(at.toVector());
                if (direction.lengthSquared() < 1.0E-6) {
                    direction = new Vector(0, 1, 0); // entity sitting exactly on the goal, just launch it up
                } else {
                    direction.normalize();
                }

                // stronger blast the closer the entity is to the goal
                double distance = entity.getLocation().distance(at);
                double strength = Math.max(0.5, 2.8 * (1 - distance / 35.0));

                Vector knockback = direction.multiply(strength);
                knockback.setY(knockback.getY() + 0.7); // add a little lift
                entity.setVelocity(knockback);
            }

            // wait 3.5 seconds before starting a new round
            Executors.delayedSync(at, 70, () -> {
                if (beneficiary != null && leadOf(beneficiary) >= LEAD_TO_WIN) {
                    sendAudienceMessage("<red>Game over!");
                    sendAudienceMessage(beneficiary.template().formatted(beneficiary.getName() + " has won!"));
                    stop();
                } else {
                    countdownStart(3);
                }
            });
        });
    }

    private void endRoundOnPlays() {
        if (!this.roundEnding.compareAndSet(false, true)) {
            return;
        }

        SoccerTeam first = this.teams.getFirst();
        SoccerTeam second = this.teams.getLast();
        int firstPlays = first.roundPlays().get();
        int secondPlays = second.roundPlays().get();

        sendAudienceMessage("<yellow>Time! <gray>No goal this round, so it goes on plays made.");
        if (firstPlays == secondPlays) {
            sendAudienceMessage("<gray>Dead even at <white>" + firstPlays + "</white> each - nobody takes it.");
            this.finishRound(null, this.ballSpawnLocation.clone());
            return;
        }

        SoccerTeam winner = firstPlays > secondPlays ? first : second;
        int winnerPlays = Math.max(firstPlays, secondPlays);
        int loserPlays = Math.min(firstPlays, secondPlays);
        sendAudienceMessage(winner.template().formatted(winner.getName() + " takes the round <dark_gray>("
            + winnerPlays + " plays to " + loserPlays + ", +" + POINTS_PER_GOAL + " points)"));
        this.finishRound(winner, this.ballSpawnLocation.clone());
    }

    // the readout only has room for one ball, so it follows whichever is closest to the player
    private double nearestBallSpeed(Location from) {
        double nearest = Double.MAX_VALUE;
        double speed = 0.0;
        for (SulfurCubeSoccerBall ball : this.balls) {
            Location at = ball.speed().lastSample();
            if (at == null || !at.getWorld().equals(from.getWorld())) {
                continue;
            }
            double distance = at.distanceSquared(from);
            if (distance < nearest) {
                nearest = distance;
                speed = ball.speed().blocksPerSecond();
            }
        }
        return speed;
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping || !this.active) {
            return;
        }

        for (SulfurCubeSoccerBall ball : this.balls) {
            ball.tick();
        }
        for (SoccerPlayer player : this.roleMap) {
            player.onTick();
        }

        long endsAt = this.roundEndsAt;
        if (endsAt > 0 && System.currentTimeMillis() >= endsAt) {
            this.endRoundOnPlays();
        }

        if (timeLeft <= 0) {
            sendAudienceMessage("Times up, game over!");
        }
    }

    @Override
    protected void handleStop() {
        this.killAllBalls();
        this.unfreezeAll();
        for (SoccerPlayer soccerPlayer : this.roleMap) {
            soccerPlayer.operatePlayer(player -> {
                player.getEquipment().setChestplate(null);
            });
            soccerPlayer.getEventPlayer().removeBossBar(this.bossBar);
        }
        this.scoreboard.handleGameEnd(this.audience, () -> {
            Executors.teleportGroupAsync(this.participants, this.spawnLocation);
            CountdownBossBar.builder()
                .audience(this.audience)
                .color(BossBar.Color.BLUE)
                .title("<aqua><b>Game Over")
                .seconds(15)
                .callback(() -> {
                    this.participants.forEach(participant -> {
                        participant.teleportAsync(this.getGameDropOffLocation());
                        participant.sendMessage("This minigame has concluded.");
                    });
                })
                .build()
                .start();
        });
    }

    private SoccerTeam opponentOf(SoccerTeam team) {
        for (SoccerTeam other : this.teams) {
            if (other != team) {
                return other;
            }
        }
        return team;
    }

    private int leadOf(SoccerTeam team) {
        return this.scoreboard.getScore(team) - this.scoreboard.getScore(this.opponentOf(team));
    }

    private int winPercent(SoccerTeam team) {
        return Math.clamp(this.leadOf(team) * 100L / LEAD_TO_WIN, 0, 100);
    }

    private @Nullable SoccerTeam leadingTeam() {
        for (SoccerTeam team : this.teams) {
            if (this.leadOf(team) > 0) {
                return team;
            }
        }
        return null;
    }

    private String scoreLine() {
        StringBuilder line = new StringBuilder();
        for (SoccerTeam team : this.teams) {
            if (!line.isEmpty()) line.append("<dark_gray> | ");
            line.append(team.template().formatted("<b>" + team.template().bossBarName() + "</b>"))
                .append("<gray>: <white>")
                .append(this.winPercent(team))
                .append('%');
        }
        return line.toString();
    }

    private boolean threatensGoal(SoccerTeam team, Entity ball) {
        WorldTiedBoundingBox goal = team.goalBox();
        if (!goal.getWorld().equals(ball.getWorld())) {
            return false;
        }

        Location at = ball.getLocation();
        if (team.distanceSquaredTo(at) <= SAVE_RADIUS * SAVE_RADIUS) {
            return true;
        }

        Vector velocity = ball.getVelocity();
        if (velocity.lengthSquared() < 1.0E-6) {
            return false;
        }
        return goal.rayTrace(at.toVector(), velocity.normalize(), SAVE_THREAT_DISTANCE) != null;
    }

    private void showGoalMarkers(Player player, SoccerTeam team) {
        spawnGoalDust(player, team.goalBox(), OWN_GOAL_DUST);
        spawnGoalDust(player, this.opponentOf(team).goalBox(), ENEMY_GOAL_DUST);
    }

    private static void spawnGoalDust(Player player, WorldTiedBoundingBox goalBox, Particle.DustOptions dust) {
        if (!goalBox.getWorld().equals(player.getWorld())) {
            return;
        }
        for (int i = 0; i < GOAL_MARKER_PARTICLES; i++) {
            double x = goalBox.getMinX() + RANDOM.nextDouble() * goalBox.getWidthX();
            double y = goalBox.getMinY() + RANDOM.nextDouble() * goalBox.getHeight();
            double z = goalBox.getMinZ() + RANDOM.nextDouble() * goalBox.getWidthZ();
            player.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, dust, true);
        }
    }

    private void refreshBossBar() {
        SoccerTeam leader = this.leadingTeam();
        this.bossBar.name(Util.color(this.scoreLine()));
        this.bossBar.progress(leader == null ? 0.0f : this.winPercent(leader) / 100.0f);
        this.bossBar.color(leader == null ? BossBar.Color.WHITE : leader.template().barColor());
    }

    private @Nullable SulfurCubeSoccerBall ballOf(Entity entity) {
        for (SulfurCubeSoccerBall ball : this.balls) {
            if (entity.equals(ball.sulfurCube())) {
                return ball;
            }
        }
        return null;
    }

    @EventHandler
    public void onSulfurCubePunch(EntityKnockbackByEntityEvent event) {
        SulfurCubeSoccerBall ball = this.ballOf(event.getEntity());
        if (ball == null || !ball.isSpawned()) {
            return;
        }

        if (!(event.getPushedBy() instanceof Player puncher)) {
            return;
        }

        SoccerPlayer attacker = this.roleMap.as(puncher.getUniqueId(), SoccerPlayer.class);
        if (attacker == null) return;


        if (!attacker.team().withinPunchRadius(puncher.getLocation())) {
            event.setCancelled(true);
            attacker.quietActionBar(1500L); // else the speed readout wipes this on the next tick
            puncher.sendActionBar(Util.color("<red>You can only punch the ball near your own goal!"));
            return;
        }

        // no sound of our own here, the cube's knockback already plays its archetype hit sound
        ball.lastContact(attacker);

        // the knockback has not been applied yet, so this is the ball as it was coming in
        if (this.threatensGoal(attacker.team(), event.getEntity())) {
            attacker.award(Feat.SAVE);
        }
        ball.markKick(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPunchPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!isParticipant(attacker) || !isParticipant(victim)) {
            return;
        }
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSulfurCubeMove(EntityMoveEvent event) {
        SulfurCubeSoccerBall ball = this.ballOf(event.getEntity());
        if (ball != null) {
            ball.handleMove(event);
        }
    }

    private void freeze(Player player) {
        this.frozenPlayers.add(player.getUniqueId());
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            instance.removeModifier(FREEZE_MOD);
            instance.addTransientModifier(FREEZE_MOD);
        }
    }

    private static void unfreeze(Player player) {
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) instance.removeModifier(FREEZE_MOD);
        }
    }

    private void unfreezeAll() {
        this.frozenPlayers.clear();
        for (SoccerPlayer soccerPlayer : this.roleMap) {
            soccerPlayer.operatePlayer(Soccer::unfreeze);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFrozenPlayerMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }
        if (!this.frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return;
        }

        Location held = from.clone();
        held.setY(to.getY());
        held.setYaw(to.getYaw());
        held.setPitch(to.getPitch());
        event.setTo(held);
    }


    private void countdownStart(final int countdown) {
        for (SoccerPlayer soccerPlayer : this.roleMap) {
            soccerPlayer.teleportToStart();
            soccerPlayer.operatePlayer(this::freeze);
        }

        AtomicInteger atomicCountdown = new AtomicInteger(countdown);
        this.roundEnding.set(false); // whatever ended the last round is done with, this one is live again
        this.roundEndsAt = 0L; // the clock starts on GO, the countdown itself is not round time
        for (SoccerTeam team : this.teams) {
            team.roundPlays().set(0);
        }
        sendAudienceTitle("<yellow>" + atomicCountdown.get(), "");
        playAudienceSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        Executors.repeatingGlobal(20, 20, task -> {
            if (atomicCountdown.decrementAndGet() <= 0) {
                for (SulfurCubeSoccerBall ball : this.balls) {
                    ball.spawn();
                }
                task.cancel();
                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200));
                sendAudienceTitle("<dark_green>GO!", "", times);
                this.unfreezeAll();
                this.roundEndsAt = System.currentTimeMillis() + ROUND_LENGTH_MILLIS;
                playAudienceSound(Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.5f);
                Location kickoff = this.ballSpawnLocation;
                Executors.runSync(kickoff, () -> kickoff.getWorld().playSound(kickoff, Sound.ITEM_GOAT_HORN_SOUND_0, 9f, 1f));
                return;
            }
            sendAudienceTitle("<yellow>" + atomicCountdown.get(), "");
            playAudienceSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        });
    }

    @Getter
    @Accessors(fluent = true)
    private class SulfurCubeSoccerBall {

        private static final Supplier<ItemStack> RANDOM_PLANKS = () -> ItemStack.of(Util.getRandom(Tag.ITEMS_PLANKS.getValues()));
        private static final ItemStack ARCHETYPE_ITEM = ItemStack.of(Material.BAMBOO_PLANKS); // bouncy

        private static final double PUSH_DISTANCE_THRESHOLD = 1.3;
        private static final long KICK_COOLDOWN_MILLIS = 250L;

        private static final double PUSH_BASE_BLOCKS_PER_TICK = 0.35; // a standing bump, ~7 b/s
        private static final double PUSH_DRIVE_SCALE = 0.09; // per block/second the pusher is closing at
        private static final double PUSH_MAX_BLOCKS_PER_TICK = POWER_KICK_BLOCKS_PER_TICK * 0.8; // a body never quite power kicks
        private static final double PUSH_LIFT = 0.22; // a skip off the turf rather than a grind along it
        private static final Sound KICK_SOUND = Sound.ENTITY_SULFUR_CUBE_BOUNCY_PUSH;

        private final Location initialSpawnLocation;
        private final Map<UUID, Long> lastKickAt = new ConcurrentHashMap<>();
        private final SpeedTracker speed = new SpeedTracker();

        private volatile SulfurCube sulfurCube;
        private volatile boolean doSpawn;
        private volatile boolean scored;
        private volatile boolean touched;
        private volatile @Setter SoccerPlayer lastContact;

        // a kick's speed only shows up on the next tick's sample, so the credit is decided after the fact
        private volatile SoccerPlayer kicker;
        private volatile long kickedAt;
        private volatile boolean powerKickCredited;

        public SulfurCubeSoccerBall(Location initialSpawnLocation) {
            this.initialSpawnLocation = initialSpawnLocation.toCenterLocation();
        }

        public void tick() {
            // i'm on the region thread for where this sulfur cube probably is, but we need to check if it's still around
            Executors.runSync(this.activeLocation(), () -> {
                // TODO: cleanup this if branch
                if (!this.isSpawned()) {
                    return;
                } else if (this.isStale()) {
                    if (doSpawn) {
                        this.spawn();
                    } else {
                        return;
                    }
                }

                Preconditions.checkState(sulfurCube != null, "Sulfur cube should be spawned at this point!");

                Executors.runSync(sulfurCube, () -> { // get on sulfur cube's known thread as soon as we 100% can
                    this.speed.sample(sulfurCube.getLocation());
                    this.checkPowerKick();

                    if (this.checkGoals()) {
                        return;
                    }

                    if (!boundingBox.contains(sulfurCube)) {
                        if (!roundEnding.compareAndSet(false, true)) {
                            this.kill(); // another ball already called the round, this one just leaves quietly
                            return;
                        }

                        SoccerPlayer lastContact = this.lastContactRole();
                        SoccerTeam lastContactTeam = lastContact == null ? null : lastContact.team();
                        killAllBalls();
                        sendAudienceMessage("<yellow>Out of bounds by " + (lastContact == null ? "Unknown" : lastContact.getName()) + "!");
                        sendAudienceMessage("<red>Penalty: <dark_gray>" + OUT_OF_BOUNDS_PENALTY + "</dark_gray> points for " + (lastContactTeam == null ? "Unknown" : lastContactTeam.getName()) + "!");

                        if (lastContactTeam != null) {
                            scoreboard.addScore(lastContactTeam, OUT_OF_BOUNDS_PENALTY);
                            refreshBossBar();
                        }

                        countdownStart(3); // TODO: subtract points for out of bounds
                        return;
                    }

                    this.detectContact();

                    SoccerPlayer soccerPlayer = this.lastContactRole();

                    if (!sulfurCube.isOnGround() && soccerPlayer != null) { // particles when flying
                        Particle.DustOptions dustOptions = soccerPlayer.team().dustOptions();
                        sulfurCube.getWorld().spawnParticle(Particle.DUST, sulfurCube.getEyeLocation(), 10, 0.3, 0.3, 0.3, dustOptions);
                    }
                });
            });
        }

        // vanilla fires nothing when a player shoves the cube with their body, have to do the work ourselves.
        // leaving it to entity collision means every body nudges the cube separately, so a crowd cancels itself
        // out and the ball just sits there jittering. instead the whole crowd resolves to a single push: whoever
        // is driving hardest into the ball this tick takes it, and their shove is applied outright
        private void detectContact() {
            if (this.isPowered()) {
                return;
            }

            double cubeBottom = sulfurCube.getY();
            double cubeTop = cubeBottom + sulfurCube.getHeight();

            record Contender(SoccerPlayer soccerPlayer, Vector push, double drive) {}
            List<Contender> contenders = new ArrayList<>();

            for (Player player : sulfurCube.getLocation().getNearbyPlayers(PUSH_DISTANCE_THRESHOLD + 1.0)) {
                Location playerLocation = player.getLocation();
                double dx = sulfurCube.getX() - playerLocation.getX();
                double dz = sulfurCube.getZ() - playerLocation.getZ();
                if (dx * dx + dz * dz >= PUSH_DISTANCE_THRESHOLD * PUSH_DISTANCE_THRESHOLD) {
                    continue;
                }

                double playerFeet = playerLocation.getY();
                if (playerFeet > cubeTop || playerFeet + player.getHeight() <= cubeBottom) {
                    continue; // no vertical overlap
                }

                SoccerPlayer soccerPlayer = roleMap.as(player.getUniqueId(), SoccerPlayer.class);
                if (soccerPlayer == null) {
                    continue;
                }

                // standing dead centre on the cube leaves nothing to push along, so fall back to where they look
                Vector push = new Vector(dx, 0.0, dz);
                if (push.lengthSquared() < 1.0E-6) {
                    push = playerLocation.getDirection().setY(0.0);
                    if (push.lengthSquared() < 1.0E-6) {
                        continue;
                    }
                }
                push.normalize();

                // how fast they are actually closing on the ball, not just that they are stood next to it
                Vector running = soccerPlayer.speed().velocity();
                double drive = running == null ? 0.0 : running.setY(0.0).dot(push);
                contenders.add(new Contender(soccerPlayer, push, drive));
            }

            if (contenders.isEmpty()) {
                return;
            }

            contenders.sort(Comparator.comparingDouble(Contender::drive).reversed());
            for (Contender contender : contenders) {
                if (this.registerContact(contender.soccerPlayer(), sulfurCube.getLocation())) {
                    this.applyPush(contender.push(), contender.drive());
                    return;
                }
            }
        }

        private void applyPush(Vector push, double drive) {
            double power = Math.clamp(
                PUSH_BASE_BLOCKS_PER_TICK + Math.max(0.0, drive) * PUSH_DRIVE_SCALE,
                PUSH_BASE_BLOCKS_PER_TICK,
                PUSH_MAX_BLOCKS_PER_TICK);

            Vector impulse = push.multiply(power);
            impulse.setY(sulfurCube.isOnGround() ? PUSH_LIFT : sulfurCube.getVelocity().getY());
            sulfurCube.setVelocity(impulse);
        }

        private void handleMove(EntityMoveEvent event) {
            if (sulfurCube == null || !event.getEntity().equals(sulfurCube) || !event.hasChangedPosition()) {
                return;
            }

            Location to = event.getTo();
            Vector start = event.getFrom().toVector();
            Vector path = to.toVector().subtract(start);
            double distance = path.length();
            if (distance < 1.0E-4) {
                return;
            }
            // parking the cube against a body is what would stop a power kick dead, so a shot travelling this
            // fast is left alone. it tunnels past everyone until a punch turns it
            if (distance >= POWER_KICK_BLOCKS_PER_TICK) {
                return;
            }

            Vector direction = path.multiply(1.0 / distance);

            // goals are deliberately not tested against this path they're only sampled once a tick by
            // the game loop, so a shot has to actually sit in the goal (TODO) to count and a keeper gets a swing at it
            this.sweepPlayers(event, to, start, direction, distance);
        }

        /** @return true if the cube was parked against a player it would otherwise have skipped straight past */
        private boolean sweepPlayers(EntityMoveEvent event, Location to, Vector start, Vector direction, double distance) {
            double height = sulfurCube.getHeight();
            for (Player player : to.getNearbyPlayers(distance + PUSH_DISTANCE_THRESHOLD + 1.0)) {
                SoccerPlayer soccerPlayer = roleMap.as(player.getUniqueId(), SoccerPlayer.class);
                if (soccerPlayer == null) {
                    continue;
                }

                BoundingBox pushRange = player.getBoundingBox().expand(PUSH_DISTANCE_THRESHOLD, height, PUSH_DISTANCE_THRESHOLD);
                if (pushRange.contains(to.toVector()) || pushRange.contains(start)) {
                    return false;
                }

                RayTraceResult crossing = pushRange.rayTrace(start, direction, distance);
                if (crossing == null) {
                    continue;
                }

                if (!this.registerContact(soccerPlayer, to)) {
                    return false;
                }

                Vector entry = crossing.getHitPosition();
                event.setTo(new Location(to.getWorld(), entry.getX(), entry.getY(), entry.getZ(), to.getYaw(), to.getPitch()));
                return true;
            }

            return false;
        }

        private boolean checkGoals() {
            if (this.scored) {
                return true;
            }

            for (SoccerTeam goal : teams) {
                if (!goal.goalBoxOverlaps(sulfurCube)) {
                    continue;
                }

                SoccerPlayer scorer = this.lastContactRole();
                if (scorer == null) {
                    LOGGER.info("Sulfur cube reached a goal without a player!");
                    return false;
                }

                if (!roundEnding.compareAndSet(false, true)) {
                    this.scored = true; // beaten to it by another ball, this goal doesn't count
                    return true;
                }

                this.scored = true;
                goal.scoreGoal(this, scorer);
                return true;
            }

            return false;
        }

        private boolean registerContact(SoccerPlayer soccerPlayer, Location at) {
            this.lastContact(soccerPlayer); // whoever touched it last owns the next goal

            long now = System.currentTimeMillis();
            Long lastKick = this.lastKickAt.get(soccerPlayer.getUuid());
            if (lastKick != null && now - lastKick < KICK_COOLDOWN_MILLIS) {
                return false;
            }
            this.lastKickAt.put(soccerPlayer.getUuid(), now);

            this.sulfurCube.getEquipment().setItem(EquipmentSlot.BODY, RANDOM_PLANKS.get()); // random plank for the next push
            at.getWorld().playSound(at, KICK_SOUND, 1.2f, 0.9f + (float) Math.random() * 0.2f);

            // awarded least notable first, so the action bar ends up showing the best of them
            if (!this.touched) {
                this.touched = true;
                soccerPlayer.award(Feat.FIRST_TOUCH);
            }
            // check if this cube is at least 0.75 off the ground
            if (!this.sulfurCube.isOnGround() && this.sulfurCube.getLocation().getY() - this.sulfurCube.getWorld().getBlockAt(this.sulfurCube.getLocation()).getY() > 0.75) {
                soccerPlayer.award(Feat.AIR_HIT);
            }
            if (threatensGoal(soccerPlayer.team(), this.sulfurCube)) {
                soccerPlayer.award(Feat.SAVE);
            }

            this.markKick(soccerPlayer);
            return true;
        }

        private boolean isPowered() {
            return this.speed.blocksPerSecond() >= POWER_KICK_BPS;
        }

        private void markKick(SoccerPlayer soccerPlayer) {
            this.kicker = soccerPlayer;
            this.kickedAt = System.currentTimeMillis();
            this.powerKickCredited = false;
        }

        private void checkPowerKick() {
            SoccerPlayer kicker = this.kicker;
            if (kicker == null || this.powerKickCredited) {
                return;
            }
            if (System.currentTimeMillis() - this.kickedAt > POWER_KICK_WINDOW_MILLIS) {
                this.kicker = null; // the kick never got up to speed, nothing to credit
                return;
            }
            if (this.speed.blocksPerSecond() >= POWER_KICK_BPS) {
                this.powerKickCredited = true;
                kicker.award(Feat.POWER_KICK);
            }
        }

        public boolean spawn() {
            if (this.isSpawned()) {
                return false;
            }
            this.doSpawn = true; // force this value
            this.lastContact = null; // a fresh ball isn't owned by last round's scorer
            this.lastKickAt.clear();
            this.scored = false;
            this.touched = false; // first touch is up for grabs again
            this.kicker = null;
            this.speed.reset(); // the jump from where the old ball died to the new spawn isn't a speed
            Executors.sync(this.activeLocation(), () -> {
                this.sulfurCube = this.activeLocation().getWorld().spawn(this.activeLocation(), SulfurCube.class, cube -> {
                    cube.getEquipment().setItem(EquipmentSlot.BODY, ARCHETYPE_ITEM);
                    cube.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 3, false, false, false));
                });
            });
            return true;
        }

        public boolean kill() {
            if (this.sulfurCube == null) {
                return false;
            }
            this.speed.reset();
            Executors.runSync(this.sulfurCube, () -> {
                this.sulfurCube.remove();
                this.sulfurCube = null;
            });
            return true;
        }

        public boolean isSpawned() {
            return sulfurCube != null && sulfurCube.isValid();
        }

        public boolean isStale() {
            return sulfurCube == null || sulfurCube.isDead();
        }

        public Location activeLocation() {
            return sulfurCube == null ? this.initialSpawnLocation : sulfurCube.getLocation();
        }

        public @Nullable Player lastContact() {
            if (this.lastContact != null) {
                return this.lastContact.getEventPlayer().getPlayer();
            }

            if (this.isStale()) {
                return null;
            }

            EntityDamageEvent lastDamageEvent = sulfurCube.getLastDamageCause();
            if (!(lastDamageEvent instanceof EntityDamageByEntityEvent damageByEntity)) {
                return null;
            }
            return damageByEntity.getDamager() instanceof Player damager ? damager : null;
        }

        public @Nullable SoccerPlayer lastContactRole() {
            if (this.lastContact != null) {
                return this.lastContact;
            }

            Player lastContact = this.lastContact();
            if (lastContact == null) return null;
            return roleMap.as(lastContact.getUniqueId(), SoccerPlayer.class);
        }
    }

    private static final class SpeedTracker {

        private volatile Location last;
        private volatile long lastAt;
        private volatile double blocksPerSecond;
        private volatile @Nullable Vector velocity;

        private void sample(Location current) {
            long now = System.currentTimeMillis();
            Location previous = this.last;
            if (previous != null && previous.getWorld().equals(current.getWorld()) && now > this.lastAt) {
                double seconds = (now - this.lastAt) / 1000.0;
                this.blocksPerSecond = previous.distance(current) / seconds;
                this.velocity = current.toVector().subtract(previous.toVector()).multiply(1.0 / seconds);
            }
            this.last = current;
            this.lastAt = now;
        }

        private void reset() {
            this.last = null;
            this.blocksPerSecond = 0.0;
            this.velocity = null;
        }

        // a snapshot, safe to read off the sampled entity's thread
        private @Nullable Location lastSample() {
            return this.last;
        }

        private double blocksPerSecond() {
            return this.blocksPerSecond;
        }

        private @Nullable Vector velocity() {
            Vector snapshot = this.velocity;
            return snapshot == null ? null : snapshot.clone();
        }
    }

    @Getter
    @Accessors(fluent = true)
    private class SoccerPlayer extends MinigameRole implements Scorer {

        private static final PotionEffect STRENGTH = new PotionEffect(PotionEffectType.STRENGTH, 300, 1, false, false, false);
        private static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 300, 3, false, false, false);
        private static final String SPEED_READOUT = "<gray>Ball <white>%.1f<gray> b/s <dark_gray>| <gray>You <white>%.1f<gray> b/s";

        private final SoccerTeam team;
        private final SpeedTracker speed = new SpeedTracker();

        private volatile long actionBarQuietUntil;
        private volatile Feat feat;
        private volatile long featAt;
        private int ticks;

        protected SoccerPlayer(EventPlayer eventPlayer, SoccerTeam team) {
            super(eventPlayer);
            this.team = team;
        }

        public void onTick() {
            this.operatePlayer(player -> {
                if (player.getSaturation() < 18) {
                    player.setSaturation(20f);
                    player.setFoodLevel(20);
                }
                player.addPotionEffect(STRENGTH);
                player.addPotionEffect(SPEED);

                this.speed.sample(player.getLocation());
                if (System.currentTimeMillis() >= this.actionBarQuietUntil) {
                    String readout = String.format(Locale.ROOT, SPEED_READOUT,
                        nearestBallSpeed(player.getLocation()), this.speed.blocksPerSecond());
                    Feat active = this.activeFeat();
                    if (active != null) {
                        readout += "<gray> — " + active.formatted();
                    }
                    player.sendActionBar(Util.color(readout));
                }

                if (this.ticks++ % GOAL_MARKER_INTERVAL_TICKS == 0) {
                    showGoalMarkers(player, this.team);
                    if (player.getInventory().getChestplate().isEmpty()) {
                        player.getInventory().setChestplate(this.team.chestplate());
                    }
                }
            });
        }

        public void quietActionBar(long millis) {
            this.actionBarQuietUntil = System.currentTimeMillis() + millis;
        }

        // the only way a player earns points. called from whichever region thread spotted the play
        public void award(Feat feat) {
            long now = System.currentTimeMillis();
            if (feat == this.feat && now - this.featAt < FEAT_REPEAT_COOLDOWN_MILLIS) {
                return; // a punch and a body push landing on the same play shouldn't pay twice
            }

            synchronized (playerScoreboard) {
                playerScoreboard.addScore(this, 1);
            }
            this.team.roundPlays().incrementAndGet();
            this.feat = feat;
            this.featAt = now;
        }

        private @Nullable Feat activeFeat() {
            return System.currentTimeMillis() - this.featAt < FEAT_DISPLAY_MILLIS ? this.feat : null;
        }

        public void teleportToStart() {
            Location randomSpread = this.team.startLocation()
                .clone()
                .add(0.5, 0, 0.5)
                .add(Math.random() * 5 - 2.5, 0, Math.random() * 5 - 2.5);
            this.speed.reset(); // a teleport is not a sprint
            this.teleportAsync(randomSpread);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SoccerPlayer other)) return false;
            return this.getEventPlayer().getUuid().equals(other.getEventPlayer().getUuid());
        }

        @Override
        public int hashCode() {
            return this.getEventPlayer().getUuid().hashCode(); // playerScoreboard keys on these
        }

        @Override
        public String getName() {
            return super.getName();
        }
    }

    @Getter
    @Accessors(fluent = true)
    private class SoccerTeam implements Scorer {

        private final Location startLocation;
        private final WorldTiedBoundingBox goalBox;
        private final TeamTemplate template;
        private final Particle.DustOptions dustOptions;
        private final ItemStack chestplate;
        private final AtomicInteger roundPlays = new AtomicInteger(); // decides a round nobody scored in

        private SoccerTeam(Location startLocation, WorldTiedBoundingBox goalBox, TeamTemplate template) {
            this.startLocation = startLocation;
            this.goalBox = goalBox;
            this.template = template;
            this.dustOptions = new Particle.DustOptions(Color.fromRGB(template.color()), 1.2f);
            this.chestplate = ItemStack.of(Material.LEATHER_CHESTPLATE);
            this.chestplate.editMeta(LeatherArmorMeta.class, meta -> {
                meta.setColor(Color.fromRGB(template.color()));
                //meta.setUnbreakable(true);
                //meta.displayName(Util.color(template.formatted(template.teamName())).decoration(TextDecoration.ITALIC, false));
            });
        }

        public boolean goalBoxOverlaps(Entity entity) {
            return goalBox.getWorld().equals(entity.getWorld()) && goalBox.overlaps(entity.getBoundingBox());
        }

        public double distanceSquaredTo(Location location) {
            double dx = Math.max(0.0, Math.max(goalBox.getMinX() - location.getX(), location.getX() - goalBox.getMaxX()));
            double dy = Math.max(0.0, Math.max(goalBox.getMinY() - location.getY(), location.getY() - goalBox.getMaxY()));
            double dz = Math.max(0.0, Math.max(goalBox.getMinZ() - location.getZ(), location.getZ() - goalBox.getMaxZ()));
            return dx * dx + dy * dy + dz * dz;
        }

        public boolean withinPunchRadius(Location location) {
            return goalBox.getWorld().equals(location.getWorld())
                && this.distanceSquaredTo(location) <= (double) goalPunchRadius * goalPunchRadius;
        }

        public void scoreGoal(SulfurCubeSoccerBall ball, SoccerPlayer scorer) {
            // Check if the scorer scored on their own team first

            String scorerName = scorer.getEventPlayer().getName();
            SoccerTeam beneficiary;
            if (scorer.team().equals(this)) {
                beneficiary = opponentOf(this);
                sendAudienceMessage(template.formatted(scorerName + " scored on their own goal! <dark_gray>(-" + POINTS_PER_GOAL + " points) <gray>—</gray> " + Util.getRandom(EXTRA_OWN_GOAL_NUANCE).formatted(scorerName, POINTS_PER_GOAL)));
            } else {
                beneficiary = scorer.team();
                sendAudienceMessage(scorer.team().template().formatted(scorerName + " scored! <dark_gray>(+" + POINTS_PER_GOAL + " points)"));
                scorer.award(Feat.GOAL);
            }

            finishRound(beneficiary, ball.activeLocation().clone());
        }

        @Override
        public String getName() {
            return this.template.teamName();
        }

    }


    @Getter
    @Accessors(fluent = true)
    private enum TeamTemplate {

        RED("Red Team", "Good Guys", 0xE2453B, BossBar.Color.RED),
        BLUE("Blue Team", "Bad Guys", 0x3B7DE2, BossBar.Color.BLUE),

        KIT_KAT("Team KitKat", "KitKats", 0xE60012, BossBar.Color.RED),
        OREO("Team Oreo", "Oreos", 0x4A6DB5, BossBar.Color.BLUE),

        MAYO("Team Mayo", "Mayo", 0xF3E9C0, BossBar.Color.WHITE),
        MUSTARD("Team Mustard", "Mustard", 0xE1AD01, BossBar.Color.YELLOW),
        ;

        private final String teamName;
        private final String bossBarName;
        private final int color;
        private final String messageFormat;
        private final BossBar.Color barColor;

        TeamTemplate(String teamName, String bossBarName, int color, BossBar.Color barColor) {
            this(teamName, bossBarName, color, barColor, "<#%06X>%%s</#%06X>".formatted(color, color));
        }

        TeamTemplate(String teamName, String bossBarName, int color, BossBar.Color barColor, String messageFormat) {
            this.teamName = teamName;
            this.bossBarName = bossBarName;
            this.color = color;
            this.barColor = barColor;
            this.messageFormat = messageFormat;
        }

        public String formatted(String message) {
            return String.format(this.messageFormat, message);
        }
    }

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    private enum Feat {

        FIRST_TOUCH("First touch!", "<yellow>"),
        AIR_HIT("Air hit!", "<aqua>"),
        POWER_KICK("Power kick!", "<gold>"),
        SAVE("Save!", "<green>"),
        GOAL("Goal!", "<light_purple>"),
        ;

        private final String display;
        private final String color;

        public String formatted() {
            return this.color + "<b>" + this.display + "</b>";
        }
    }

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    private enum TeamMatchup {

        RED_VS_BLUE(TeamTemplate.RED, TeamTemplate.BLUE),
        KIT_KAT_VS_OREO(TeamTemplate.KIT_KAT, TeamTemplate.OREO),
        MAYO_VS_MUSTARD(TeamTemplate.MAYO, TeamTemplate.MUSTARD),
        ;

        private final TeamTemplate first;
        private final TeamTemplate second;
    }
}
