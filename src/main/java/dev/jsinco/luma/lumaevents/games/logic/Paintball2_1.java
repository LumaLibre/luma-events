package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.shapes.Sphere;
import lombok.AllArgsConstructor;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO:
//  - Need to calculate scores and display winning team
//  - Handle tokens
//  - Handle players killing each other/respawning
//  - Test
public final class Paintball2_1 extends InventoryUnifiedMinigame {

    private static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER, Material.AIR, Material.CAVE_AIR);
    private static final ItemStack PAINTBALL = new ItemStack(Material.SNOWBALL);

    private final Location spawnPoint;
    private final ConcurrentLinkedQueue<PaintballArea> painted;
    private CountdownBossBar countdownBossBar;
    private List<PaintballTeam> paintballTeams;

    public Paintball2_1(MinigameDefinition def) {
        super("Paintball 2.1", "Cover as much area as possible.", 120000, 20, true, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.painted = new ConcurrentLinkedQueue<>();

    }

    @Override
    public @Nullable ItemStack defaultItem() {
        return PAINTBALL;
    }

    @Override
    protected void handleStart() {
        // Split participants into 2 teams
        PaintballColorKit colorKit = Util.getRandom(PaintballColorKit.values());
        int middle = this.participants.size() / 2;
        this.paintballTeams = List.of(
                new PaintballTeam(this.participants.subList(0, middle), colorKit.team1),
                new PaintballTeam(this.participants.subList(middle, this.participants.size()), colorKit.team2)
        );


        this.countdownBossBar = CountdownBossBar.builder()
                .title("<yellow><b>Time Remaining</b><gray>:</gray> <b>%s</b></yellow>")
                .color(BossBar.Color.YELLOW)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        this.countdownBossBar.start();
    }

    @Override
    protected void handleStop() {
        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }
        // TODO: Need to calculate scores and display winning team
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
    }

    @Override
    protected void onRunnable(long timeLeft) {
        audience.sendActionBar(Util.color("<yellow>Throw your paintball!"));
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnPoint);
        return true;
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
            eventPlayer.sendMessage("You are not participating in this minigame.");
            return;
        }

        shooter.playSound(event.getHitBlock().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitBlock(event.getHitBlock(), eventPlayer));
    }


    private void handleProjectileHitBlock(Block blockHit, EventPlayer shooter) {
        if (BLACKLISTED_MATERIALS.contains(blockHit.getType())) {
            return;
        }

        PaintballTeam paintballTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(shooter))
                .findFirst()
                .orElseThrow();


        boolean overlaying = false;
        PaintballArea sphere = new PaintballArea(blockHit.getLocation(), paintballTeam);
        for (PaintballArea otherSphere : painted) {
            if (!otherSphere.isNearCenter(blockHit.getLocation()) ) {
                continue;
            }

            if (otherSphere.getPainter() == paintballTeam) {
                overlaying = true;
            } else {
                // change the sphere painter to the shooter's team
                otherSphere.setPainter(paintballTeam);
            }
            break;
        }

        if (overlaying) return;
        paintballTeam.addScore(3, shooter);

        sphere.paint(this.participants, paintballTeam.getBlockData());
        painted.add(sphere);

        // if the list's size is over 500, we start truncating
        if (painted.size() > 500) {
            // remove first 100 elements
            synchronized (painted) { // have to lock
                int i = 0;
                for (Iterator<PaintballArea> iterator = painted.iterator();
                     iterator.hasNext() && i < 100; i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }

        // TODO: Replace
//        if (scoreboard.getTempScore(shooter) >= 300) {
//            scoreboard.resetTempScore(shooter);
//            Util.giveTokens(shooter.getPlayer(), 1);
//        }
    }


    @Getter
    @Setter
    public static class PaintballTeam {
        private final Map<EventPlayer, Integer> scoreMap;
        private final BlockData blockData;


        public PaintballTeam(List<EventPlayer> members, Material material) {
            this.scoreMap = new HashMap<>();
            for (EventPlayer member : members) {
                this.scoreMap.put(member, 0);
            }
            this.blockData = material.createBlockData();
        }

        public List<EventPlayer> members() {
            return scoreMap.keySet().stream().toList();
        }

        public boolean isMember(EventPlayer player) {
            return scoreMap.keySet().stream().anyMatch(member -> member.getUuid().equals(player.getUuid()));
        }

        public void addScore(int points, EventPlayer player) {
            if (!scoreMap.containsKey(player)) {
                throw new IllegalArgumentException("Player is not a member of this team.");
            }
            scoreMap.put(player, scoreMap.get(player) + points);
        }
    }



    @Getter
    @Setter
    public static class PaintballArea extends Sphere {

        private PaintballTeam painter;

        public PaintballArea(Location center, PaintballTeam painter) {
            super(center, 2, 14);
            this.painter = painter;
        }

        public boolean isOverlaying(PaintballArea paintballSphere) {
            return this.isWithinMarge(paintballSphere.getCenter(), paintballSphere.getRadius());
        }

        public boolean isNearCenter(Location location) {
            return this.isWithinMarge(location, -2);
        }

        public void paint(List<EventPlayer> participants, BlockData blockData) {
            players: for (EventPlayer player : participants) {
                for (Block block : this.getSphere().stream().filter(b -> !BLACKLISTED_MATERIALS.contains(b.getType())).toList()) {
                    Player bukkitPlayer = player.getPlayer();
                    if (bukkitPlayer == null) {
                        continue players;
                    }
                    bukkitPlayer.sendBlockChange(block.getLocation(), blockData);
                }
            }
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;
            PaintballArea that = (PaintballArea) object;
            return painter == that.painter;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(painter);
        }
    }


    @Getter
    @AllArgsConstructor
    public enum PaintballColorKit {

        DEFAULT(Material.RED_WOOL, Material.LIME_WOOL),
        A(Material.BLUE_WOOL, Material.ORANGE_WOOL),
        B(Material.YELLOW_WOOL, Material.PURPLE_WOOL),
        C(Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL),
        D(Material.RED_WOOL, Material.ORANGE_WOOL),
        E(Material.LIGHT_BLUE_WOOL, Material.PINK_WOOL);

        private final Material team1;
        private final Material team2;
    }
}
