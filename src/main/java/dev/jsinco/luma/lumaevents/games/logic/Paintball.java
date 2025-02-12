package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.shapes.Sphere;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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

    private final Location spawnPoint;
    private final MinigameScoreboard scoreboard;

    private final List<EncapsulatedPaintballTeam> encapsulatedPaintballTeams = List.of(
            new EncapsulatedPaintballTeam(EventTeamType.ROSETHORN, Material.RED_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.SWEETHEARTS, Material.LIME_WOOL),
            new EncapsulatedPaintballTeam(EventTeamType.HEARTBREAKERS, Material.CYAN_WOOL)
    );

    public Paintball(MinigameDefinition def) {
        super("Paintball", MinigameConstants.PAINTBALL_DESC, 30000L, 30, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnPoint = def.getSpawnLocation();
        this.scoreboard = new MinigameScoreboard(1);
    }

    @Override
    protected void handleStart() {
        CountdownBossBar.builder()
                .title("<yellow><b>Time Remaining</b><gray>:</gray> <b>%s</b></yellow>")
                .color(BossBar.Color.YELLOW)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build()
                .start();
    }

    @Override
    protected void handleStop() {
        scoreboard.handleGameEnd(this.participants, this.audience, () -> {
            // TODO: Teleport players to spawn
            this.audience.sendMessage(Component.text("game has concluded"));
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        audience.sendActionBar(Util.color("<yellow>Click to shoot!"));
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        bukkitPlayer.teleportAsync(this.spawnPoint);
    }

    @EventHandler
    public void onPlayerClickInBoundingBox(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        if (!boundingBox.contains(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        player.launchProjectile(Snowball.class);
    }

    @EventHandler
    public void onProjectileHitInBoundingBox(ProjectileHitEvent event) {
        this.ensureNotIllegal();
        if (!boundingBox.contains(event.getEntity()) || event.getHitBlock() == null) {
            return;
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
        scoreboard.addScore(shooter, 1);

        Sphere sphere = new Sphere(blockHit.getLocation(), 3, 5);

        for (EventPlayer player : this.participants) {
            for (Block block : sphere.getSphere().stream().filter(b -> !BLACKLISTED_MATERIALS.contains(b.getType())).toList()) {
                Player bukkitPlayer = player.getPlayer();
                bukkitPlayer.sendBlockChange(block.getLocation(), encapsulatedPaintballTeam.getBlockData());
            }
        }

        // TODO: Tokens
        String name = shooter.getPlayer().getName();
        if (scoreboard.getScore(shooter) % 200 == 0) {
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
