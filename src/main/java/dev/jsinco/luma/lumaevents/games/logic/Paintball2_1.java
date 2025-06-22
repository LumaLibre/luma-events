package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.obj.Sphere;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO:
//  - Need to calculate scores and display winning team
//  - Handle tokens
//  - Test
// - Handle players killing each other/respawning (DONE!)
public final class Paintball2_1 extends InventoryUnifiedMinigame {

    private static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER, Material.AIR, Material.CAVE_AIR, Material.LADDER);
    private static final List<Material> PAINTABLE_WHITELIST = List.of(
            Material.WAXED_WEATHERED_CUT_COPPER, Material.POLISHED_BASALT, Material.POLISHED_ANDESITE,
            Material.STONE, Material.OAK_PLANKS, Material.POLISHED_BLACKSTONE, Material.CHISELED_DEEPSLATE,
            Material.BLACK_TERRACOTTA, Material.STRIPPED_MANGROVE_LOG, Material.YELLOW_CONCRETE,
            Material.WAXED_OXIDIZED_COPPER, Material.SMOOTH_STONE, Material.NETHER_BRICKS, Material.LIGHT_GRAY_TERRACOTTA,
            Material.STRIPPED_BAMBOO_BLOCK
    );
    private static final ItemStack PAINTBALL = new ItemStack(Material.SNOWBALL);

    private final Paintball2_1Definition def;
    private final ConcurrentHashMap<Location, PaintballTeam> paintedLocations;
    private CountdownBossBar countdownBossBar;
    private List<PaintballTeam> paintballTeams;

    public Paintball2_1(Paintball2_1Definition def) {
        super("Paintball 2.1", "Cover as much area as possible.", 120000, 20, true, true);
        this.def = def;
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.paintedLocations = new ConcurrentHashMap<>();

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
                new PaintballTeam(this.participants.subList(0, middle), colorKit.team1, def.getTeam1SpawnLocation()),
                new PaintballTeam(this.participants.subList(middle, this.participants.size()), colorKit.team2, def.getTeam2SpawnLocation())
        );


        this.countdownBossBar = CountdownBossBar.builder()
                .title("<dark_purple><b>Time Remaining</b><gray>:</gray> <b>%s</b></dark_purple>")
                .color(BossBar.Color.PURPLE)
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
        ).forEach(p -> p.getPlayer().teleportAsync(def.getSpawnLocation()));
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
        player.teleportAsync(def.getSpawnLocation());
        return true;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        this.ensureNotIllegal();
        if (!this.boundingBox.contains(event.getLocation()) || !(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        event.setCancelled(true);
        shooter.launchProjectile(Snowball.class);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player player = event.getPlayer();
        if (!boundingBox.contains(player) || event.getDamageSource().getCausingEntity() == null || !(event.getDamageSource().getCausingEntity() instanceof Player shooter)) {
            return; // Player is not in the bounding box, ignore
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        EventPlayer shooterEventPlayer = EventPlayerManager.getByUUID(shooter.getUniqueId());
        if (!this.participants.contains(eventPlayer)) {
            eventPlayer.sendMessage("You are not participating in this minigame.");
            return;
        }

        PaintballTeam paintballTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(eventPlayer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player is not a member of any team."));

        // Respawn the player at their team's spawn point
        event.setCancelled(true);
        event.setReviveHealth(10.0);
        event.setDeathSound(Sound.ITEM_TOTEM_USE);
        player.teleportAsync(paintballTeam.getSpawnPoint().toCenterLocation());
        this.paint(player.getLocation().add(0, -1, 0), paintballTeam, shooterEventPlayer, 3);
    }


    @EventHandler
    public void onProjectileHitInBoundingBox(ProjectileHitEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(shooter.getUniqueId());

        if (!this.participants.contains(eventPlayer)) {
            eventPlayer.sendMessage("You are not participating in this minigame.");
            return;
        }

        Entity hitEntity = event.getHitEntity();
        Block hitBlock = event.getHitBlock();

        //System.out.println("test1: " + hitEntity != null && boundingBox.contains(hitEntity) && hitEntity instanceof Player hitPlayer);
        if (hitEntity != null && boundingBox.contains(hitEntity) && hitEntity instanceof Player hitPlayer) {
            // Check if the hit entity is standing on a block painted by the other team
            shooter.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitPlayer(hitPlayer, eventPlayer));
        } else if (hitBlock != null && boundingBox.contains(hitBlock.getLocation())) {
            shooter.playSound(hitBlock.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitBlock(event.getHitBlock(), eventPlayer));
        }

    }

    private void handleProjectileHitPlayer(Player hitPlayer, EventPlayer shooter) {
        Player shooterPlayer = shooter.getPlayer();
        if (shooterPlayer == null || !boundingBox.contains(hitPlayer.getLocation())) {
            return; // Shooter is not in the bounding box or hit player is not in the bounding box
        }
        hitPlayer.damage(4.0, shooterPlayer);
    }

    private void handleProjectileHitBlock(Block blockHit, EventPlayer shooter) {
        if (BLACKLISTED_MATERIALS.contains(blockHit.getType())) {
            return;
        }


        PaintballTeam paintballTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(shooter))
                .findFirst()
                .orElseThrow();


        this.paint(blockHit.getLocation(), paintballTeam, shooter, 1);

        // TODO: Replace
//        if (scoreboard.getTempScore(shooter) >= 300) {
//            scoreboard.resetTempScore(shooter);
//            Util.giveTokens(shooter.getPlayer(), 1);
//        }
    }

    public void paint(Location blockHit, PaintballTeam paintballTeam, EventPlayer shooter, int size) {
        if (size == 1) {
            // Paint a single block
            this.paintOnBlock(blockHit.getBlock(), paintballTeam, shooter);
            return;
        }
        Sphere sphere = new Sphere(blockHit, size, 14);

        // Check if the sphere is already painted by the same team
        for (Block block : sphere.getSphereFast()) {
           this.paintOnBlock(block, paintballTeam, shooter);
        }


    }

    private void paintOnBlock(Block block, PaintballTeam paintballTeam, EventPlayer shooter) {
        if (!PAINTABLE_WHITELIST.contains(block.getType())) {
            return; // Do not paint if the block is not in the whitelist
        }

        PaintballTeam existingPainter = paintedLocations.get(block.getLocation());
        if (existingPainter != null && existingPainter.equals(paintballTeam)) {
            // Resend the block change to the player
            Player player = shooter.getPlayer();
            if (player != null) {
                player.sendBlockChange(block.getLocation(), paintballTeam.getBlockData());
            }
            return; // Already painted by the same team
        }

        Location loc = block.getLocation().clone();
        paintedLocations.put(loc, paintballTeam);
        paintballTeam.addScore(1, shooter);

        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player != null) {
                player.sendBlockChange(loc, paintballTeam.getBlockData());
            }
        }
    }


    @Getter
    @Setter
    public static class PaintballTeam {
        private final Map<EventPlayer, Integer> scoreMap;
        private final Material material;
        private final BlockData blockData;
        private final Location spawnPoint;


        public PaintballTeam(List<EventPlayer> members, Material material, Location spawnPoint) {
            this.scoreMap = new HashMap<>();
            for (EventPlayer member : members) {
                this.scoreMap.put(member, 0);
            }
            this.material = material;
            this.blockData = material.createBlockData();
            this.spawnPoint = spawnPoint;
        }

        public boolean isMember(EventPlayer player) {
            return scoreMap.keySet().stream().anyMatch(member -> member.getUuid().equals(player.getUuid()));
        }

        public boolean isMember(Player player) {
            return scoreMap.keySet().stream().anyMatch(member -> member.getUuid().equals(player.getUniqueId()));
        }

        public void addScore(int points, EventPlayer player) {
            if (!scoreMap.containsKey(player)) {
                throw new IllegalArgumentException("Player is not a member of this team.");
            }
            scoreMap.put(player, scoreMap.get(player) + points);
        }
    }



    @Getter
    @AllArgsConstructor
    public enum PaintballColorKit {

        DEFAULT(Material.RED_WOOL, Material.LIME_WOOL),
        A(Material.BLUE_WOOL, Material.ORANGE_WOOL),
        B(Material.ORANGE_WOOL, Material.PURPLE_WOOL),
        C(Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL),
        D(Material.RED_WOOL, Material.ORANGE_WOOL),
        E(Material.LIGHT_BLUE_WOOL, Material.PINK_WOOL);

        private final Material team1;
        private final Material team2;
    }
}
