package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition.TeamBedPart;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.games.interfaces.Scorer;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.tokenformula.Paintball2_1TokenFormula;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.BlockFaces;
import dev.jsinco.luma.lumaevents.utility.Couple;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.obj.Sphere;
import dev.jsinco.luma.lumaitems.particles.ParticleDisplay;
import dev.jsinco.luma.lumaitems.particles.Particles;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Test, cleanup
public final class Paintball2_1 extends InventoryUnifiedMinigame {

    private static final List<Material> STANDARD_BLACKLIST = List.of(Material.BARRIER, Material.AIR, Material.CAVE_AIR, Material.LADDER);
    private static final AttributeModifier ATTRIBUTE_MODIFIER = new AttributeModifier(new NamespacedKey(EventMain.getInstance(), "paintball"), -4.5, AttributeModifier.Operation.ADD_NUMBER);
    private static final PotionEffect REGEN = new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false,false);

    private final Paintball2_1Definition def;
    private final ConcurrentHashMap<Location, PaintballTeam> paintedLocations; // TODO: Don't use locations
    private final Scoreboard<PaintballTeam> scoreboard;
    private final List<Material> blacklistedMaterials;
    private final Paintball2_1TokenFormula tokenFormula;
    private CountdownBossBar countdownBossBar;
    private List<PaintballTeam> paintballTeams;

    public Paintball2_1(Paintball2_1Definition def) {
        super("Paintball 2.1", "Cover as much area as possible.", 240000L, 20, true, true);
        this.def = def;
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.paintedLocations = new ConcurrentHashMap<>();
        this.scoreboard = new Scoreboard<>();
        this.blacklistedMaterials = def.getAllBlacklistedBlocks();
        this.blacklistedMaterials.addAll(STANDARD_BLACKLIST);
        this.tokenFormula = new Paintball2_1TokenFormula();
    }


    @Override
    protected void handleStart() {
        // Split participants into 2 teams
        Couple<ColorKit, ColorKit> colorKits = ColorKits.getRandomColorKits();
        int middle = this.participants.size() / 2;
        this.paintballTeams = List.of(
                new PaintballTeam(this.participants.subList(0, middle), colorKits.a(), def.getTeam1SpawnLocation(), def.getTeam1BedParts()),
                new PaintballTeam(this.participants.subList(middle, this.participants.size()), colorKits.b(), def.getTeam2SpawnLocation(), def.getTeam2BedParts())
        );
        this.scoreboard.addScorers(this.paintballTeams);

        for (PaintballTeam team : this.paintballTeams) {
            for (EventPlayer member : team.getMembers()) {
                Player player = member.getPlayer();
                if (player == null) {
                    continue;
                }
                player.teleportAsync(team.getSpawnPoint().toCenterLocation());
                Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                    player.getInventory().setItemInMainHand(team.getPaintball());
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                });
            }
        }


        this.countdownBossBar = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
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
        this.scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.stream().filter(
                    p -> p.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(def.getSpawnLocation()));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Game Over")
                    .seconds(15)
                    .callback(() -> this.boundingBox.getPlayers().forEach(player -> {
                        Location loc = this.getGameDropOffLocation();
                        if (loc != null) {
                            player.teleportAsync(loc);
                        }
                        Util.sendMsg(player, "This minigame has concluded.");

                        if (this.isInBoundingBox(player)) {
                            for (Location location : paintedLocations.keySet()) {
                                Block block = location.getBlock();
                                player.sendBlockChange(location, block.getBlockData());
                            }
                        }
                    }))
                    .build()
                    .start();
        });
    }

    @Override
    protected void handleTokens() {
        PaintballTeam winningTeam = this.scoreboard.getTopScorer();

        for (PaintballTeam team : this.paintballTeams) {
            int position = 1;
            for (var mapEntry : team.membersByScores().entrySet()) {
                EventPlayer member = mapEntry.getKey();
                int score = mapEntry.getValue();
                Couple<Integer, Boolean> couple = Couple.of(position, team.equals(winningTeam));

                tokenFormula.giveTokens(member, couple);
                member.addPermanentScore(MinigameConstant.PAINTBALL2_1, score);
                position++;
            }
        }
    }

    @Override
    protected void onRunnable(long timeLeft) {
        // Determine what % of all paintedLocations are painted by each team and show as:
        // Coverage: <team1> 45% <team2> 55%
        int totalPaintedBlocks = paintedLocations.size();
        if (totalPaintedBlocks == 0) {
            this.audience.sendActionBar(Util.color("<yellow>Throw your paintball!"));
            return;
        }
        int team1PaintedBlocks = (int) paintedLocations.values().stream()
                .filter(team -> team.equals(paintballTeams.getFirst()))
                .count();
        int team2PaintedBlocks = totalPaintedBlocks - team1PaintedBlocks; // Since there are only 2 teams
        double team1Coverage = (double) team1PaintedBlocks / totalPaintedBlocks * 100;
        double team2Coverage = (double) team2PaintedBlocks / totalPaintedBlocks * 100;
        PaintballTeam team1 = this.paintballTeams.getFirst();

        Component breakLine = Util.color(" <white>|</white> ");


        for (PaintballTeam paintballTeam : this.paintballTeams) {
            for (EventPlayer member : paintballTeam.getMembers()) {
                PaintballTeam enemyTeam = paintballTeams.stream().filter(t -> t != paintballTeam).findFirst().orElse(null);
                if (enemyTeam == null) continue;

                double ourCoverage = paintballTeam == team1 ? team1Coverage : team2Coverage;
                double enemyCoverage = enemyTeam == team1 ? team1Coverage : team2Coverage;

                Component ourTeamComp = Component.text("Good Guys " + String.format("%.0f", ourCoverage) + "%")
                        .color(paintballTeam.getTextColor());
                Component enemyTeamComp = Component.text("Bad Guys " + String.format("%.0f", enemyCoverage) + "%")
                        .color(enemyTeam.getTextColor());

                Component actionBar = ourTeamComp.append(breakLine).append(enemyTeamComp).decorate(TextDecoration.BOLD);

                member.sendActionBar(actionBar);
                // regen
                Player player = member.getPlayer();
                if (player != null) {
                    Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> player.addPotionEffect(REGEN));
                }
            }
        }

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        player.teleportAsync(def.getSpawnLocation());
        return true;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        this.ensureNotIllegal();
        if (!this.boundingBox.contains(event.getLocation())) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player shooter) || !(event.getEntity() instanceof Snowball snowball)) {
            return; // Only handle projectiles shot by players
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(shooter.getUniqueId());
        if (!this.participants.contains(eventPlayer)) {
            eventPlayer.sendMessage("You are not participating in this minigame.");
            return;
        }

        PaintballTeam paintballTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(shooter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player is not a member of any team."));
        snowball.setItem(paintballTeam.getSnowballMaterial());
        snowball.setVelocity(snowball.getVelocity().multiply(0.4));
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
                .filter(team -> team.isMember(shooterEventPlayer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player is not a member of any team."));
        PaintballTeam victimTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(eventPlayer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Victim is not a member of any team."));


        this.paint(player.getLocation().add(0, -1, 0), paintballTeam, shooterEventPlayer, 3);

        // LumaItems ParticleLib
        ParticleDisplay particleDisplay = ParticleDisplay.of(Particle.DUST)
                        .withColor(Util.bukkitToAwtColor(paintballTeam.getBlockData().getMapColor()))
                                .withLocation(player.getEyeLocation());
        Particles.spikeSphere(3.0, 10.0, 3, 0.1, 0.8, particleDisplay);

        // Respawn the player at their team's spawn point
        event.setCancelled(true);
        event.setReviveHealth(20.0);
        event.setDeathSound(Sound.ITEM_TOTEM_USE);
        player.teleportAsync(victimTeam.getSpawnPoint().toCenterLocation());
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

        PaintballTeam paintballTeam = this.paintballTeams.stream()
                .filter(team -> team.isMember(shooter))
                .findFirst()
                .orElseThrow();

        // food level
        shooter.getInventory().setItemInMainHand(paintballTeam.getPaintball());
        if (shooter.getFoodLevel() < 20) {
            shooter.setFoodLevel(20);
        }

        Entity hitEntity = event.getHitEntity();
        Block hitBlock = event.getHitBlock();

        if (hitEntity != null && boundingBox.contains(hitEntity) && hitEntity instanceof Player hitPlayer) {
            // Check if the hit entity is standing on a block painted by the other team
            Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitPlayer(hitPlayer, shooter, paintballTeam));
        } else if (hitBlock != null && boundingBox.contains(hitBlock.getLocation())) {
            shooter.playSound(hitBlock.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> handleProjectileHitBlock(hitBlock, paintballTeam, eventPlayer));
        }

    }

    private void handleProjectileHitPlayer(Player hitPlayer, Player shooter, PaintballTeam paintballTeam) {
        PaintballTeam paintballTeam2 = this.paintballTeams.stream()
                .filter(team -> team.isMember(hitPlayer))
                .findFirst()
                .orElseThrow();

        if (!boundingBox.contains(hitPlayer.getLocation()) || paintballTeam == paintballTeam2) {
            return; // Shooter is not in the bounding box or hit player is not in the bounding box or shooter hit their own teammate
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () ->{
            shooter.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            hitPlayer.damage(12.0, shooter);
        });
    }

    private void handleProjectileHitBlock(Block blockHit, PaintballTeam paintballTeam, EventPlayer shooter) {
        if (this.blacklistedMaterials.contains(blockHit.getType())) {
            return;
        }
        this.paint(blockHit.getLocation(), paintballTeam, shooter, 1);
    }

    public void paint(Location blockHit, PaintballTeam paintballTeam, EventPlayer shooter, int size) {
        if (size == 1) {
            // Paint a single block
            this.paintOnBlock(blockHit.getBlock(), paintballTeam, shooter);
            return;
        }
        Sphere sphere = new Sphere(blockHit, size, 75);


        for (Block block : sphere.getSphere()) {
           this.paintOnBlock(block, paintballTeam, shooter);
        }

    }

    private void paintOnBlock(Block block, PaintballTeam paintballTeam, EventPlayer shooter) {
        if (this.blacklistedMaterials.contains(block.getType()) || !block.isSolid()) {
            return;
        }

        PaintballTeam existingPainter = paintedLocations.get(block.getLocation());
        if (existingPainter != null && existingPainter.equals(paintballTeam)) {
            return; // Already painted by the same team
        }

        Location loc = block.getLocation();
        paintedLocations.put(loc, paintballTeam);
        paintballTeam.addScore(1, shooter);
        scoreboard.addScore(paintballTeam, 1);

        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player != null) {
                player.sendBlockChange(loc, paintballTeam.getBlockData());
            }
        }
    }


    @Getter
    @Setter
    public static class PaintballTeam implements Scorer {

        private final Map<EventPlayer, Integer> scoreMap;
        private final Material material;
        private final TextColor textColor;
        private final BlockData blockData;
        private final Location spawnPoint;
        private final ItemStack paintball;
        private final ItemStack snowballMaterial;


        public PaintballTeam(List<EventPlayer> members, ColorKit colorKit, Location spawnPoint, List<TeamBedPart> teamBedParts) {
            this.scoreMap = new HashMap<>();
            for (EventPlayer member : members) {
                this.scoreMap.put(member, 0);
            }
            this.material = colorKit.material;
            this.textColor = colorKit.textColor;
            this.blockData = material.createBlockData();
            this.spawnPoint = spawnPoint;
            this.paintball = new ItemStack(Material.SNOWBALL);
            this.snowballMaterial = new ItemStack(Material.valueOf(colorKit.model.toUpperCase()));

            this.paintball.setAmount(16);
            this.paintball.editMeta(meta -> {
                meta.displayName(Component.text(shortName() + " Paintball").color(textColor).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(Component.text("Throw to paint blocks!").color(NamedTextColor.GRAY)));
                meta.addAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, ATTRIBUTE_MODIFIER);
            });
            this.paintball.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft(colorKit.model));

            // team bed parts
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                for (TeamBedPart teamBedPart : teamBedParts) {
                    Location l = teamBedPart.getBlockLocation();
                    if (l == null) continue;

                    Block block = l.getBlock();
                    BlockState blockState = block.getState();
                    blockState.setType(colorKit.bed);

                    if (blockState.getBlockData() instanceof Bed bed) {
                        bed.setPart(teamBedPart.getPart());
                        bed.setFacing(BlockFaces.yawToFace(l.getYaw(), false).getOppositeFace());
                        blockState.setBlockData(bed);
                    } else {
                        Util.log("<red>Failed to set bed data for team: " + shortName() + " at location: " + l);
                    }
                    blockState.update(true, false);
                }
            });

        }


        public Map<EventPlayer, Integer> membersByScores() {
            return scoreMap.entrySet().stream()
                    .sorted(Map.Entry.<EventPlayer, Integer>comparingByValue().reversed())
                    .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
        }
        public List<EventPlayer> getMembers() {
            return List.copyOf(scoreMap.keySet());
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

        public String shortName() {
            String materialName = material.name().replace("_WOOL", "");
            return Util.formatMaterialName(materialName);
        }

        @Override
        public String getName() {
            return shortName() + " Team";
        }
    }

    @AllArgsConstructor
    public enum ColorKits {

        RED(Material.RED_WOOL, Material.RED_BED, NamedTextColor.RED, "red_dye"),
        GREEN(Material.LIME_WOOL, Material.LIME_BED, NamedTextColor.GREEN, "lime_dye"),
        BLUE(Material.BLUE_WOOL, Material.BLUE_BED, NamedTextColor.BLUE, "blue_dye"),
        ORANGE(Material.ORANGE_WOOL, Material.ORANGE_BED, NamedTextColor.GOLD, "orange_dye"),
        PURPLE(Material.PURPLE_WOOL, Material.PURPLE_BED, NamedTextColor.DARK_PURPLE, "purple_dye"),
        // YELLOW
        PINK(Material.PINK_WOOL, Material.PINK_BED, TextColor.fromHexString("#f4b8da"), "pink_dye"),
        LIGHT_BLUE(Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_BED, NamedTextColor.AQUA, "light_blue_dye"),
        MAGENTA(Material.MAGENTA_WOOL, Material.MAGENTA_BED, TextColor.fromHexString("#d630d6"), "magenta_dye");

        private final Material material;
        private final Material bed;
        private final TextColor textColor;
        private final String model;

        public static Couple<ColorKit, ColorKit> getRandomColorKits() {
            ColorKits[] values = ColorKits.values();
            ColorKits team1 = Util.getRandom(values);
            ColorKits team2;
            do {
                team2 = Util.getRandom(values);
            } while (team1 == team2);
            return Couple.of(ColorKit.of(team1.material, team1.bed, team1.textColor, team1.model),
                    ColorKit.of(team2.material, team2.bed, team2.textColor, team2.model));
        }
    }

    @AllArgsConstructor
    public static class ColorKit {
        private final Material material;
        private final Material bed;
        private final TextColor textColor;
        private final String model;

        public static ColorKit of(Material material, Material bed, TextColor textColor, String model) {
            return new ColorKit(material, bed, textColor, model);
        }
    }
}
