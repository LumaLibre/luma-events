package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.games.Scoreboard;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.AcaciaBoat;
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
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// TODO:
//  - Debug
//  - Test
public class BoatRace2 extends Minigame {

    private static final String FINISH_LINE_IDENTIFIER = "finish";

    private static final int MAX_LAPS = 3;

    private final Set<BoatRaceCheckpoint> checkpoints;
    private final Set<BoatRacePlayer> racers;
    private final Location spawnLocation;
    private final Location startLocation;
    private final Scoreboard<EventPlayer> scoreboard;


    private CountdownBossBar countdownBossBar;
    private int basePoints;

    public BoatRace2(BoatRaceDefinition def) {
        super("Boat Race 2", "Place as high as you can!", 300000L, 10, true, false, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.checkpoints = new HashSet<>();
        this.racers = new HashSet<>();
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.startLocation = def.getStartLocation().toCenterLocation();
        this.scoreboard = new Scoreboard<>();

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, UUID.randomUUID().toString())));
        this.checkpoints.add(
                new BoatRaceCheckpoint(WorldTiedBoundingBox.of(def.getFinishLine().getLoc1(), def.getFinishLine().getLoc2()), FINISH_LINE_IDENTIFIER)
        );
    }

    @Override
    protected void handleStart() {
        this.cleanBoats();
        this.basePoints = this.participants.size() + 1;
        this.scoreboard.addScorers(this.participants);
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                continue;
            }
            Location loc = this.startLocation.clone().add(RANDOM.nextInt(5), 0, RANDOM.nextInt(5));
            player.teleportAsync(loc);

            // Synchronize
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                Boat boat = player.getWorld().spawn(loc, Util.getRandom(BoatRaceBoatType.values()).getBoatType());
                boat.addPassenger(player);

                BoatRacePlayer racer = BoatRacePlayer.of(participant, boat);
                this.racers.add(racer);
            });
        }

        this.audience.showTitle(Title.title(
                Util.color("<gold>↑"),
                Util.color("<green>Go!")
        ));

        // Start countdown
        this.countdownBossBar = CountdownBossBar.builder()
                .audience(Audience.audience()) // Empty audience
                .miliseconds(this.getDuration())
                .color(BossBar.Color.WHITE)
                .title(":)")
                .build();
        this.countdownBossBar.start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        for (BoatRacePlayer racer : this.racers) {
            if (racer.isFinished()) continue;

            EventPlayer player = racer.getEventPlayer();
            player.sendActionBar("<b><green>Points: " + this.scoreboard.getScore(player) + " <gold>Position: #" + this.position(racer) + " <gray>(" + this.countdownBossBar.secondsRemaining() + "s left)");
        }
        this.tryEndIfNoMoreRacers();
    }

    @Override
    protected void handleStop() {
        for (BoatRacePlayer racer : this.racers) {
            if (!racer.isFinished()) {
                racer.finish();
            }
        }

        this.cleanBoats();

        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }
        scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.stream().filter(player -> player.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(this.spawnLocation));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Game Over</b></red>")
                    .seconds(15)
                    .callback(() -> this.boundingBox.getPlayers().forEach(player -> {
                                player.teleportAsync(this.getGameDropOffLocation());
                                Util.sendMsg(player, "This minigame has concluded.");
                            }
                    ))
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

            boolean completedLap = racer.checkIfCompletedLap(this.checkpoints, true);

            if (!checkpoint.isFinishLine() && !completedLap) {
                racer.addCheckpoint(checkpoint, this.checkpoints);
            }
            EventPlayer eplayer = racer.getEventPlayer();

            Integer position = this.position(racer);


            // Need to check if the racer has completed some laps beforehand to prevent
            // completing a lap at the beginning of the race
            if (checkpoint.isFinishLine() && completedLap) {
                racer.incrementLap(this.checkpoints);
                if (position != null) {
                    int points = this.basePoints - position;
                    this.scoreboard.addScore(racer.getEventPlayer(), points);
                }

                if (racer.getLap() < MAX_LAPS) {
                    eplayer.sendTitle("<green>Lap Completed!", "<gray>You have completed <gold>Lap " + racer.getLap() + "</gray>");
                }
            }


            if (racer.getLap() >= MAX_LAPS) {
                Player bukkitPlayer = event.getPlayer();
                if (this.countdownBossBar != null) {
                    this.countdownBossBar.getBossBar().removeViewer(bukkitPlayer);
                }
                eplayer.sendTitle("<green>Finished!", "<gray>You placed <gold>#" + position);
                Util.sendMsg(this.audience, "<gold>"+bukkitPlayer.getName()+"</gold>"+ " has finished in <gold>#" + position + "</gold> place!");
                racer.finish();
                event.getPlayer().teleportAsync(this.spawnLocation);
                this.tryEndIfNoMoreRacers();
            }

            // TODO:
//                if (RANDOM.nextInt(100) <= 35) {
//                    Util.giveTokens(event.getPlayer(), 1);
//                }
        });
    }


    @Nullable
    private Integer position(BoatRacePlayer player) {
        List<BoatRacePlayer> sortedRacers = getSortedRacers();
        if (!sortedRacers.contains(player)) {
            return null; // Player not found in the sorted list
        }
        return sortedRacers.indexOf(player) + 1; // +1 to convert from index to position
    }

    private List<BoatRacePlayer> getSortedRacers() {
        Comparator<BoatRacePlayer> comparator = Comparator
                .comparingInt(BoatRacePlayer::getLap)
                .thenComparingInt(r -> r.getCheckpoints().size()).reversed()
                .thenComparing(BoatRacePlayer::isFinished, Comparator.reverseOrder())
                .thenComparingDouble(player -> {
                    Player bukkitPlayer = player.getEventPlayer().getPlayer();
                    if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                        return Double.MAX_VALUE; // If player is offline, place them at the end
                    }
                    Location nextCheckpointLoc = getNextCheckpoint(player).getCenterLocation(); // Implement this
                    return bukkitPlayer.getLocation().distance(nextCheckpointLoc);
                });

        return this.racers.stream()
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
        if (Bukkit.isPrimaryThread()) {
            this.boundingBox.getEntities(Boat.class).forEach(Boat::remove);
        } else {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> this.boundingBox.getEntities(Boat.class).forEach(Boat::remove));
        }
    }


    @Getter
    @Setter
    public static class BoatRacePlayer {

        private final EventPlayer eventPlayer;
        private final Map<BoatRaceCheckpoint, Integer> checkpoints;
        private final Boat boat;
        private final BossBar bossBar;

        private int lap = 0;
        private boolean finished = false;

        public BoatRacePlayer(EventPlayer eventPlayer, Boat boat) {
            this.eventPlayer = eventPlayer;
            this.checkpoints = new LinkedHashMap<>();
            this.boat = boat;
            this.bossBar = BossBar.bossBar(
                    Util.color("<b>Lap: " + (this.lap + 1) + "/" + MAX_LAPS),
                    0.0f,
                    BossBar.Color.WHITE,
                    BossBar.Overlay.NOTCHED_12
            );

            this.bossBar.addViewer(Objects.requireNonNull(eventPlayer.getPlayer()));
        }

        public boolean checkIfCompletedLap(Set<BoatRaceCheckpoint> trackCheckpoints, boolean ignoreFinish) {
            return checkIfCompletedLap(trackCheckpoints, this.lap, ignoreFinish);
        }

        public boolean checkIfCompletedLap(Set<BoatRaceCheckpoint> trackCheckpoints, int lap, boolean ignoreFinish) {
            if (this.finished) {
                return true; // Already finished
            }

            Set<BoatRaceCheckpoint> setCopy = new HashSet<>(trackCheckpoints);
            // Remove finish line if ignoreFinish is true
            if (ignoreFinish) {
                setCopy.removeIf(BoatRaceCheckpoint::isFinishLine);
            }

            // Check if all checkpoints for lap are present
            for (BoatRaceCheckpoint checkpoint : setCopy) {
                if (!this.checkpoints.containsKey(checkpoint)) {
                    return false; // Missing checkpoint
                }
                int currentValueForCheckpoint = this.checkpoints.get(checkpoint);
                if (currentValueForCheckpoint < lap) {
                    return false;
                }
            }
            return true;
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
            this.bossBar.name(Util.color("<b>Lap: " + (this.lap + 1) + "/" + MAX_LAPS));
        }

        public void addCheckpoint(BoatRaceCheckpoint checkpoint, Set<BoatRaceCheckpoint> trackCheckpoints) {
            checkpoints.put(checkpoint, this.lap);
            this.updateBossBar(trackCheckpoints);
        }

        public void incrementLap(Set<BoatRaceCheckpoint> trackCheckpoints) {
            this.lap++;
            this.updateBossBar(trackCheckpoints);
            Bukkit.getAsyncScheduler().runDelayed(EventMain.getInstance(), (task) -> {
                if (this.bossBar.progress() > 0.9f) {
                    this.bossBar.progress(0.0f); // Reset progress for the new lap
                }
            }, 1, TimeUnit.SECONDS);
        }

        public void finish() {
            this.finished = true;
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

        public static BoatRacePlayer of(EventPlayer eventPlayer, Boat boat) {
            return new BoatRacePlayer(eventPlayer, boat);
        }
    }


    @ToString
    @Getter
    public static class BoatRaceCheckpoint extends WorldTiedBoundingBox {

        private final String identifier;

        public BoatRaceCheckpoint(WorldTiedBoundingBox boundingBox, String identifier) {
            super(boundingBox.getWorld(), boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
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
        PALE_OAK(PaleOakBoat.class);

        private final Class<? extends Boat> boatType;

        BoatRaceBoatType(Class<? extends Boat> boatType) {
            this.boatType = boatType;
        }
    }
}
