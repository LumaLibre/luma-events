package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.enums.minigame.BoatRaceBoatType;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.obj.minigame.BoatRaceCheckpoint;
import dev.jsinco.luma.lumaevents.obj.minigame.BoatRacePlayer;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleCollisionEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public non-sealed class BoatRace extends Minigame {

    private static final int POINT_MULTIPLIER = 20;

    private final Set<BoatRaceCheckpoint> checkpoints;
    private final Set<BoatRacePlayer> racers;
    private final Location spawnLocation;
    private final Location startLocation;
    private final MinigameScoreboard scoreboard;
    private CountdownBossBar countdownBossBar;

    public BoatRace(BoatRaceDefinition def) {
        super("Boat Race", MinigameConstants.BOATRACE_DESC, MinigameConstants.BOATRACE_DURATION, 30, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.checkpoints = new HashSet<>();
        this.racers = new HashSet<>();
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.startLocation = def.getStartLocation().toCenterLocation();
        this.scoreboard = new MinigameScoreboard(POINT_MULTIPLIER);

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, UUID.randomUUID().toString())));
    }

    @Override
    protected void handleStart() {
        this.checkpoints.forEach(boatRaceCheckpoint -> boatRaceCheckpoint.setWorth(this.participants.size()));

        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                continue;
            }
            Location loc = this.startLocation.clone().add(RANDOM.nextInt(4), 0, RANDOM.nextInt(4));
            player.teleportAsync(loc);

            // Synchronize
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                Boat boat = player.getWorld().spawn(loc, Util.getRandFromList(BoatRaceBoatType.values()).getBoatType());
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
        countdownBossBar = CountdownBossBar.builder()
                .audience(this.audience)
                .miliseconds(this.getDuration())
                .color(BossBar.Color.PINK)
                .title("<light_purple><b>Time Remaining</b><gray>:</gray> <b>%s</b></light_purple>")
                .build();
        countdownBossBar.start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        for (BoatRacePlayer racer : this.racers) {
            if (racer.isFinished()) {
                continue;
            }

            EventPlayer player = racer.getEventPlayer();
            if (!scoreboard.hasScore(player)) {
                player.sendActionBar(
                        "<green>Checkpoint: <gold>" + racer.getCheckpointsAchieved().size() + "<green> / <gold>" + this.checkpoints.size()
                );
            } else {
                player.sendActionBar(
                        "<green>Checkpoint: <gold>" + racer.getCheckpointsAchieved().size() + "<green> / <gold>" + this.checkpoints.size() +
                                " <gray>| <green>#<gold>" + scoreboard.getPosition(player) + " <gray>(<gold>" + scoreboard.getPoints(player) + "<gray>)"
                );
            }
        }
        this.tryEndIfNoMoreRacers();
    }

    @Override
    protected void handleStop() {
        if (Bukkit.isPrimaryThread()) {
            this.boundingBox.getEntities(Boat.class).forEach(Boat::remove);
        } else {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> this.boundingBox.getEntities(Boat.class).forEach(Boat::remove));
        }
        if (countdownBossBar != null) {
            countdownBossBar.stop(false);
        }
        scoreboard.handleGameEnd(this.participants, this.audience, () -> {
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
    protected void handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
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
        if (racer == null || racer.isReturningToCheckpoint()) {
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

        if (racer == null || racer.isReturningToCheckpoint()) {
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
            this.ensureNotIllegal();

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

            if (!racer.hasAchievedCheckpoint(checkpoint)) {
                racer.addCheckpoint(checkpoint);
                EventPlayer eplayer = racer.getEventPlayer();

                final int checkpointWorth = checkpoint.getWorth();
                checkpoint.setWorth(checkpointWorth - 1);

                scoreboard.addScore(eplayer, checkpointWorth);
                int position = this.getPositionFromCheckpointWorth(checkpointWorth);
                eplayer.sendMessage("Checkpoint reached! <gold>+"+checkpointWorth*POINT_MULTIPLIER+" <gray>points (Use F to teleport back to this checkpoint!)");
                eplayer.sendMessage("You are in <gold>#" + position + "<gray> place");

                if (RANDOM.nextInt(100) <= 22) {
                    Util.giveTokens(event.getPlayer(), 1);
                }

                if (racer.finish(this.checkpoints.size())) { // ehh
                    Player bukkitPlayer = event.getPlayer();
                    if (countdownBossBar != null) {
                        countdownBossBar.getBossBar().removeViewer(bukkitPlayer);
                    }
                    eplayer.sendTitle("<green>Finished!", "<gray>You placed <gold>#" + this.getPositionFromCheckpointWorth(checkpointWorth));
                    Util.sendMsg(this.audience, "<gold>"+bukkitPlayer.getName()+"</gold>"+ " has finished in <gold>#" + position + "</gold> place!");
                    event.getPlayer().teleportAsync(this.spawnLocation);
                    this.tryEndIfNoMoreRacers();
                }
            }
        });
    }

    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        if (!this.boundingBox.contains(event.getPlayer().getLocation()) || !event.getPlayer().isInsideVehicle()) {
            return;
        }

        BoatRacePlayer racer = this.racers.stream()
                .filter(r -> r.is(event.getPlayer()))
                .findFirst()
                .orElse(null);
        if (racer == null) {
            return;
        }

        event.setCancelled(true);
        racer.teleportToLastCheckpoint();
    }

    @EventHandler
    public void onBoatCollide(VehicleEntityCollisionEvent event) {
        this.ensureNotIllegal();
        if (!(event.getVehicle() instanceof Boat) || !this.boundingBox.contains(event.getVehicle().getLocation())) {
            return;
        }
        event.setCancelled(true);
    }


    private int getPositionFromCheckpointWorth(int worth) {
        return (this.participants.size() - worth) + 1;
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

}
