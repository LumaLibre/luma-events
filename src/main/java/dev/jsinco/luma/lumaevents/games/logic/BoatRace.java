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
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public non-sealed class BoatRace extends Minigame {

    private static final Random RANDOM = new Random();

    private final Set<BoatRaceCheckpoint> checkpoints;
    private final Set<BoatRacePlayer> racers;
    private final Location spawnLocation;
    private final Location startLocation;
    private final MinigameScoreboard scoreboard;

    public BoatRace(BoatRaceDefinition def) {
        super("BoatRace", MinigameConstants.BOATRACE_DESC, 30000L, 30, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.checkpoints = new HashSet<>();
        this.racers = new HashSet<>();
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();
        this.scoreboard = new MinigameScoreboard(100);

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, UUID.randomUUID().toString())));
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
                Util.color("<green>Go!"),
                Component.empty()
        ));

        // Start countdown
        CountdownBossBar.builder()
                .audience(this.audience)
                .seconds(5)
                .color(BossBar.Color.GREEN)
                .title("<green>Get Ready...")
                .callback(() -> {

                })
                .build()
                .start();
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
        this.audience.sendMessage(Component.text("game has ended"));
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
        } else {
            racer.getBoat().remove();
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
        if (racer != null && !racer.isFinished()) {
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
                racer.finish(this.checkpoints.size()); // Ehh
                EventPlayer eplayer = racer.getEventPlayer();
                eplayer.sendMessage("Checkpoint achieved! <gold>(+100 <gray>points<gold>)");
                scoreboard.addScore(eplayer, 1);
            }
        });
    }

}
