package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.shapes.Sphere;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public non-sealed class Paintball extends Minigame {

    private static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER, Material.AIR, Material.CAVE_AIR);

    private final WorldTiedBoundingBox boundingBox;
    private final Location spawnPoint;
    private final MinigameScoreboard scoreboard;
    private CountdownBossBar countdownBossBar;

    private final List<EncapsulatedPaintballTeam> encapsulatedPaintballTeams = List.of(
            new EncapsulatedPaintballTeam(EventTeamType.ROSETHORN, Material.RED_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.SWEETHEARTS, Material.LIME_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.HEARTBREAKERS, Material.CYAN_WOOL)
    );

    public Paintball(Location loc1, Location loc2, Location spawnPoint) {
        super("Paintball", MinigameConstants.PAINTBALL_DESC, 30000L, 30, true);
        this.boundingBox = WorldTiedBoundingBox.of(loc1, loc2);
        this.spawnPoint = spawnPoint;
        this.scoreboard = new MinigameScoreboard();
    }

    @Override
    protected void handleStart() {
        audience.showTitle(Title.title(
                Util.color("<yellow>Paintball"),
                Util.color("<red>Cover as much area as possible!")
        ));
        this.audience.sendMessage(Util.color("Game has started"));

        countdownBossBar = CountdownBossBar.builder()
                .title("Time Remaining: %s")
                .color(BossBar.Color.YELLOW)
                .miliseconds(30000L)
                .audience(this.audience)
                .build();
        countdownBossBar.start();
    }

    @Override
    protected void handleStop() {
        EventTeamType winner = scoreboard.getLeadingTeam();
        audience.showTitle(Title.title(
                Util.color("<yellow>Game over"),
                Util.color(winner.getColor() + winner.getFormatted() + " <red>team has won!")
        ));
        for (EventPlayer player : this.participants) {
            player.sendNoPrefixedMessage("<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ");
            player.sendMessage("The " + winner.getTeamWithGradient() + " team <white>has won!");
            player.sendMessage("Total scores<gray>:");
            for (EventTeamType team : scoreboard.getTeamsByScore()) {
                player.sendMessage(
                        team.getTeamWithGradient() + "<gray>: <gold>" + scoreboard.getPoints(team) + " +"
                                + scoreboard.getFinalPositionAdditionalPoints(team) + " additional <gray>points"
                );
            }
            // TODO: Teleport players to spawn
        }

        for (EventTeamType team : scoreboard.getTeamsByScore()) {
            List<EventPlayer> teamParticipants = this.participants.stream()
                    .filter(player -> player.getTeamType().equals(team))
                    .toList();
            scoreboard.distributePoints(teamParticipants, team);
        }

        if (!countdownBossBar.isCancelled()) {
            countdownBossBar.stop(false);
        }
    }

    @Override
    protected void onRunnable(long timeLeft) {
        // Double check if audience member is in bounding box...
        audience.sendActionBar(Util.color("<yellow>Click to shoot!"));
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        bukkitPlayer.teleportAsync(this.spawnPoint);
    }

    @EventHandler
    public void onPlayerClickInBoundingBox(PlayerInteractEvent event) {
        if (!boundingBox.contains(event.getPlayer())) {
            return;
        } else if (!this.active) {
            throw new GameComponentIllegallyActive("Still listening for player clicks when game is not active. @" + this.hashCode());
        }

        Player player = event.getPlayer();
        player.launchProjectile(Snowball.class);
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
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> {
            handleProjectileHitBlock(event.getHitBlock(), eventPlayer);
        });
    }


    private void handleProjectileHitBlock(Block blockHit, EventPlayer shooter) {
        if (BLACKLISTED_MATERIALS.contains(blockHit.getType())) {
            return;
        }

        EncapsulatedPaintballTeam encapsulatedPaintballTeam = this.encapsulatedPaintballTeams.stream()
                .filter(team -> team.getTeamType().equals(shooter.getTeamType()))
                .findFirst()
                .orElseThrow();
        //encapsulatedPaintballTeam.addAreaCovered();
        shooter.addPoints(1);
        scoreboard.addPoints(shooter, 1);

        Sphere sphere = new Sphere(blockHit.getLocation(), 3, 5);

        for (EventPlayer player : this.participants) {
            for (Block block : sphere.getSphere().stream().filter(b -> !BLACKLISTED_MATERIALS.contains(b.getType())).toList()) {
                Player bukkitPlayer = player.getPlayer();
                bukkitPlayer.sendBlockChange(block.getLocation(), encapsulatedPaintballTeam.getBlockData());
            }
        }

        // tokens
        String name = shooter.getPlayer().getName();
        if (scoreboard.getPoints(shooter) % 200 == 0) {
            Util.giveTokens(name, 1);
        }
    }


    @Getter
    @Setter
    public static class EncapsulatedPaintballTeam {
        private final EventTeamType teamType;
        private final BlockData blockData;

        public EncapsulatedPaintballTeam(EventTeamType teamType, Material material) {
            this.teamType = teamType;
            this.blockData = material.createBlockData();
        }
    }
}
