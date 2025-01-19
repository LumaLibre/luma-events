package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.List;

public non-sealed class Paintball extends Minigame {

    private static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER);

    private final WorldTiedBoundingBox boundingBox;
    private final Location spawnPoint;

    private final List<EncapsulatedPaintballTeam> encapsulatedPaintballTeams = List.of(
            new EncapsulatedPaintballTeam(EventTeamType.PEEP_PLUSHY, Material.YELLOW_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.LOVERS, Material.RED_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.LONERS, Material.BLUE_WOOL)
    );

    public Paintball(Location loc1, Location loc2, Location spawnPoint) {
        super("Paintball", MinigameConstants.PAINTBALL_DESC, 30000L, 600);
        this.boundingBox = WorldTiedBoundingBox.of(loc1, loc2);
        this.spawnPoint = spawnPoint;
    }

    @Override
    protected void handleStart() {
        for (EventPlayer player : this.participants) {
            Player bukkitPlayer = player.getPlayer();
            bukkitPlayer.teleportAsync(this.spawnPoint);
            player.sendMessage("Game has started");
        }

    }

    @Override
    protected void handleStop() {
        for (EventPlayer player : this.participants) {
            Player bukkitPlayer = player.getPlayer();
            bukkitPlayer.teleportAsync(this.spawnPoint);
            player.sendMessage("Game has ended");
        }
    }

    @Override
    protected void onRunnable(long timeLeft) {
        for (EventPlayer player : this.participants) {
            player.sendMessage("Time left: " + timeLeft);
        }
    }

    @EventHandler
    public void onProjectileHitInBoundingBox(ProjectileHitEvent event) {
        if (!boundingBox.contains(event.getEntity()) || event.getHitBlock() == null) {
            return;
        } else if (!this.active) {
            throw new GameComponentIllegallyActive("Still listening for projectile hits when game is not active. @" + this.hashCode());
        }
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(shooter.getUniqueId());
        if (!this.participants.contains(eventPlayer)) {
            eventPlayer.sendMessage("You are not participating in this minigame");
            return;
        }

        shooter.playSound(event.getHitBlock().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        handleProjectileHitBlock(event.getHitBlock(), eventPlayer);
    }


    private void handleProjectileHitBlock(Block blockHit, EventPlayer shooter) {
        if (BLACKLISTED_MATERIALS.contains(blockHit.getType())) {
            return;
        }

        EncapsulatedPaintballTeam encapsulatedPaintballTeam = this.encapsulatedPaintballTeams.stream()
                .filter(team -> team.getTeamType().equals(shooter.getTeamType()))
                .findFirst()
                .orElseThrow();
        encapsulatedPaintballTeam.addAreaCovered();
        shooter.addPoints(1);

        for (EventPlayer player : this.participants) {
            Player bukkitPlayer = player.getPlayer();
            bukkitPlayer.sendBlockChange(blockHit.getLocation(),
                    encapsulatedPaintballTeam.getMaterial().createBlockData());
        }
    }


    @Getter
    @Setter
    public static class EncapsulatedPaintballTeam {
        private final EventTeamType teamType;
        private final Material material;
        private int areaCovered = 0;

        public EncapsulatedPaintballTeam(EventTeamType teamType, Material material) {
            this.teamType = teamType;
            this.material = material;
        }

        public void addAreaCovered() {
            this.areaCovered++;
        }
    }
}
