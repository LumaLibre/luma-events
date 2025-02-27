package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.enums.minigame.PaintballColorKit;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.PaintballSphere;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public non-sealed class Paintball extends Minigame {

    public static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER, Material.AIR, Material.CAVE_AIR);

    private final Location spawnPoint;
    private final MinigameScoreboard scoreboard;
    private CountdownBossBar countdownBossBar;

    private final List<EncapsulatedPaintballTeam> encapsulatedPaintballTeams;
    private final ConcurrentLinkedQueue<PaintballSphere> painted;
    // There are probably better methods to this, but I'm in a hurry
    private final Map<UUID, Integer> cpsMap;

    public Paintball(MinigameDefinition def) {
        super("Paintball", MinigameConstants.PAINTBALL_DESC, MinigameConstants.PAINTBALL_DURATION, 20, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.scoreboard = new MinigameScoreboard(1);
        this.painted = new ConcurrentLinkedQueue<>();
        this.cpsMap = new HashMap<>();

        PaintballColorKit colorKit = Util.getRandFromList(PaintballColorKit.values());
        this.encapsulatedPaintballTeams = List.of(
                new EncapsulatedPaintballTeam(EventTeamType.ROSETHORN, colorKit.getRosethorn()),
                new EncapsulatedPaintballTeam(EventTeamType.SWEETHEARTS, colorKit.getSweethearts()),
                new EncapsulatedPaintballTeam(EventTeamType.HEARTBREAKERS, colorKit.getHeartbreakers())
        );
    }

    @Override
    protected void handleStart() {
        countdownBossBar = CountdownBossBar.builder()
                .title("<yellow><b>Time Remaining</b><gray>:</gray> <b>%s</b></yellow>")
                .color(BossBar.Color.YELLOW)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        countdownBossBar.start();
    }

    @Override
    protected void handleStop() {
        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }
        scoreboard.handleGameEnd(this.participants, this.audience, () -> {
            this.participants.stream().filter(
                    p -> p.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(this.spawnPoint));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.PURPLE)
                    .title("<light_purple><b>Game Over</b></light_purple>")
                    .seconds(15)
                    .callback(() -> this.boundingBox.getPlayers().forEach(player -> {
                        player.teleportAsync(this.getGameDropOffLocation());
                        Util.sendMsg(player, "This minigame has concluded.");
                    }))
                    .build()
                    .start();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        audience.sendActionBar(Util.color("<yellow>Click to shoot!"));
        cpsMap.clear();
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnPoint);
    }

    @EventHandler
    public void onPlayerClickInBoundingBox(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        if (!event.getAction().isLeftClick() || !boundingBox.contains(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        int cps = cpsMap.getOrDefault(player.getUniqueId(), 0);
        if (cps > 9) {
            Util.sendMsg(player, "<dark_red>You are clicking too fast!");
            return;
        }
        cpsMap.put(player.getUniqueId(), cps + 1);

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
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitBlock(event.getHitBlock(), eventPlayer));
    }


    private void handleProjectileHitBlock(Block blockHit, EventPlayer shooter) {
        if (BLACKLISTED_MATERIALS.contains(blockHit.getType())) {
            return;
        }

        EncapsulatedPaintballTeam encapsulatedPaintballTeam = this.encapsulatedPaintballTeams.stream()
                .filter(team -> team.getTeamType().equals(shooter.getTeamType()))
                .findFirst()
                .orElseThrow();


        boolean overlaying = false;
        PaintballSphere sphere = new PaintballSphere(blockHit.getLocation(), shooter.getTeamType());
        for (PaintballSphere otherSphere : painted) {
            if (!otherSphere.isNearCenter(blockHit.getLocation()) ) {
                continue;
            }

            if (otherSphere.getPainter() == shooter.getTeamType()) {
                overlaying = true;
            } else {
                // change the sphere painter to the shooter's team
                otherSphere.setPainter(shooter.getTeamType());
            }
            break;
        }

        int p = overlaying ? 1 : 3;
        scoreboard.addScore(shooter, p);
        scoreboard.addTempScore(shooter, p);

        sphere.paint(this.participants, encapsulatedPaintballTeam.getBlockData());
        painted.add(sphere);

        // if the list's size is over 500, we start truncating
        if (painted.size() > 500) {
            // remove first 100 elements
            synchronized (painted) { // have to lock
                int i = 0;
                for (Iterator<PaintballSphere> iterator = painted.iterator();
                     iterator.hasNext() && i < 100; i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }

        if (scoreboard.getTempScore(shooter) >= 300) {
            scoreboard.resetTempScore(shooter);
            Util.giveTokens(shooter.getPlayer(), 1);
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
