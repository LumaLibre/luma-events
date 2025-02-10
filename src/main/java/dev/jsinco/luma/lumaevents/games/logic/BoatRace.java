package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.obj.minigame.BoatRaceCheckpoint;
import dev.jsinco.luma.lumaevents.obj.minigame.BoatRacePlayer;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public non-sealed class BoatRace extends Minigame {

    private static final Random R = new Random();

    private final Set<BoatRaceCheckpoint> checkpoints = new HashSet<>();
    private final Set<BoatRacePlayer> racers = new HashSet<>();

    private final Location spawnLocation;
    private final Location startLocation;
    //private final MinigameScoreboard scoreboard;

    protected BoatRace(BoatRaceDefinition def) {
        super("BoatRace", MinigameConstants.BOATRACE_DESC, 30000L, 30, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();
        //this.scoreboard = new MinigameScoreboard();

        def.getCheckpoints().stream()
                .map(region -> WorldTiedBoundingBox.of(region.getLoc1(), region.getLoc2()))
                .forEach(box -> this.checkpoints.add(new BoatRaceCheckpoint(box, UUID.randomUUID().toString())));
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.getParticipants()) {
            Player player = participant.getPlayer();
            Location loc = this.startLocation.clone().add(R.nextInt(5), 0, R.nextInt(5));
            player.teleportAsync(loc);

            Boat boat = player.getWorld().spawn(player.getLocation(), Boat.class);
            boat.addPassenger(player);

            BoatRacePlayer racer = BoatRacePlayer.of(participant, boat);
            this.racers.add(racer);
        }

        // TODO: Start countdown
    }

    @Override
    protected void onRunnable(long timeLeft) {

    }

    @Override
    protected void handleStop() {

    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        player.getPlayer().teleportAsync(this.spawnLocation);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!this.isActive()) {
            throw new GameComponentIllegallyActive("Minigame is not active");
        } else if (!this.boundingBox.contains(event.getExited().getLocation())) {
            return;
        }

        EventPlayer eplayer = EventPlayerManager.getByUUID(event.getExited().getUniqueId());
        if (this.getParticipants().contains(eplayer)) {
            event.setCancelled(true);
            eplayer.sendMessage("Don't leave your boat!");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!this.isActive()) { // Ensure active. Should be unregistered if not active
            throw new GameComponentIllegallyActive("Minigame is not active");
        } else if (!this.getBoundingBox().contains(event.getFrom())) { // Ensure player is in minigame
            return;
        }

        Player player = event.getPlayer();
        if (player.isInsideVehicle()) {
            event.setCancelled(true);
            player.sendMessage("You can't leave this minigame while you're still racing!");
        }
    }


    public void onPlayerMove(PlayerMoveEvent event) {

    }
}
