package dev.lumas.events.games.logic;

import dev.lumas.lumacore.utility.Logging;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.tokenformula.BoatRace2TokenFormula;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.BoatRace2Definition;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.obj.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.AcaciaBoat;
import org.bukkit.entity.boat.BambooRaft;
import org.bukkit.entity.boat.BirchBoat;
import org.bukkit.entity.boat.CherryBoat;
import org.bukkit.entity.boat.DarkOakBoat;
import org.bukkit.entity.boat.JungleBoat;
import org.bukkit.entity.boat.MangroveBoat;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.entity.boat.PaleOakBoat;
import org.bukkit.entity.boat.SpruceBoat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public final class BoatRace2 extends Minigame {

    private static final String FINISH_LINE_IDENTIFIER = "finish";
    private static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 250, 4, false, false);

    private final Set<BoatRaceCheckpoint> checkpoints;
    private final Set<BoatRacePlayer> racers;
    private final Location spawnLocation;
    private final Location spectateLocation;
    private final Scoreboard<EventPlayer> scoreboard;
    private final int maxLaps;
    private final BoatRace2TokenFormula tokenFormula;

    private final List<Location> spawnPoints;
    private final Location overFlowPoint;


    private int basePoints;

    // Here for caching/performance reasons
    // TODO: Use one list of racers that is kept sorted rather than sorting every tick?
    private List<BoatRacePlayer> sortedRacers;

    public BoatRace2(BoatRace2Definition def) {
        super("Boat Race 2", "Aim for first place!", 300000L, 2, true, false, false, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.checkpoints = new HashSet<>();
        this.racers = new HashSet<>();
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.spectateLocation = def.getSpectateLocation().toCenterLocation();
        this.scoreboard = new Scoreboard<>();
        this.maxLaps = def.getMaxLaps();
        this.tokenFormula = new BoatRace2TokenFormula(false);
        this.spawnPoints = def.getSpawnPoints();
        this.overFlowPoint = def.getOverFlowPoint();

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, UUID.randomUUID().toString())));
        this.checkpoints.add(
                new BoatRaceCheckpoint(WorldTiedBoundingBox.of(def.getFinishLine().getLoc1(), def.getFinishLine().getLoc2()), FINISH_LINE_IDENTIFIER)
        );
    }

    @Override
    protected void handleStart() {
        this.basePoints = this.participants.size() + 1;
        this.scoreboard.addScorers(this.participants);

        int index = 0;

        // Keep yaw/pitch consistent
        float yaw = this.overFlowPoint.getYaw();
        float pitch = this.overFlowPoint.getPitch();

        List<EventPlayer> shuffledParticipants = new ArrayList<>(this.participants);
        Collections.shuffle(shuffledParticipants);

        for (EventPlayer participant : shuffledParticipants) {
            Player player = participant.getPlayer();
            if (player == null) continue;

            Location loc;

            if (index < this.spawnPoints.size()) {
                loc = this.spawnPoints.get(index).toCenterLocation();
                loc.setYaw(yaw);
                loc.setPitch(pitch);
                index++;
            } else {
                loc = this.overFlowPoint.toCenterLocation().add(RANDOM.nextDouble(6), 0, RANDOM.nextDouble(6));
            }

            player.teleportAsync(loc).whenComplete((v, b) -> {
                // Synchronize
                Executors.sync(() -> {
                    BoatRaceBoatType boatType = BoatRaceBoatType.BAMBOO; // Util.getRandom(BoatRaceBoatType.values());
                    Boat boat = player.getWorld().spawn(loc, boatType.getBoatType());
                    boat.addPassenger(player);

                    BoatRacePlayer racer = BoatRacePlayer.of(participant, boat, this.maxLaps);
                    this.racers.add(racer);
                });
            });
        }

        this.audience.showTitle(Title.title(
                Util.color("<gold>↑"),
                Util.color("<green>Go!")
        ));

    }

    @Override
    protected void onRunnable(long timeLeft) {
        this.sortRacers();
        this.tryEndIfNoMoreRacers();
        for (BoatRacePlayer racer : this.racers) {
            if (racer.isFinished()) continue;

            EventPlayer player = racer.getEventPlayer();
            player.sendActionBar("<b><gold>Position: #" + this.position(racer) + " <gray>(" + Util.millisToSecs(timeLeft) + "s left)");
        }

        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null || player.isInsideVehicle() || !isInBoundingBox(player)) continue;
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                player.addPotionEffect(SPEED);
            });
        }
    }

    @Override
    protected void handleStop() {
        for (BoatRacePlayer racer : this.racers) {
            if (!racer.isFinished()) {
                Integer position = this.position(racer);
                if (position != null) {
                    racer.finish(position);
                }
            }
        }

        this.cleanBoats();

        scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.stream().filter(player -> player.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(this.spawnLocation));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Game Over</b></red>")
                    .seconds(15)
                    .callback(() -> {
                        this.participants.forEach(player -> {
                            player.teleportAsync(this.getGameDropOffLocation());
                            player.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer player) {
        BoatRacePlayer racer = this.racers.stream()
                .filter(r -> r.is(player))
                .findFirst()
                .orElse(null);
        if (racer != null) {
            racer.cleanup();
            this.racers.remove(racer);
        }
        return super.removeParticipant(player);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        this.ensureNotIllegal();
        if (!this.boundingBox.contains(event.getExited().getLocation())) {
            return;
        }

        if (!(event.getExited() instanceof Player player)) {
            return;
        }

        BoatRacePlayer racer = this.racers.stream()
                .filter(r -> r.is(player))
                .findFirst()
                .orElse(null);
        if (racer == null) {
            return;
        }

        if (!racer.isFinished()) {
            event.setCancelled(true);
            racer.getEventPlayer().sendMessage("Don't leave your boat!");
        }
    }

    @EventHandler // Needs custom impl of this
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        this.ensureNotIllegal();
        if (!this.boundingBox.contains(event.getFrom())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) {
            return;
        }

        BoatRacePlayer racer = this.racers.stream()
                .filter(r -> r.is(player))
                .findFirst()
                .orElse(null);

        if (racer == null) {
            return;
        }

        if (!racer.isFinished()) {
            event.setCancelled(true);
            racer.getEventPlayer().sendMessage("You can't teleport while you're still racing!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasExplicitlyChangedPosition()) {
            return;
        }

        // Get off the main thread
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> {
            Location loc = event.getTo();
            if (!this.boundingBox.contains(loc)) { // Ensure player is in minigame
                return;
            }
            this.ensureNotIllegal(); // illegal check down here

            BoatRaceCheckpoint checkpoint = this.checkpoints.stream()
                    .filter(cp -> cp.isInWithMarge(loc, 2.5))
                    .findFirst()
                    .orElse(null);

            if (checkpoint == null) {
                return;
            }

            BoatRacePlayer racer = this.racers.stream()
                    .filter(r -> r.is(event.getPlayer()))
                    .findFirst()
                    .orElse(null);

            if (racer == null) {
                return;
            }

            if (racer.hasLappedCheckpoint(checkpoint)) {
                return;
            }

            boolean completedLap = racer.checkIfCompletedLap(this.checkpoints, true, 1);

            if (!checkpoint.isFinishLine() && !completedLap) {
                racer.addCheckpoint(checkpoint, this.checkpoints);
            }
            EventPlayer eventPlayer = racer.getEventPlayer();

            Integer position = this.position(racer);

            if (position == null) {
                return; // Player not found in the sorted list
            }


            // Need to check if the racer has completed some laps beforehand to prevent
            // completing a lap at the beginning of the race
            if (checkpoint.isFinishLine() && completedLap) {
                int formattedLaps = racer.getLap() + 1;
                racer.incrementLap(this.checkpoints);
                int points = this.basePoints - position;
                this.scoreboard.addScore(eventPlayer, points);
                eventPlayer.addPermanentScore(MinigameConstant.BOATRACE2, points);

                if (racer.getLap() < this.maxLaps) {
                    eventPlayer.sendTitle("<green>Lap Completed!", "<white>You finished <gold>lap " + formattedLaps + "</gold>!");
                }

                // Tokens
                tokenFormula.giveTokens(eventPlayer, position);
            }


            if (racer.getLap() >= this.maxLaps) {
                Player bukkitPlayer = event.getPlayer();
                eventPlayer.sendTitle("<green>Finished!", "<gray>You placed <gold>#" + position);
                Util.sendMsg(this.audience, "<gold>"+bukkitPlayer.getName()+"</gold>"+ " has finished in <gold>#" + position + "</gold> place!");
                racer.finish(position);
                event.getPlayer().teleportAsync(this.spectateLocation);
                this.tryEndIfNoMoreRacers();
            }
        });
    }


    @Nullable
    private Integer position(BoatRacePlayer player) {
        if (!this.sortedRacers.contains(player)) {
            return null; // Player not found in the sorted list
        }
        return this.sortedRacers.indexOf(player) + 1; // +1 to convert from index to position
    }

    private void sortRacers() {
        // tie-breaker formula:
        Comparator<BoatRacePlayer> comparator = Comparator
                .comparingInt(BoatRacePlayer::getLap).reversed()
                .thenComparingInt(racer -> {
                    // Only return a value for this if the racer is finished
                    if (racer.isFinished()) {
                        return racer.getFinishedPosition(); // Use finished position if available
                    }
                    return Integer.MAX_VALUE; // If not finished...
                })
                .thenComparingInt(racer -> racer.getRemainingCheckPoints(this.checkpoints))
                .thenComparingDouble(player -> {
                    Player bukkitPlayer = player.getEventPlayer().getPlayer();
                    if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                        return Double.MAX_VALUE; // If player is offline, place them at the end
                    }
                    Location nextCheckpointLoc = getNextCheckpoint(player).getCenterLocation(); // Implement this
                    return bukkitPlayer.getLocation().distance(nextCheckpointLoc); // Sort by distance to next checkpoint
                });
        this.sortedRacers = this.racers.stream()
                .filter(BoatRacePlayer::isOnline)
                .sorted(comparator)
                .toList();
    }


    private BoatRaceCheckpoint getNextCheckpoint(BoatRacePlayer player) {
        for (BoatRaceCheckpoint checkpoint : this.checkpoints) {
            if (!player.hasLappedCheckpoint(checkpoint)) {
                return checkpoint;
            }
        }
        // If all checkpoints are lapped, return finish
        return this.checkpoints.stream()
                .filter(BoatRaceCheckpoint::isFinishLine)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No finish line checkpoint found!"));
    }


    private void tryEndIfNoMoreRacers() {
        if (this.racers.isEmpty()) {
            return;
        }

        List<BoatRacePlayer> stillRacing = this.racers.stream()
                .filter(BoatRacePlayer::isOnline)
                .filter(r -> !r.isFinished())
                .toList();
        if (stillRacing.isEmpty()) {
            this.stop();
        }

    }

    private void cleanBoats() {
        Executors.runSync(() -> {
            this.boundingBox.getEntities(Boat.class).forEach(Boat::remove);
        });
    }


    @Getter
    @Setter
    public static class BoatRacePlayer {

        private final EventPlayer eventPlayer;
        private final Map<BoatRaceCheckpoint, Integer> checkpoints;
        private final Boat boat;
        private final BossBar bossBar;
        private final int maxLaps;

        private int lap = 0;
        private boolean finished = false;
        private int finishedPosition = -1;

        public BoatRacePlayer(EventPlayer eventPlayer, Boat boat, int maxLaps) {
            this.eventPlayer = eventPlayer;
            this.checkpoints = new LinkedHashMap<>();
            this.boat = boat;
            this.bossBar = BossBar.bossBar(
                    Util.color("<b>Lap: " + (this.lap + 1) + "/" + maxLaps),
                    0.0f,
                    BossBar.Color.WHITE,
                    BossBar.Overlay.PROGRESS
            );
            this.maxLaps = maxLaps;
            this.bossBar.addViewer(Objects.requireNonNull(eventPlayer.getPlayer()));
        }

        public boolean checkIfCompletedLap(Set<BoatRaceCheckpoint> trackCheckpoints, boolean ignoreFinish, int allowGrace) {
            return checkIfCompletedLap(trackCheckpoints, this.lap, ignoreFinish, allowGrace);
        }

        public boolean checkIfCompletedLap(Set<BoatRaceCheckpoint> trackCheckpoints, int lap, boolean ignoreFinish, int allowGrace) {
            if (this.finished) {
                return true; // Already finished
            }

            Set<BoatRaceCheckpoint> setCopy = new HashSet<>(trackCheckpoints);
            // Remove finish line if ignoreFinish is true
            if (ignoreFinish) {
                setCopy.removeIf(BoatRaceCheckpoint::isFinishLine);
            }

            // Check if all checkpoints for lap are present
            int missed = 0;
            for (BoatRaceCheckpoint checkpoint : setCopy) {
                Integer currentValueForCheckpoint = this.checkpoints.get(checkpoint);
                if (currentValueForCheckpoint == null || currentValueForCheckpoint < lap) {
                    missed++; // Not lapped this checkpoint in the current lap
                }
            }
            return missed <= allowGrace; // Not enough checkpoints lapped
        }

        public int getRemainingCheckPoints(Set<BoatRaceCheckpoint> trackCheckpoints) {
            if (this.finished) {
                return 0; // Already finished
            }

            int remaining = 0;
            for (BoatRaceCheckpoint checkpoint : trackCheckpoints) {
                Integer lapCount = this.checkpoints.get(checkpoint);
                if (lapCount != null && lapCount == this.lap) {
                    continue; // Already lapped this checkpoint in the current lap
                }
                remaining++;
            }
            return remaining;
        }

        public boolean hasLappedCheckpoint(BoatRaceCheckpoint checkpoint) {
            if (this.finished) {
                return true;
            }
            Integer lapCount = checkpoints.get(checkpoint);
            if (lapCount == null) {
                return false;
            }
            return lapCount == this.lap; // Check if already lapped this checkpoint in the current lap
        }

        public void updateBossBar(Set<BoatRaceCheckpoint> trackCheckpoints) {
            // Update boss bar progress based on the amount of checkpoints remaining in the current lap
            int remainingCheckpoints = this.getRemainingCheckPoints(trackCheckpoints);
            float progress = 1.0f - ((float) remainingCheckpoints / trackCheckpoints.size());
            this.bossBar.progress(progress);
            this.bossBar.name(Util.color("<b>Lap: " + (this.lap + 1) + "/" + this.maxLaps));
        }

        public void addCheckpoint(BoatRaceCheckpoint checkpoint, Set<BoatRaceCheckpoint> trackCheckpoints) {
            checkpoints.put(checkpoint, this.lap);
            this.updateBossBar(trackCheckpoints);
        }

        public void incrementLap(Set<BoatRaceCheckpoint> trackCheckpoints) {
            this.lap++;
            this.updateBossBar(trackCheckpoints);
            Bukkit.getAsyncScheduler().runDelayed(EventMain.getInstance(), (task) -> {
                this.bossBar.color(Util.getRandom(BossBar.Color.values()));
                if (this.bossBar.progress() > 0.9f) {
                    this.bossBar.progress(0.0f); // Reset progress for the new lap
                }
            }, 1, TimeUnit.SECONDS);
        }

        public void finish(int position) {
            this.finished = true;
            this.finishedPosition = position;
            this.cleanup();
        }

        public boolean is(EventPlayer eventPlayer) {
            return this.eventPlayer.equals(eventPlayer);
        }

        public boolean is(Player player) {
            Player bukkitPlayer = this.eventPlayer.getPlayer();
            if (bukkitPlayer == null) {
                return false;
            }
            return bukkitPlayer.equals(player);
        }

        public void cleanup() {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(EventMain.getInstance(), this::cleanup);
                return; // Ensure cleanup is run on the main thread
            }
            Player player = this.eventPlayer.getPlayer();
            if (this.bossBar != null && player != null) {
                this.bossBar.removeViewer(player);
            }
            if (this.boat != null && !this.boat.isDead()) {
                this.boat.remove();
            }
        }

        public boolean isOnline() {
            Player player = eventPlayer.getPlayer();
            if (player == null) {
                return false;
            }
            return player.isOnline();
        }

        public static BoatRacePlayer of(EventPlayer eventPlayer, Boat boat, int maxLaps) {
            return new BoatRacePlayer(eventPlayer, boat, maxLaps);
        }
    }


    @ToString
    @Getter
    public static class BoatRaceCheckpoint extends WorldTiedBoundingBox {

        private final String identifier;

        public BoatRaceCheckpoint(WorldTiedBoundingBox boundingBox, String identifier) {
            super(boundingBox.getWorld(), boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
            if (boundingBox.getMax() == boundingBox.getMin()) {
                Logging.errorLog("Ambiguous bounding box for checkpoint: " + identifier + ". Checkpoints must have a non-zero size!");
                Logging.errorLog("Center location: " + boundingBox.getCenterLocation());
            }
            this.identifier = identifier;
        }

        public boolean isFinishLine() {
            return FINISH_LINE_IDENTIFIER.equals(this.identifier);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            BoatRaceCheckpoint that = (BoatRaceCheckpoint) obj;
            return this.identifier.equals(that.identifier);
        }
    }

    @Getter
    public enum BoatRaceBoatType {

        OAK(OakBoat.class),
        SPRUCE(SpruceBoat.class),
        BIRCH(BirchBoat.class),
        JUNGLE(JungleBoat.class),
        ACACIA(AcaciaBoat.class),
        DARK_OAK(DarkOakBoat.class),
        MANGROVE(MangroveBoat.class),
        CHERRY(CherryBoat.class),
        PALE_OAK(PaleOakBoat.class),
        BAMBOO(BambooRaft.class);

        private final Class<? extends Boat> boatType;

        BoatRaceBoatType(Class<? extends Boat> boatType) {
            this.boatType = boatType;
        }
    }
}
