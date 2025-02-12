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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public non-sealed class BoatRace extends Minigame {

    private static final Random RANDOM = new Random();

    private final Set<BoatRaceCheckpoint> checkpoints;
    private final Set<BoatRacePlayer> racers;
    private final Location spawnLocation;
    private final Location startLocation;
    private final MinigameScoreboard scoreboard;
    private CountdownBossBar countdownBossBar;

    public BoatRace(BoatRaceDefinition def) {
        super("BoatRace", MinigameConstants.BOATRACE_DESC, 90000L, 30, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.checkpoints = new HashSet<>();
        this.racers = new HashSet<>();
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();
        this.scoreboard = new MinigameScoreboard(100);

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, checkpoints.size())));
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.getParticipants()) {
            Player player = participant.getPlayer();
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

            Player player = racer.getEventPlayer().getPlayer();
            player.sendActionBar(Util.color(
                    "<green>Checkpoint: <gold>" + racer.getCheckpointsAchieved().size() + "<green> / <gold>" + this.checkpoints.size()
            ));
        }
    }

    @Override
    protected void handleStop() {
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            this.boundingBox.getEntities(Boat.class).forEach(Boat::remove);
        });
        scoreboard.handleGameEnd(this.participants, this.audience, () -> {
            this.participants.forEach(p -> p.getPlayer().teleportAsync(this.spawnLocation));
            // TODO: make this shared across all minigames
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Race Over</b></red>")
                    .seconds(15)
                    .callback(() -> {
                        // TODO: Teleport players to spawn
                        this.audience.sendMessage(Component.text("game has concluded"));
                    })
                    .build()
                    .start();
        });
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        player.getPlayer().teleportAsync(this.spawnLocation);
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

        if (racer.isReturningToCheckpoint()) {
            racer.setReturningToCheckpoint(false);
        } else if (!racer.isFinished()) {
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
                    .filter(cp -> cp.contains(loc))
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
                eplayer.sendMessage("Checkpoint reached! <gold>+100 <gray>points");
                scoreboard.addScore(eplayer, 1);

                if (racer.finish(this.checkpoints.size())) { // ehh
                    countdownBossBar.getBossBar().removeViewer(event.getPlayer());
                    eplayer.sendTitle("<green>Finished!", "<gray>You placed <gold>#" + scoreboard.getPosition(eplayer));
                    event.getPlayer().teleportAsync(this.spawnLocation);
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

}
