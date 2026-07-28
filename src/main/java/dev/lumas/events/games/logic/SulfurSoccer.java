package dev.lumas.events.games.logic;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.ContextLogger;
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
import dev.lumas.lumaitems.particles.ParticleDisplay;
import dev.lumas.lumaitems.particles.Particles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: bossbars for each team showing how close they are to winning/goals
public final class SulfurSoccer extends InventoryUnifiedMinigame {

    private static final ContextLogger LOGGER = ContextLogger.getLogger(NamedTextColor.YELLOW, false);

    private final Location spawnLocation;
    private final Set<SoccerTeam> teams;
    private final SulfurCubeSoccerBall soccerBall;
    private final MinigameRoleMap<SoccerPlayer> roleMap = new MinigameRoleMap<>();
    private final Scoreboard<SoccerTeam> scoreboard = new Scoreboard<>();


    public SulfurSoccer(SulfurSoccerDefinition def) {
        super(
            "Sulfur Soccer",
            "Kick the sulfur cube into opposing team's goal!",
            300000L,
            1,
            true,
            true,
            false,
            true
        );

        this.spawnLocation = def.getSpawnLocation();
        this.boundingBox = def.getBounds().toWorldTiedBoundingBox();

        this.teams = Set.of(
            new SoccerTeam(def.getTeam1StartLocation(), def.getTeam1Goal().toWorldTiedBoundingBox(), TeamColorTemplate.RED),
            new SoccerTeam(def.getTeam2StartLocation(), def.getTeam2Goal().toWorldTiedBoundingBox(), TeamColorTemplate.BLUE)
        );

        this.soccerBall = new SulfurCubeSoccerBall(def.getSoccerBallStartLocation());
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    protected void handleStart() {
        if (this.teams.size() != 2) {
            this.stop();
            throw new IllegalStateException("Illegal number of teams, expected 2 but got: " + this.teams.size() + " - " + this.teams);
        }

        SoccerTeam team1 = this.teams.stream().findFirst().orElseThrow();
        SoccerTeam team2 = this.teams.stream().skip(1).findFirst().orElseThrow();


        List<EventPlayer> shuffled = new ArrayList<>(this.getParticipants());
        Collections.shuffle(shuffled);
        for (EventPlayer eventPlayer : shuffled) {
            SoccerTeam team = this.roleMap.size() % 2 == 0 ? team1 : team2;
            SoccerPlayer soccerPlayer = new SoccerPlayer(eventPlayer, team);
            this.roleMap.put(soccerPlayer);
        }

        this.countdownStart(5);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        this.soccerBall.tick();
    }

    @Override
    protected void handleStop() {
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

    @EventHandler
    public void onSulfurCubeContact(EntityDamageByEntityEvent event) {
        SulfurCubeSoccerBall ball = this.soccerBall;
        if (!event.getEntity().equals(ball.sulfurCube())) {
            return;
        }

        SoccerPlayer attacker = this.roleMap.as(event.getDamager().getUniqueId(), SoccerPlayer.class);

        if (attacker == null) return;

        if (event.getCause() != EntityDamageEvent.DamageCause.CONTACT) {
            event.setCancelled(true); // only allow contact directly
            return;
        }

        ball.lastContact(attacker);
        attacker.onKick(ball);
    }

    public void countdownStart(final int countdown) {
        for (SoccerPlayer soccerPlayer : roleMap) {
            soccerPlayer.teleportToStart();
            soccerPlayer.slowDown(countdown);
        }

        AtomicInteger atomicCountdown = new AtomicInteger(countdown);
        Executors.repeatingGlobal(20, task -> {
            if (atomicCountdown.decrementAndGet() <= 0) {
                task.cancel();
                sendAudienceMessage("<dark_green>GO!");
                this.soccerBall.spawn();
                return;
            }
            sendAudienceMessage("<yellow>" + atomicCountdown.get());
        });
    }

    @Getter
    @Accessors(fluent = true)
    private class SulfurCubeSoccerBall {

        private static final int FALLBACK_COLOR = 0x210B2F;
        private static final Particle.DustOptions FALLBACK_DUST_OPTIONS = new Particle.DustOptions(Color.fromRGB(FALLBACK_COLOR), 1.2f);
        private static final ItemStack ARCHETYPE_ITEM = ItemStack.of(Material.STRIPPED_PALE_OAK_WOOD); // bouncy

        private final Location initialSpawnLocation;

        private volatile SulfurCube sulfurCube;
        private volatile boolean doSpawn;
        private volatile @Setter SoccerPlayer lastContact;

        public SulfurCubeSoccerBall(Location initialSpawnLocation) {
            this.initialSpawnLocation = initialSpawnLocation;
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
                    SoccerPlayer soccerPlayer = this.lastContactRole();

                    if (!sulfurCube.isOnGround()) { // particles when flying
                        Particle.DustOptions dustOptions = soccerPlayer == null ? FALLBACK_DUST_OPTIONS : soccerPlayer.team().dustOptions();
                        sulfurCube.getWorld().spawnParticle(Particle.DUST, sulfurCube.getLocation(), 10, 0.6, 0.6, 0.6, dustOptions);
                    }

                    // check goal statuses
                    for (SoccerTeam goal : teams) {
                        if (goal.goalBoxOverlaps(sulfurCube)) {
                            if (soccerPlayer == null) {
                                LOGGER.info("Sulfur cube contacted a goal without a player!");
                            } else {
                                goal.scoreGoal(this, soccerPlayer);
                            }
                            break; // only score once
                        }
                    }
                });
            });
        }

        public boolean spawn() {
            if (this.isSpawned()) {
                return false;
            }
            this.doSpawn = true; // force this value
            this.sulfurCube = this.activeLocation().getWorld().spawn(this.activeLocation(), SulfurCube.class, cube -> {
                cube.getEquipment().setItem(EquipmentSlot.BODY, ARCHETYPE_ITEM);
            });
            return true;
        }

        public boolean kill() {
            if (this.isStale()) {
                return false;
            }
            this.sulfurCube.remove();
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

            EntityDamageEvent lastDamageEvent = sulfurCube.getLastDamageCause();
            if (lastDamageEvent == null) {
                return null;
            }
            return lastDamageEvent.getEntity() instanceof Player ? (Player) lastDamageEvent.getEntity() : null;
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

    @Getter
    @Accessors(fluent = true)
    private class SoccerPlayer extends MinigameRole {

        private static final double GROUNDED_KICK_Y_BOOST = 0.35;

        private final SoccerTeam team;

        protected SoccerPlayer(EventPlayer eventPlayer, SoccerTeam team) {
            super(eventPlayer);
            this.team = team;
        }

        public void teleportToStart() {
            // random spread of 5 block in x and z
            Location randomSpread = this.team.startLocation()
                .clone()
                .add(Math.random() * 5 - 2.5, 0, Math.random() * 5 - 2.5);
            this.teleportAsync(randomSpread);
        }

        public void slowDown(int seconds) {
            int ticks = seconds * 20;
            PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, ticks, 70, false, false, false);
            this.getEventPlayer().operatePlayer(player -> {
                player.addPotionEffect(effect);
            });
        }

        public void onKick(SulfurCubeSoccerBall ball) {
            SulfurCube cube = ball.sulfurCube();
            if (cube == null || !cube.isOnGround()) {
                return; // mid air kicks keep whatever arc they already have
            }

            // if knockback gets applied after this event resolves, wait a tick before lifting it
//            Executors.delayedSync(cube, 1, () -> {
//                if (!cube.isValid()) return;
//
//            });
            cube.setVelocity(cube.getVelocity().add(new Vector(0, GROUNDED_KICK_Y_BOOST, 0)));
            this.team().addPoints(this, 1);
        }
    }

    @Getter
    @Accessors(fluent = true)
    private class SoccerTeam implements Scorer {

        private final Map<SoccerPlayer, Integer> pointMap = new HashMap<>();

        private final Location startLocation;
        private final WorldTiedBoundingBox goalBox;
        private final TeamColorTemplate teamColorTemplate;
        private final Particle.DustOptions dustOptions;

        private SoccerTeam(Location startLocation, WorldTiedBoundingBox goalBox, TeamColorTemplate teamColorTemplate) {
            this.startLocation = startLocation;
            this.goalBox = goalBox;
            this.teamColorTemplate = teamColorTemplate;
            this.dustOptions = new Particle.DustOptions(Color.fromRGB(teamColorTemplate.color()), 1.2f);
        }

        public boolean goalBoxOverlaps(Entity entity) {
            return goalBox.contains(entity.getWorld(), entity.getBoundingBox());
        }

        public void scoreGoal(SulfurCubeSoccerBall ball, SoccerPlayer scorer) {
            sendAudienceMessage(teamColorTemplate.formatted(scorer.getEventPlayer().getName() + " scored! <dark_gray>(+1)"));
            scoreboard.addScore(scorer.team(), 1);

            // capture where the ball is before we remove it
            Location goalLocation = ball.activeLocation().clone();

            // kill the sulfur cube and have it explode into a bunch of dust particles
            ball.kill();

            ParticleDisplay particleDisplay = ParticleDisplay.of(Particle.DUST)
                .withColor(new java.awt.Color(teamColorTemplate.color()))
                .withLocation(goalLocation);
            Particles.spikeSphere(6.0, 25.0, 3, 0.4, 0.8, particleDisplay);

            goalLocation.getWorld().playSound(goalLocation, Sound.ITEM_GOAT_HORN_SOUND_2, 2f, 1.25f);

            // blast every entity within a 25 block radius away from the goal
            for (LivingEntity entity : goalLocation.getNearbyLivingEntities(25)) {
                Vector direction = entity.getLocation().toVector().subtract(goalLocation.toVector());
                if (direction.lengthSquared() < 1.0E-6) {
                    direction = new Vector(0, 1, 0); // entity sitting exactly on the goal, just launch it up
                } else {
                    direction.normalize();
                }

                // stronger blast the closer the entity is to the goal
                double distance = entity.getLocation().distance(goalLocation);
                double strength = Math.max(0.5, 2.5 * (1 - distance / 25.0));

                Vector knockback = direction.multiply(strength);
                knockback.setY(knockback.getY() + 0.6); // add a little lift
                entity.setVelocity(knockback);
            }

            // wait 2 seconds before starting a new round
            Executors.delayedSync(goalLocation, 20 * 2, () -> {
                if (scoreboard.getScore(scorer.team()) >= 3) {
                    sendAudienceMessage("<red>Game over!");
                    sendAudienceMessage(teamColorTemplate.name() + " has won!");
                    stop();
                } else {
                    countdownStart(3);
                }
            });
        }

        @Override
        public String getName() {
            return this.teamColorTemplate.name();
        }

        public void addPoints(SoccerPlayer soccerPlayer, int amount) {
            // TODO: Ensure only a max of one point can be earned per tick
            this.pointMap.putIfAbsent(soccerPlayer, 0);
            this.pointMap.put(soccerPlayer, this.pointMap.get(soccerPlayer) + amount);
        }
    }


    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    private enum TeamColorTemplate {
        RED("Red Team", 0x8665C7, "<#8665C7>%s</#8665C7>"),
        BLUE("Blue Team", 0xf4d5e0, "<#f4d5e0>%s</#f4d5e0>");

        private final String name;
        private final int color;
        private final String messageFormat;

        public String formatted(String message) {
            return String.format(this.messageFormat, message);
        }
    }
}
