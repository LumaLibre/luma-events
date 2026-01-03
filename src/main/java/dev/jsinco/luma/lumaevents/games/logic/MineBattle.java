package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MineBattleDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Logger;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MineBattle extends InventoryUnifiedMinigame {

    private static final long HEARTBEAT = 5; // tick every 0.25 seconds

    private final long maxDurationMillis;
    private final Location lobbyLocation;
    private final Location arenaOrigin;
    private final int arenaHeight;
    private final int minRadius;
    private final int maxRadius;
    private final String innerPattern;
    private final String shellPattern;
    private final String outerPattern;
    private final double minPocketSpacing;
    private final int wallPadding;

    private volatile boolean arenaReady = false;
    private CountdownBossBar gameTimerBossBar;
    private ArenaRegions arenaRegions;
    private List<Location> pocketCenters = List.of();
    private final Scoreboard<EventPlayer> scoreboard;
    private final Set<UUID> eliminated = new HashSet<>();
    private final Map<UUID, Set<UUID>> hiddenByViewer = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> forceVisibleUntil = new HashMap<>();

    public MineBattle(MineBattleDefinition def) {
        super("MineBattle", "Break ores, gear up, and fight!", def.getMaxDurationMillis(), HEARTBEAT, true, true, false, false);
        this.lobbyLocation = def.getLobbyLocation();
        this.arenaOrigin = def.getArenaOrigin();
        this.arenaHeight = def.getArenaHeight();
        this.minRadius = def.getMinRadius();
        this.maxRadius = def.getMaxRadius();
        this.innerPattern = buildWeightedPattern(def.getInnerPattern());
        this.shellPattern = buildWeightedPattern(def.getShellPattern());
        this.outerPattern = buildWeightedPattern(def.getOuterPattern());
        this.minPocketSpacing = def.getMinPocketSpacing();
        this.wallPadding = def.getWallPadding();
        this.scoreboard = new Scoreboard<>();
        this.maxDurationMillis = def.getMaxDurationMillis();
        this.boundingBox = computeBoundingBox(arenaOrigin, maxRadius, arenaHeight);
    }

    private WorldTiedBoundingBox computeBoundingBox(Location origin, int maxRadius, int height) {
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        int r = maxRadius + 2; // include outer walls
        Location outerMin = new Location(origin.getWorld(), ox - r, oy - 2, oz - r);
        Location outerMax = new Location(origin.getWorld(), ox + r, oy + height - 1 + 2, oz + r);
        return WorldTiedBoundingBox.of(outerMin, outerMax);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    public static String buildWeightedPattern(Map<String, Double> weights) {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String key = entry.getKey();
            double weight = entry.getValue();
            if (weight <= 0.0) continue;
            Material material;
            try { material = Material.valueOf(key.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { continue; }
            joiner.add(weight + "%" + material.getKey().getKey());
        }
        return joiner.toString();
    }

    @Override
    protected void handleStart() {
        this.arenaReady = false;
        int playerCount = Math.max(1, this.participants.size());
        Logger.log("Generating arena for " + playerCount + " players...");
        int radius = computeRadiusForPlayers(playerCount);
        this.pocketCenters = generatePocketCenters(this.arenaOrigin, radius, this.arenaHeight, playerCount);
        this.arenaRegions = ArenaRegions.of(this.arenaOrigin, radius, this.arenaHeight);
        Executors.runAsync(() -> {
            try {
                buildArenaFAWE(this.arenaRegions);
                carvePocketsFAWE(this.arenaRegions.world(), this.pocketCenters);
            } catch (Throwable t) {
                Logger.logErr(t);
            }
            Executors.runSync(() -> {
                if (this.isCancelled()) return;
                this.arenaReady = true;
                teleportPlayersToPockets();
                Logger.log("Duration remaining when arena ready: " + this.getDuration());
                startGameTimerBossBar();
                this.sendAudienceMessage("<green>MineBattle started!</green>");
            });
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.arenaReady) return;
        Executors.runSync(this::updateLineOfSightVisibility);
        if (aliveCount() <= 1) this.stop();
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.lobbyLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        UUID uuid = participant.getUuid();
        eliminated.add(uuid);
        Executors.runSync(() -> {
            Player leaving = participant.getPlayer();
            if (leaving != null) {
                for (EventPlayer ep : this.participants) {
                    Player viewer = ep.getPlayer();
                    if (viewer != null && !viewer.getUniqueId().equals(uuid)) {
                        viewer.showPlayer(EventMain.getInstance(), leaving);
                    }
                }
            }
            hiddenByViewer.remove(uuid);
            for (Set<UUID> set : hiddenByViewer.values()) set.remove(uuid);
            forceVisibleUntil.remove(uuid);
            for (Map<UUID, Long> m : forceVisibleUntil.values()) m.remove(uuid);
        });
        return super.removeParticipant(participant);
    }

    @Override
    protected void handleStop() {
        unsafe(() -> {
            if (this.gameTimerBossBar != null && !this.gameTimerBossBar.isCancelled()) {
                this.gameTimerBossBar.stop(false);
            }
        });

        Executors.runSync(() -> {
            for (EventPlayer ep : this.participants) {
                ep.teleportAsync(this.lobbyLocation);
                Player p = ep.getPlayer();
                if (p == null) continue;
                cleanPlayer(p);
                for (EventPlayer other : this.participants) {
                    Player o = other.getPlayer();
                    if (o != null) {
                        o.showPlayer(EventMain.getInstance(), p);
                    }
                }
            }
        });
        forceVisibleUntil.clear();
        hiddenByViewer.clear();

        ArenaRegions regions = this.arenaRegions;
        if (regions != null) {
            Executors.runAsync(() -> {
                try {
                    removeArenaFAWE(regions);
                } catch (Throwable t) {
                    Logger.logErr(t);
                }
            });
        }

        this.scoreboard.handleGameEnd(this.audience, () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<blue><b>Game Over")
                    .seconds(10)
                    .callback(() -> {
                        this.participants.forEach(eventPlayer -> {
                            eventPlayer.teleportAsync(this.getGameDropOffLocation());
                            eventPlayer.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    private void startGameTimerBossBar() {
        this.gameTimerBossBar = CountdownBossBar.builder()
                .title("<green>Time Left: %ss")
                .color(BossBar.Color.GREEN)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .callback(() -> {
                    this.sendAudienceMessage("<yellow>Time is up!</yellow>");
                    this.stop();
                })
                .build()
                .start();
    }

    public int computeRadiusForPlayers(int playerCount) {
        playerCount = Math.max(1, playerCount);
        int edgeLoss = 2 * (wallPadding + 1/*to pocket center*/);
        double areaNeededPerPocket = minPocketSpacing * minPocketSpacing;
        double requiredArea = playerCount * areaNeededPerPocket * 1.5/*safety margin*/;
        double usableSideNeeded = Math.sqrt(requiredArea);
        double rNeeded = (usableSideNeeded + edgeLoss - 1.0) / 2.0;
        int radius = (int) Math.ceil(rNeeded);
        radius = Math.max(minRadius, Math.min(maxRadius, radius));

        // Ensure usableSide isn't degenerate if minRadius is tiny
        int usableSide = (2 * radius + 1) - edgeLoss;
        if (usableSide < 3) radius = Math.min(maxRadius, radius + 3);

        return radius;
    }

    private void buildArenaFAWE(ArenaRegions regions) {
        Pattern inner = parsePattern(regions.world(), innerPattern);
        Pattern shell = parsePattern(regions.world(), shellPattern);
        Pattern outer = parsePattern(regions.world(), outerPattern);

        try (EditSession session = WorldEdit.getInstance().newEditSession(regions.world())) {
            int changedOuter = session.setBlocks((Region) regions.outer(), outer);
            int changedShell = session.setBlocks((Region) regions.shell(), shell);
            int changedInner = session.setBlocks((Region) regions.inner(), inner);
            session.flushQueue();
        }
    }
    private void carvePocketsFAWE(World weWorld, List<Location> centers) {
        Pattern air = parsePattern(weWorld, "air");

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (Location c : centers) {
                CuboidRegion pocket = pocketRegion(weWorld, c);
                int changedBlocks = session.setBlocks((Region) pocket, air);
                session.flushQueue();
            }
        }
    }
    private void removeArenaFAWE(ArenaRegions regions) {
        Pattern air = parsePattern(regions.world(), "air");
        try (EditSession session = WorldEdit.getInstance().newEditSession(regions.world())) {
            int changedBlocks = session.setBlocks((Region) regions.outer(), air);
            session.flushQueue();
        }
    }

    private static Pattern parsePattern(World weWorld, String input) throws InputParseException {
        ParserContext ctx = new ParserContext();
        ctx.setWorld(weWorld);
        ctx.setRestricted(false);

        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, ctx);
    }

    private List<Location> generatePocketCenters(Location origin, int radius, int height, int count) {
        org.bukkit.World bw = origin.getWorld();
        if (bw == null) return List.of();
        int cx = origin.getBlockX();
        int cy = origin.getBlockY();
        int cz = origin.getBlockZ();

        // Ensure the 3x3x3 pocket fits and is away from inner walls
        int xMin = cx - radius + wallPadding + 1/*to 3x3x3*/;
        int xMax = cx + radius - wallPadding - 1/*to 3x3x3*/;
        int zMin = cz - radius + wallPadding + 1/*to 3x3x3*/;
        int zMax = cz + radius - wallPadding - 1/*to 3x3x3*/;

        // Keep pockets within inner vertical range and not too near floor/ceiling
        int yMin = cy + 2 + 1/*to 3x3x3*/;
        int yMax = cy + Math.max(4, height - 3 - 1/*to 3x3x3*/);

        if (xMin > xMax || zMin > zMax || yMin > yMax) { // Arena too small
            return List.of(new Location(bw, cx + 0.5, cy + 2.1, cz + 0.5));
        }

        double minDistSq = minPocketSpacing * minPocketSpacing;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        List<Location> pockets = new ArrayList<>(count);
        int maxAttempts = 8000 + count * 2000;
        int attempts = 0;

        while (pockets.size() < count && attempts++ < maxAttempts) {
            int px = rng.nextInt(xMin, xMax + 1);
            int py = rng.nextInt(yMin, yMax + 1);
            int pz = rng.nextInt(zMin, zMax + 1);

            boolean ok = true;
            for (Location other : pockets) {
                double dx = other.getX() - px;
                double dy = other.getY() - py;
                double dz = other.getZ() - pz;
                if ((dx * dx + dy * dy + dz * dz) < minDistSq) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            pockets.add(new Location(bw, px + 0.5, py + 0.1, pz + 0.5));
        }

        // Fall back to coarse grid if random distribution fails
        if (pockets.size() < count) {
            int step = (int) Math.max(4, Math.floor(minPocketSpacing));
            outer:
            for (int x = xMin; x <= xMax; x += step) {
                for (int z = zMin; z <= zMax; z += step) {
                    int y = (yMin + yMax) / 2;
                    Location candidate = new Location(bw, x + 0.5, y + 0.1, z + 0.5);

                    boolean ok = true;
                    for (Location other : pockets) {
                        double dx = other.getX() - candidate.getX();
                        double dy = other.getY() - candidate.getY();
                        double dz = other.getZ() - candidate.getZ();
                        if ((dx * dx + dy * dy + dz * dz) < minDistSq) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;

                    pockets.add(candidate);
                    if (pockets.size() >= count) break outer;
                }
            }
        }

        // If still short, duplicate last-resort near center-ish but offset
        while (pockets.size() < count) {
            double off = pockets.size() * 2.0;
            pockets.add(new Location(bw, cx + 0.5 + off, cy + 2.1, cz + 0.5));
        }

        return pockets;
    }

    private static CuboidRegion pocketRegion(World weWorld, Location center) {
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        BlockVector3 min = BlockVector3.at(x - 1/*to 3x3x3*/, y - 1/*to 3x3x3*/, z - 1/*to 3x3x3*/);
        BlockVector3 max = BlockVector3.at(x + 1/*to 3x3x3*/, y + 1/*to 3x3x3*/, z + 1/*to 3x3x3*/);
        return new CuboidRegion(weWorld, min, max);
    }

    private void teleportPlayersToPockets() {
        int n = Math.min(this.participants.size(), this.pocketCenters.size());
        for (int i = 0; i < n; i++) {
            this.participants.get(i).operatePlayer(this::cleanPlayer);
            this.participants.get(i).teleportAsync(this.pocketCenters.get(i));
            this.participants.get(i).operatePlayer(this::equip);
            this.participants.get(i).sendMessage("<green>You have been placed into your mining pocket!</green>");
        }
    }

    private void updateLineOfSightVisibility() {
        if (!arenaReady) return;

        for (EventPlayer viewerEp : this.participants) {
            UUID viewerId = viewerEp.getUuid();
            if (eliminated.contains(viewerId)) continue;

            Player viewer = viewerEp.getPlayer();
            if (viewer == null) continue;
            if (viewer.getGameMode() != GameMode.SURVIVAL) continue;

            Set<UUID> hidden = hiddenByViewer.computeIfAbsent(viewerId, k -> new HashSet<>());

            for (EventPlayer targetEp : this.participants) {
                UUID targetId = targetEp.getUuid();
                if (viewerId.equals(targetId)) continue;
                if (eliminated.contains(targetId)) continue;

                Player target = targetEp.getPlayer();
                if (target == null) continue;
                if (target.getGameMode() != GameMode.SURVIVAL) continue;

                if (viewer.getWorld() != target.getWorld()) {
                    if (hidden.add(targetId)) {
                        viewer.hidePlayer(EventMain.getInstance(), target);
                    }
                    continue;
                }

                if (isForceVisible(viewerId, targetId)) {
                    if (hidden.remove(targetId)) {
                        viewer.showPlayer(EventMain.getInstance(), target);
                    }
                    continue;
                }

                boolean canSee = viewer.hasLineOfSight(target);

                if (canSee) {
                    if (hidden.remove(targetId)) {
                        viewer.showPlayer(EventMain.getInstance(), target);
                    }
                } else {
                    if (hidden.add(targetId)) {
                        viewer.hidePlayer(EventMain.getInstance(), target);
                    }
                }
            }
        }
    }

    private void forceShowFor(UUID viewerId, UUID targetId, long durationMs) {
        long until = System.currentTimeMillis() + durationMs;
        forceVisibleUntil
                .computeIfAbsent(viewerId, k -> new HashMap<>())
                .merge(targetId, until, Math::max);
        Set<UUID> hidden = hiddenByViewer.computeIfAbsent(viewerId, k -> new HashSet<>());
        hidden.remove(targetId);
        Player viewer = Bukkit.getPlayer(viewerId);
        Player target = Bukkit.getPlayer(targetId);
        if (viewer != null && target != null) {
            viewer.showPlayer(EventMain.getInstance(), target);
        }
    }

    private boolean isForceVisible(UUID viewerId, UUID targetId) {
        Map<UUID, Long> map = forceVisibleUntil.get(viewerId);
        if (map == null) return false;
        Long until = map.get(targetId);
        if (until == null) return false;

        if (until < System.currentTimeMillis()) {
            map.remove(targetId);
            if (map.isEmpty()) forceVisibleUntil.remove(viewerId);
            return false;
        }
        return true;
    }

    private void cleanPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.clearActivePotionEffects();
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(20.0);
        player.setHealth(20.0);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setExp(0.0f);
        player.setLevel(0);
        player.getInventory().clear();
        player.updateInventory();
    }

    private void equip(Player player) {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        sword.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        player.getInventory().addItem(sword);
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        pickaxe.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 3);
        player.getInventory().addItem(pickaxe);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, PotionEffect.INFINITE_DURATION, 0));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamagedByEntityEvent(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (eliminated.contains(damager.getUniqueId())) {
            event.setCancelled(true); // Eliminated players shouldn't be able to hit others
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (eliminated.contains(player.getUniqueId())) {
            event.setCancelled(true); // Eliminated players should be invincible
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (eliminated.contains(player.getUniqueId())) {
            event.setCancelled(true); // Eliminated players shouldn't be able to pick up items
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityDropItemEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;
        if (eliminated.contains(player.getUniqueId())) {
            event.setCancelled(true); // Eliminated players shouldn't be able to drop items
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player dead = event.getEntity();
        UUID deadId = dead.getUniqueId();
        if (!isParticipant(deadId)) return;
        if (eliminated.contains(deadId)) {
            event.setCancelled(true);
            event.getDrops().clear();
            return;
        }
        event.setCancelled(true);
        event.getDrops().clear();
        event.deathMessage(null);

        Player killer = null;
        try {
            if (event.getDamageSource().getCausingEntity() instanceof Player p) killer = p;
        } catch (Throwable ignored) {}
        if (killer == null) killer = dead.getKiller();

        Player finalKiller = killer;
        Executors.runSync(() -> {
            dropInventoryAndClear(dead);
            eliminated.add(deadId);
            if (finalKiller != null) {
                awardKill(finalKiller.getUniqueId(), deadId);
                if (finalKiller.getGameMode() == GameMode.SURVIVAL) {
                    if (finalKiller.hasPotionEffect(PotionEffectType.REGENERATION)) finalKiller.removePotionEffect(PotionEffectType.REGENERATION);
                    finalKiller.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
                }
            }

            hiddenByViewer.remove(deadId);
            for (Set<UUID> set : hiddenByViewer.values()) set.remove(deadId);
            forceVisibleUntil.remove(deadId);
            for (Map<UUID, Long> m : forceVisibleUntil.values()) m.remove(deadId);

            cleanPlayer(dead);
            hideFromOtherPlayers(dead);
            dead.setGameMode(GameMode.ADVENTURE);
            dead.getWorld().playSound(dead.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
            Util.sendMsg(dead, "<red>You have been eliminated!");
        });
    }

    private int aliveCount() {
        int alive = 0;
        for (EventPlayer p : this.participants) {
            if (!eliminated.contains(p.getUuid())) {
                alive++;
            }
        }
        return alive;
    }

    private boolean isParticipant(UUID uuid) {
        return this.participants.stream().anyMatch(p -> p.getUuid().equals(uuid));
    }

    private EventPlayer getParticipant(UUID uuid) {
        return this.participants.stream()
                .filter(p -> p.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    private void hideFromOtherPlayers(Player dead) {
        for (EventPlayer ep : this.participants) {
            if (ep.getUuid().equals(dead.getUniqueId())) continue;
            Player other = ep.getPlayer();
            if (other != null) {
                other.hidePlayer(EventMain.getInstance(), dead);
            }
        }
    }

    private void dropInventoryAndClear(Player dead) {
        org.bukkit.World w = dead.getWorld();
        Location loc = dead.getLocation();
        for (ItemStack item : dead.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) continue;
            w.dropItemNaturally(loc, item.clone());
        }
        for (ItemStack item : dead.getInventory().getArmorContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) continue;
            w.dropItemNaturally(loc, item.clone());
        }
        ItemStack off = dead.getInventory().getItemInOffHand();
        if (!off.getType().isAir() && off.getEnchantmentLevel(Enchantment.VANISHING_CURSE) <= 0) {
            w.dropItemNaturally(loc, off.clone());
        }

        dead.getInventory().clear();
        dead.updateInventory();
    }

    private void awardKill(UUID killer, UUID victim) {
        if (killer.equals(victim)) return;
        if (!isParticipant(killer)) return;
        if (eliminated.contains(killer)) return;
        EventPlayer killerEp = getParticipant(killer);
        if (killerEp != null) {
            scoreboard.addScore(killerEp, 1);
            killerEp.sendMessage("<green>+1 kill</green> <gray>(" + scoreboard.getScore(killerEp) + " total)</gray>");
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        this.ensureNotIllegal();
        if (event.getEntityType() != EntityType.TNT) return;
        event.setYield(0.0f); // prevent block drops
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExp(BlockExpEvent event) {
        this.ensureNotIllegal();
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        if (blockType == Material.TNT) {
            Location location = event.getBlock().getLocation();
            event.setCancelled(true);
            player.getInventory().removeItem(new ItemStack(Material.TNT, 1));
            TNTPrimed tnt = location.getWorld().spawn(location.add(0.5, 0.5, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(40);
            tnt.setSource(player);
            Executors.delayedSync(120L, tnt::remove);
        }

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        Random random = new Random();
        event.setDropItems(false);
        event.setExpToDrop(0);

        switch (blockType) {
            case DEEPSLATE_COAL_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                giveOrDrop(player, new ItemStack(switch (random.nextInt(4)) {
                    case 0 -> Material.CHAINMAIL_HELMET;
                    case 1 -> Material.CHAINMAIL_CHESTPLATE;
                    case 2 -> Material.CHAINMAIL_LEGGINGS;
                    default -> Material.CHAINMAIL_BOOTS;
                }));
            }
            case DEEPSLATE_IRON_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                giveOrDrop(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.IRON_HELMET;
                    case 1 -> Material.IRON_CHESTPLATE;
                    case 2 -> Material.IRON_LEGGINGS;
                    case 3 -> Material.IRON_BOOTS;
                    default -> Material.IRON_SWORD;
                }));
            }
            case DEEPSLATE_REDSTONE_ORE -> {
                player.playSound(player, Sound.ENTITY_CREEPER_DEATH, 1, 1);
                giveOrDrop(player, new ItemStack(Material.TNT));
            }
            case DEEPSLATE_COPPER_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
                handleCopper(player);
            }
            case DEEPSLATE_GOLD_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
                this.getParticipants().stream()
                        .filter(ep -> !Objects.equals(ep.getUuid(), player.getUniqueId()))
                        .forEach(ep -> ep.operatePlayer(p -> {
                            if (p.getGameMode() != GameMode.SURVIVAL) return;
                            if (p.getLocation().distanceSquared(player.getLocation()) > 25*25) return;
                            forceShowFor(player.getUniqueId(), p.getUniqueId(), 3_000); // (= 60 ticks)
                            if (p.hasPotionEffect(PotionEffectType.GLOWING)) p.removePotionEffect(PotionEffectType.GLOWING);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                            p.playSound(p, Sound.ENTITY_WITHER_SPAWN, 1, 1);
                        }));
            }
            case DEEPSLATE_LAPIS_ORE -> {
                player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
                ItemStack reward = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta rewardMeta = (EnchantmentStorageMeta) reward.getItemMeta();
                switch (random.nextInt(14)) {
                    case 0  -> rewardMeta.addStoredEnchant(Enchantment.SHARPNESS, 1, true);
                    case 1  -> rewardMeta.addStoredEnchant(Enchantment.SHARPNESS, 2, true);
                    case 2  -> rewardMeta.addStoredEnchant(Enchantment.FEATHER_FALLING, 3, true);
                    case 3  -> rewardMeta.addStoredEnchant(Enchantment.PROTECTION, 1, true);
                    case 4  -> rewardMeta.addStoredEnchant(Enchantment.PROTECTION, 2, true);
                    case 5  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 1, true);
                    case 6  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 2, true);
                    case 7  -> rewardMeta.addStoredEnchant(Enchantment.BLAST_PROTECTION, 3, true);
                    case 8  -> rewardMeta.addStoredEnchant(Enchantment.KNOCKBACK, 1, true);
                    case 9  -> rewardMeta.addStoredEnchant(Enchantment.KNOCKBACK, 2, true);
                    case 10 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 1, true);
                    case 11 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 2, true);
                    case 12 -> rewardMeta.addStoredEnchant(Enchantment.THORNS, 3, true);
                    default -> rewardMeta.addStoredEnchant(Enchantment.VANISHING_CURSE, 1, true);
                }
                reward.setItemMeta(rewardMeta);
                giveOrDrop(player, reward);
            }
            case DEEPSLATE_EMERALD_ORE -> {
                player.playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1);
                handleEmerald(player);
            }
            case DEEPSLATE_DIAMOND_ORE -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                giveOrDrop(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.DIAMOND_HELMET;
                    case 1 -> Material.DIAMOND_CHESTPLATE;
                    case 2 -> Material.DIAMOND_LEGGINGS;
                    case 3 -> Material.DIAMOND_BOOTS;
                    default -> Material.DIAMOND_SWORD;
                }));
            }
            case ANCIENT_DEBRIS -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                giveOrDrop(player, new ItemStack(switch (random.nextInt(5)) {
                    case 0 -> Material.NETHERITE_HELMET;
                    case 1 -> Material.NETHERITE_CHESTPLATE;
                    case 2 -> Material.NETHERITE_LEGGINGS;
                    case 3 -> Material.NETHERITE_BOOTS;
                    default -> Material.NETHERITE_SWORD;
                }));
            }
            case RAW_GOLD_BLOCK -> {
                player.playSound(player, Sound.BLOCK_BEEHIVE_EXIT, 1, 1);
                giveOrDrop(player, new ItemStack(Material.GOLDEN_APPLE));
            }
            default -> {}
        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSlotChange(InventoryClickEvent event) {
        this.ensureNotIllegal();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current == null) return;
        if (cursor.getType() != Material.ENCHANTED_BOOK) return;
        if (!isSword(current) && !isArmor(current)) return;

        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player player)) return;

        handleEnchantment(event, player, Enchantment.SHARPNESS, 5);
        handleEnchantment(event, player, Enchantment.KNOCKBACK, 2);
        handleEnchantment(event, player, Enchantment.FEATHER_FALLING, 4);
        handleEnchantment(event, player, Enchantment.PROTECTION, 4);
        handleEnchantment(event, player, Enchantment.BLAST_PROTECTION, 4);
        handleEnchantment(event, player, Enchantment.THORNS, 3);
        handleEnchantment(event, player, Enchantment.VANISHING_CURSE, 1);

        event.setCancelled(true);
    }

    private static void handleCopper(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        double currentMax = attr.getBaseValue();
        double delta = ThreadLocalRandom.current().nextBoolean() ? 2.0 : -2.0;
        double newMax = currentMax + delta;
        attr.setBaseValue(newMax);

        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static void handleEmerald(Player player) {

        // Define effects and their max amplifier (0-based, so 2 = level 3)
        Map<PotionEffectType, Integer> effectLimits = new HashMap<>();
        effectLimits.put(PotionEffectType.ABSORPTION, 4);
        effectLimits.put(PotionEffectType.HASTE, 2);
        effectLimits.put(PotionEffectType.RESISTANCE, 1);
        effectLimits.put(PotionEffectType.STRENGTH, 0);
        effectLimits.put(PotionEffectType.FIRE_RESISTANCE, 0);
        effectLimits.put(PotionEffectType.INVISIBILITY, 0);
        effectLimits.put(PotionEffectType.JUMP_BOOST, 0);
        effectLimits.put(PotionEffectType.SPEED, 0);

        boolean hasAllEffectsAtMax = effectLimits.entrySet().stream().allMatch(entry -> {
            PotionEffectType type = entry.getKey();
            int maxAmp = entry.getValue();
            return player.getActivePotionEffects().stream()
                    .anyMatch(effect -> effect.getType().equals(type) && effect.getAmplifier() >= maxAmp);
        });

        if (!hasAllEffectsAtMax) {
            Random random = new Random();
            List<PotionEffectType> keys = new ArrayList<>(effectLimits.keySet());

            while (true) {
                PotionEffectType randomEffect = keys.get(random.nextInt(keys.size()));
                int maxAmp = effectLimits.get(randomEffect);

                PotionEffect current = player.getPotionEffect(randomEffect);
                if (current == null) {
                    player.addPotionEffect(new PotionEffect(randomEffect, -1, 0, false, false)); // Level 1
                    break;
                } else if (current.getAmplifier() < maxAmp) {
                    player.addPotionEffect(new PotionEffect(randomEffect, -1, current.getAmplifier() + 1, false, false));
                    break;
                }
            }

            hasAllEffectsAtMax = effectLimits.entrySet().stream().allMatch(entry -> {
                PotionEffectType type = entry.getKey();
                int maxAmp = entry.getValue();
                return player.getActivePotionEffects().stream()
                        .anyMatch(effect -> effect.getType().equals(type) && effect.getAmplifier() >= maxAmp);
            });

            if (hasAllEffectsAtMax) {
                Util.sendMsg(player, "<red><bold>Caution!</bold></red> <red>Your body can't handle so many effects. If you get one more, you'll die</red>");
            }

        } else {
            player.setHealth(0.0);
            Util.sendMsg(player, "<red>Better listen to our warnings in the future");
        }
    }

    private static void giveOrDrop(Player player, ItemStack toGive) {
        if (player == null || toGive == null || toGive.getType().isAir() || toGive.getAmount() <= 0) return;

        PlayerInventory inv = player.getInventory();
        ItemStack remaining = toGive.clone();

        // Fill partial stacks of similar items
        for (int slot = 0; slot < 36 && remaining.getAmount() > 0; slot++) {
            ItemStack existing = inv.getItem(slot);
            if (existing == null || existing.getType().isAir()) continue;
            if (!existing.isSimilar(remaining)) continue;

            int max = existing.getMaxStackSize();
            int space = max - existing.getAmount();
            if (space <= 0) continue;

            int move = Math.min(space, remaining.getAmount());
            existing.setAmount(existing.getAmount() + move);
            remaining.setAmount(remaining.getAmount() - move);
            inv.setItem(slot, existing);
        }

        // Put into empty slots
        for (int slot = 0; slot < 36 && remaining.getAmount() > 0; slot++) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && !existing.getType().isAir()) continue;

            int max = remaining.getMaxStackSize();
            int move = Math.min(max, remaining.getAmount());

            ItemStack stack = remaining.clone();
            stack.setAmount(move);
            inv.setItem(slot, stack);

            remaining.setAmount(remaining.getAmount() - move);
        }

        // Drop any leftover
        if (remaining.getAmount() > 0) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }

        player.updateInventory();
    }

    private void handleEnchantment(InventoryClickEvent event, Player player, Enchantment enchantment, int maxLevel) {
        EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) event.getCursor().getItemMeta();
        if (enchantmentStorageMeta == null) return;
        if (!enchantmentStorageMeta.hasStoredEnchant(enchantment)) return;
        if (event.getCurrentItem() == null) return;

        // preconditions
        if (List.of(Enchantment.SHARPNESS, Enchantment.KNOCKBACK).contains(enchantment) && !isSword(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to swords!").color(NamedTextColor.RED)));
            return;
        }
        if (List.of(Enchantment.FEATHER_FALLING).contains(enchantment) && !isBoots(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to boots!").color(NamedTextColor.RED)));
            return;
        }
        if (List.of(Enchantment.PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.THORNS).contains(enchantment) && !isArmor(event.getCurrentItem())) {
            player.sendActionBar(enchantment.displayName(1).color(NamedTextColor.RED)
                    .append(Component.text(" may only be applied to armor!").color(NamedTextColor.RED)));
            return;
        }

        // compute target level
        int bookLevel = enchantmentStorageMeta.getStoredEnchantLevel(enchantment);
        int itemLevel = event.getCurrentItem().getEnchantmentLevel(enchantment);
        int resultLevel = Math.max(bookLevel, itemLevel);
        if (bookLevel > 0 && bookLevel == itemLevel) {
            resultLevel++; // equal levels combine to +1 (Luma exclusive as a replacement for crafting them together)
        }
        resultLevel = Math.min(maxLevel, resultLevel);

        // don't enchant if the level wouldn't increase
        if (event.getCurrentItem().getEnchantmentLevel(enchantment) >= resultLevel) {
            player.sendActionBar(Component.text("This item is already enchanted with a higher level!").color(NamedTextColor.RED));
            return;
        }

        // enchant
        event.getCurrentItem().addUnsafeEnchantment(enchantment, resultLevel);
        event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
        player.playSound (player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
    }

    private boolean isSword(ItemStack item) {
        return item.getType().name().toUpperCase().contains("_SWORD");
    }

    private boolean isArmor(ItemStack item) {
        String name = item.getType().name().toUpperCase();
        return name.contains("_HELMET") || name.contains("_CHESTPLATE")
                || name.contains("_LEGGINGS") || name.contains("_BOOTS");
    }

    private boolean isBoots(ItemStack item) {
        return item.getType().name().toUpperCase().contains("_BOOTS");
    }

    private record ArenaRegions(World world, CuboidRegion inner, CuboidRegion shell, CuboidRegion outer) {
        static ArenaRegions of(Location origin, int radius, int height) {
            org.bukkit.World bw = origin.getWorld();
            Preconditions.checkNotNull(bw, "Arena origin world is null");
            World weWorld = BukkitAdapter.adapt(bw);
            int cx = origin.getBlockX();
            int cy = origin.getBlockY();
            int cz = origin.getBlockZ();
            BlockVector3 innerMin = BlockVector3.at(cx - radius, cy, cz - radius);
            BlockVector3 innerMax = BlockVector3.at(cx + radius, cy + height - 1, cz + radius);
            BlockVector3 shellMin = BlockVector3.at(cx - (radius + 1), cy - 1, cz - (radius + 1));
            BlockVector3 shellMax = BlockVector3.at(cx + (radius + 1), cy + height, cz + (radius + 1));
            BlockVector3 outerMin = BlockVector3.at(cx - (radius + 2), cy - 2, cz - (radius + 2));
            BlockVector3 outerMax = BlockVector3.at(cx + (radius + 2), cy + height + 1, cz + (radius + 2));
            return new ArenaRegions(weWorld,
                new CuboidRegion(weWorld, innerMin, innerMax),
                new CuboidRegion(weWorld, shellMin, shellMax),
                new CuboidRegion(weWorld, outerMin, outerMax)
            );
        }
    }
}
