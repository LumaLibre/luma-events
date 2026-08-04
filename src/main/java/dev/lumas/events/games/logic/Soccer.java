package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.google.common.base.Preconditions;
import dev.lumas.core.util.ContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.SulfurSoccerDefinition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final int PLAYER_SELF_GOAL_PENALTY = -5;

    private static final Particle.DustOptions OWN_GOAL_DUST = new Particle.DustOptions(Color.fromRGB(0xE23B3B), 2.0f);
    private static final Particle.DustOptions ENEMY_GOAL_DUST = new Particle.DustOptions(Color.fromRGB(0x3BE24E), 2.0f);
    private static final int GOAL_MARKER_PARTICLES = 12;
    private static final int GOAL_MARKER_INTERVAL_TICKS = 10;

    private final Location spawnLocation;
    private final List<SoccerTeam> teams;
    private final SulfurCubeSoccerBall soccerBall;
    private final MinigameRoleMap<SoccerPlayer> roleMap = new MinigameRoleMap<>();
    private final Scoreboard<SoccerTeam> scoreboard = new Scoreboard<>();
    private final Scoreboard<SoccerPlayer> playerScoreboard = new Scoreboard<>();
    private final int goalPunchRadius;

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0.0f, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);

    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();


    public Soccer(SulfurSoccerDefinition def) {
        super(
            "Soccer",
            Util.getRandom(SPLASH),
            1200000L,
            1,
            true,
            true,
            false,
            true
        );

        this.spawnLocation = def.getSpawnLocation();
        this.boundingBox = def.getBounds().toWorldTiedBoundingBox();

        TeamMatchup matchup = Util.getRandom(TeamMatchup.values());
        this.teams = List.of(
            new SoccerTeam(def.getTeam1StartLocation(), def.getTeam1Goal().toWorldTiedBoundingBox(), matchup.first()),
            new SoccerTeam(def.getTeam2StartLocation(), def.getTeam2Goal().toWorldTiedBoundingBox(), matchup.second())
        );

        this.soccerBall = new SulfurCubeSoccerBall(def.getSoccerBallStartLocation());
        this.goalPunchRadius = def.getPunchRadius();
    }

    @Override
    protected int minimumParticipants() {
        return 1; // TODO: Change to 2
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: Tokens
//        int score = this.playerScoreboard.getScore(participant);
//        participant.addPermanentScore(MinigameConstant.SOCCER, );
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        this.frozenPlayers.remove(participant.getUuid());
        participant.operatePlayer(Soccer::unfreeze);
        participant.removeBossBar(this.bossBar);
        this.roleMap.remove(participant.getUuid());
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

        this.refreshBossBar();
        for (SoccerPlayer soccerPlayer : this.roleMap) {
            soccerPlayer.getEventPlayer().addBossBar(this.bossBar);
        }

        this.countdownStart(5);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        this.soccerBall.tick();
        for (SoccerPlayer player : this.roleMap) {
            player.onTick();
        }

        if (timeLeft <= 0) {
            sendAudienceMessage("Times up, game over!");
        }
    }

    @Override
    protected void handleStop() {
        this.soccerBall.kill();
        this.unfreezeAll();
        for (SoccerPlayer soccerPlayer : this.roleMap) {
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

    @EventHandler
    public void onSulfurCubePunch(EntityKnockbackByEntityEvent event) {
        SulfurCubeSoccerBall ball = this.soccerBall;
        if (!ball.isSpawned() || !event.getEntity().equals(ball.sulfurCube())) {
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
    }

    @EventHandler(ignoreCancelled = true)
    public void onSulfurCubeMove(EntityMoveEvent event) {
        this.soccerBall.handleMove(event);
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
        this.soccerBall.spawn();
        sendAudienceTitle("<yellow>" + atomicCountdown.get(), "");
        playAudienceSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        Executors.repeatingGlobal(20, 20, task -> {
            if (atomicCountdown.decrementAndGet() <= 0) {
                task.cancel();
                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200));
                sendAudienceTitle("<dark_green>GO!", "", times);
                this.unfreezeAll();
                playAudienceSound(Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.5f);
                Executors.runSync(this.soccerBall.activeLocation(), () -> {
                    this.soccerBall.activeLocation().getWorld().playSound(this.soccerBall.activeLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 9f, 1f);
                });
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
        private static final Sound KICK_SOUND = Sound.ENTITY_SULFUR_CUBE_BOUNCY_PUSH;

        private final Location initialSpawnLocation;
        private final Map<UUID, Long> lastKickAt = new ConcurrentHashMap<>();
        private final SpeedTracker speed = new SpeedTracker();

        private volatile SulfurCube sulfurCube;
        private volatile boolean doSpawn;
        private volatile boolean scored;
        private volatile @Setter SoccerPlayer lastContact;

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

                    if (this.checkGoals()) {
                        return;
                    }

                    if (!boundingBox.contains(sulfurCube)) {
                        SoccerPlayer lastContact = this.lastContactRole();
                        SoccerTeam lastContactTeam = lastContact == null ? null : lastContact.team();
                        this.kill();
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

        // vanilla fires nothing when a player shoves the cube with their body, have to do the work ourselves
        private void detectContact() {
            double cubeBottom = sulfurCube.getY();
            double cubeTop = cubeBottom + sulfurCube.getHeight();

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

                this.registerContact(soccerPlayer, sulfurCube.getLocation());
                return; // one contact per tick
            }
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

            if (RANDOM.nextBoolean()) {
                playerScoreboard.addScore(soccerPlayer, 1); // TODO: Vary based on velocity
            }
            return true;
        }

        public boolean spawn() {
            if (this.isSpawned()) {
                return false;
            }
            this.doSpawn = true; // force this value
            this.lastContact = null; // a fresh ball isn't owned by last round's scorer
            this.lastKickAt.clear();
            this.scored = false;
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

        private void sample(Location current) {
            long now = System.currentTimeMillis();
            Location previous = this.last;
            if (previous != null && previous.getWorld().equals(current.getWorld()) && now > this.lastAt) {
                this.blocksPerSecond = previous.distance(current) / ((now - this.lastAt) / 1000.0);
            }
            this.last = current;
            this.lastAt = now;
        }

        private void reset() {
            this.last = null;
            this.blocksPerSecond = 0.0;
        }

        private double blocksPerSecond() {
            return this.blocksPerSecond;
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
                    player.sendActionBar(Util.color(String.format(Locale.ROOT, SPEED_READOUT,
                        soccerBall.speed().blocksPerSecond(), this.speed.blocksPerSecond())));
                }

                if (this.ticks++ % GOAL_MARKER_INTERVAL_TICKS == 0) {
                    showGoalMarkers(player, this.team);
                }
            });
        }

        public void quietActionBar(long millis) {
            this.actionBarQuietUntil = System.currentTimeMillis() + millis;
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
        public String getName() {
            return super.getName();
        }
    }

    @Getter
    @Accessors(fluent = true)
    private class SoccerTeam implements Scorer {

        private final Map<SoccerPlayer, Integer> pointMap = new HashMap<>();

        private final Location startLocation;
        private final WorldTiedBoundingBox goalBox;
        private final TeamTemplate template;
        private final Particle.DustOptions dustOptions;

        private SoccerTeam(Location startLocation, WorldTiedBoundingBox goalBox, TeamTemplate template) {
            this.startLocation = startLocation;
            this.goalBox = goalBox;
            this.template = template;
            this.dustOptions = new Particle.DustOptions(Color.fromRGB(template.color()), 1.2f);
        }

        public boolean goalBoxOverlaps(Entity entity) {
            return goalBox.getWorld().equals(entity.getWorld()) && goalBox.overlaps(entity.getBoundingBox());
        }

        public boolean withinPunchRadius(Location location) {
            if (!goalBox.getWorld().equals(location.getWorld())) {
                return false;
            }

            double dx = Math.max(0.0, Math.max(goalBox.getMinX() - location.getX(), location.getX() - goalBox.getMaxX()));
            double dy = Math.max(0.0, Math.max(goalBox.getMinY() - location.getY(), location.getY() - goalBox.getMaxY()));
            double dz = Math.max(0.0, Math.max(goalBox.getMinZ() - location.getZ(), location.getZ() - goalBox.getMaxZ()));

            return dx * dx + dy * dy + dz * dz <= (double) goalPunchRadius * goalPunchRadius;
        }

        public void scoreGoal(SulfurCubeSoccerBall ball, SoccerPlayer scorer) {
            // Check if the scorer scored on their own team first

            String scorerName = scorer.getEventPlayer().getName();
            if (scorer.team().equals(this)) {
                // get opposing team
                SoccerTeam opposingTeam = teams.stream().filter(team -> !team.equals(this)).findFirst().orElse(null);
                Preconditions.checkNotNull(opposingTeam, "No opposing team found for scorer's team: " + scorer.team().getName());

                scoreboard.addScore(opposingTeam, POINTS_PER_GOAL);
                playerScoreboard.addScore(scorer, PLAYER_SELF_GOAL_PENALTY);
                sendAudienceMessage(template.formatted(scorerName + " scored on their own goal! <dark_gray>(-" + POINTS_PER_GOAL + " points) <gray>—</gray> " + Util.getRandom(EXTRA_OWN_GOAL_NUANCE).formatted(scorerName, POINTS_PER_GOAL)));
            } else {
                sendAudienceMessage(scorer.team().template().formatted(scorerName + " scored! <dark_gray>(+" + POINTS_PER_GOAL + " points)"));
                scoreboard.addScore(scorer.team(), POINTS_PER_GOAL);
                playerScoreboard.addScore(scorer, POINTS_PER_GOAL);
            }
            refreshBossBar();

            // capture where the ball is before we remove it
            Location ballLoc = ball.activeLocation().clone();

            // kill the sulfur cube and have it explode into a bunch of dust particles
            ball.kill();

            ParticleDisplay particleDisplay = ParticleDisplay.of(Particle.DUST)
                .withColor(new java.awt.Color(scorer.team().template().color()))
                .withLocation(ballLoc);
            Particles.spikeSphere(6.0, 25.0, 3, 0.4, 0.8, particleDisplay);


            for (SoccerTeam team : teams) {
                Location goalLocation = team.goalBox().getCenterLocation();
                goalLocation.getWorld().playSound(goalLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 4f, 2f);
                goalLocation.getWorld().playSound(goalLocation, Sound.ITEM_GOAT_HORN_SOUND_2, 4f, 1.25f);
            }

            Location ballInitialSpawnLoc = ball.initialSpawnLocation();
            ballInitialSpawnLoc.getWorld().playSound(ballInitialSpawnLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 4f, 2f);
            ballInitialSpawnLoc.getWorld().playSound(ballInitialSpawnLoc, Sound.ITEM_GOAT_HORN_SOUND_2, 4f, 1.25f);

            // blast every entity within a 25 block radius away from the goal
            for (LivingEntity entity : ballLoc.getNearbyLivingEntities(35)) {
                Vector direction = entity.getLocation().toVector().subtract(ballLoc.toVector());
                if (direction.lengthSquared() < 1.0E-6) {
                    direction = new Vector(0, 1, 0); // entity sitting exactly on the goal, just launch it up
                } else {
                    direction.normalize();
                }

                // stronger blast the closer the entity is to the goal
                double distance = entity.getLocation().distance(ballLoc);
                double strength = Math.max(0.5, 2.8 * (1 - distance / 35.0));

                Vector knockback = direction.multiply(strength);
                knockback.setY(knockback.getY() + 0.7); // add a little lift
                entity.setVelocity(knockback);
            }

            // wait 3.5 seconds before starting a new round
            Executors.delayedSync(ballLoc, 70, () -> {
                if (leadOf(scorer.team()) >= LEAD_TO_WIN) {
                    sendAudienceMessage("<red>Game over!");
                    sendAudienceMessage(scorer.team().template().formatted(scorer.team().getName() + " has won!"));
                    stop();
                } else {
                    countdownStart(3);
                }
            });
        }

        @Override
        public String getName() {
            return this.template.teamName();
        }

        public void addPoints(SoccerPlayer soccerPlayer, int amount) { // TODO:
            this.pointMap.putIfAbsent(soccerPlayer, 0);
            this.pointMap.put(soccerPlayer, this.pointMap.get(soccerPlayer) + amount);
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
    private enum TeamMatchup {

        RED_VS_BLUE(TeamTemplate.RED, TeamTemplate.BLUE),
        KIT_KAT_VS_OREO(TeamTemplate.KIT_KAT, TeamTemplate.OREO),
        MAYO_VS_MUSTARD(TeamTemplate.MAYO, TeamTemplate.MUSTARD),
        ;

        private final TeamTemplate first;
        private final TeamTemplate second;
    }
}
