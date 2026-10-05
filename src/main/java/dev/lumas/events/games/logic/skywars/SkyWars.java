package dev.lumas.events.games.logic.skywars;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.configurable.sectors.game.skywars.SkyWarsDefinition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.games.logic.skywars.SkyWarsTeam.TeamColor;
import dev.lumas.events.games.logic.skywars.roles.ActivePlayer;
import dev.lumas.events.games.logic.skywars.roles.SkyWarsRole;
import dev.lumas.events.games.logic.skywars.roles.Spectator;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SkyWars extends InventoryUnifiedMinigame {

    private static final String[] SPLASH = {
        "Trust me, this one's good!",
        "Trust me, this one's new!",
        "Don't fall! >.<",
        "Now on Luma!",
        "I hope my teammate is good...",
        "That's it?!",
        "You know how this goes...",
        "A doozy!",
        "How do we play this?",
        "600,000 milliseconds long!"
    };
    private static final long DURATION = 600000;
    private static final long END_DELAY_SECONDS = 10;
    private static final long CLEANUP_TIMEOUT_SECONDS = 30;
    private static final double THROWABLE_KNOCKBACK = 0.4;
    static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();
    private static final BlockData AIR = Material.AIR.createBlockData();
    private static final Set<Material> MARKER_BLOCKS = Set.of(Material.TEST_BLOCK, Material.CHEST);

    private final Location lobby;
    private final GenericStructure map;
    private final List<Location> spawnPoints;
    @Getter
    private final WorldTiedBoundingBox arena;
    private final double islandRadius;
    private final LootChests lootChests;

    @Getter
    private final MinigameRoleMap<SkyWarsRole> roleMap = new MinigameRoleMap<>(SkyWarsRole::cleanup);
    private volatile @Nullable Component setupStatus;
    private final AtomicBoolean ended = new AtomicBoolean();
    private final Scoreboard<SkyWarsRole> scoreboard = new Scoreboard<>();

    public SkyWars(SkyWarsDefinition def) {
        super("SkyWars", Util.getRandom(SPLASH), DURATION, 2, true, true, false, false);
        this.lobby = def.getLobby();
        this.map = new GenericStructure(def.getOrigin(), def.getMap());
        this.spawnPoints = map.findBlocks(data -> data.getMaterial() == Material.TEST_BLOCK).stream()
                .map(location -> location.add(0.5, 0, 0.5))
                .toList();
        Preconditions.checkArgument(!spawnPoints.isEmpty(), "SkyWars map " + def.getMap() + " has no test blocks marking spawn points");
        this.arena = def.getRegion().toWorldTiedBoundingBox();
        this.boundingBox = arena;
        this.islandRadius = def.getIslandRadius();
        this.lootChests = new LootChests(map, def.getLoot());
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: Event tokens
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(lobby);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        SkyWarsRole skyWarsPlayer = roleMap.remove(participant.getUuid());
        if (skyWarsPlayer != null) {
            skyWarsPlayer.cleanup();
        }
        participant.teleportAsync(lobby);
        boolean removed = super.removeParticipant(participant, doTeleport);
        this.checkForWinner();
        return removed;
    }

    @Override
    protected void handleStart() {
        Collections.shuffle(participants);
        List<Location> spawns = new ArrayList<>(spawnPoints);
        Collections.shuffle(spawns);

        int teamCount = Math.ceilDiv(participants.size(), Math.ceilDiv(participants.size(), spawns.size()));
        TeamColor[] colors = TeamColor.values();
        List<SkyWarsTeam> teams = IntStream.range(0, teamCount)
                .mapToObj(i -> new SkyWarsTeam(colors[i % colors.length], spawns.get(i)))
                .toList();
        for (int i = 0; i < participants.size(); i++) {
            roleMap.put(new ActivePlayer(this, participants.get(i), teams.get(i % teamCount)));
        }

        // teams that ended up a player short get their island's chests bumped up a tier
        Map<SkyWarsTeam, Long> teamSizes = roleMap.values().stream()
                .collect(Collectors.groupingBy(SkyWarsRole::getTeam, Collectors.counting()));
        long largestTeam = Collections.max(teamSizes.values());
        Set<Location> shortHandedIslands = teams.stream()
                .filter(team -> teamSizes.get(team) < largestTeam)
                .map(SkyWarsTeam::spawn)
                .collect(Collectors.toSet());

        this.showSetupProgress("Pasting Arena", 0);
        CompletableFuture<Void> paste = map.pasteAsync(
                (position, data) -> true,
                data -> MARKER_BLOCKS.contains(data.getMaterial()) ? AIR : data,
                progress -> showSetupProgress("Pasting Arena", progress));
        CompletableFuture.allOf(lootChests.find(), paste)
                .thenCompose(v -> {
                    lootChests.activateDynamic((int) largestTeam);
                    this.showSetupProgress("Filling Chests", 0);
                    return lootChests.fillAll(
                            chest -> shortHandedIslands.contains(islandOf(chest)),
                            progress -> showSetupProgress("Filling Chests", progress));
                })
                .whenComplete((v, throwable) -> {
                    this.setupStatus = null;
                    if (throwable != null) {
                        LOGGER.error("Failed to set up SkyWars map", throwable);
                        this.stop();
                        return;
                    }

                    if (!this.active || this.stopping) {
                        return;
                    }

                    for (SkyWarsRole skyWarsPlayer : roleMap) {
                        skyWarsPlayer.play();
                    }
                });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.finish();
        }
        Component status = this.setupStatus;
        if (status != null && this.audience != null) {
            this.audience.sendActionBar(status);
        }
        for (SkyWarsRole skyWarsPlayer : roleMap) {
            skyWarsPlayer.tick();
        }
    }

    private void showSetupProgress(String label, double progress) {
        this.setupStatus = Util.color(Util.getTextColor() + label + ": " + (int) (progress * 100) + "%");
    }

    public void checkForWinner() {
        if (!this.active || this.stopping || this.open) {
            return;
        } else if (aliveTeams().size() > 1 || !finish()) {
            return;
        }

        Executors.runDelayedAsync(TimeUnit.SECONDS, END_DELAY_SECONDS, _ -> this.stop());
    }

    private boolean finish() {
        if (!ended.compareAndSet(false, true)) {
            return false;
        }

        Set<SkyWarsTeam> alive = aliveTeams();
        SkyWarsTeam winner = alive.size() == 1 ? alive.iterator().next() : null;
        scoreboard.handleGameEnd(this.audience, () -> sendAudienceMessage(winner == null
                ? Component.text("No team won this time!")
                : Component.empty()
                        .append(winner.displayName().append(Component.text(" Team")))
                        .append(Component.text(" won the game!"))));
        return true;
    }

    @Override
    protected void handleStop() {
        this.finish();

        List<CompletableFuture<Void>> cleanups = new ArrayList<>();
        for (SkyWarsRole skyWarsPlayer : roleMap) {
            try {
                cleanups.add(skyWarsPlayer.cleanup());
            } catch (Throwable throwable) {
                cleanups.add(CompletableFuture.failedFuture(throwable));
            }
        }
        for (EventPlayer participant : participants) {
            participant.teleportAsync(lobby);
        }

        CompletableFuture.allOf(cleanups.toArray(CompletableFuture[]::new))
                .orTimeout(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    LOGGER.error("Not every SkyWars player finished cleaning up, removing the map anyway", throwable);
                    return null;
                })
                .thenComposeAsync(v -> map.removeAsync(), Executors::runAsync)
                .thenCompose(v -> clearArena())
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to clean up the SkyWars arena", throwable);
                    return null;
                });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (ended.get() && roleMap.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Entity damager = event.getDamageSource().getCausingEntity();
        if (damager == null) return;

        SkyWarsRole attacker = roleMap.get(damager.getUniqueId());
        if (attacker == null) return;

        SkyWarsRole victim = roleMap.get(event.getEntity().getUniqueId());
        if (victim != null && victim != attacker && !attacker.attack(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        this.ensureNotIllegal();
        if (ended.get()) return;
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball || projectile instanceof Egg)) return;
        if (!(event.getHitEntity() instanceof Player victim) || !(projectile.getShooter() instanceof Player thrower)) return;

        ActivePlayer target = roleMap.as(victim.getUniqueId(), ActivePlayer.class);
        ActivePlayer attacker = roleMap.as(thrower.getUniqueId(), ActivePlayer.class);

        if (target == null || attacker == null || target == attacker || !attacker.attack(target)) return;

        Vector from = projectile.getVelocity().multiply(-1);
        Executors.runSync(victim, () -> {
            if (!victim.isValid() || victim.getNoDamageTicks() > victim.getMaximumNoDamageTicks() / 2) return;

            victim.knockback(THROWABLE_KNOCKBACK, from.getX(), from.getZ());
            victim.playHurtAnimation((float) (Math.toDegrees(Math.atan2(from.getZ(), from.getX())) - victim.getYaw() - 90));
            victim.getWorld().playSound(victim, Sound.ENTITY_PLAYER_HURT, 1f, 1f);
            victim.setNoDamageTicks(victim.getMaximumNoDamageTicks());
            victim.setLastDamage(0);
            victim.setKiller(thrower);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        SkyWarsRole victim = roleMap.get(event.getPlayer().getUniqueId());
        if (victim == null) return;

        Player killer = event.getPlayer().getKiller();
        victim.die(event, killer == null ? null : roleMap.get(killer.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        this.ensureNotIllegal();
        ActivePlayer player = roleMap.as(event.getPlayer().getUniqueId(), ActivePlayer.class);
        if (player == null) return;

        int points = lootChests.claim(event.getInventory());
        if (points > 0) {
            scoreboard.addScore(player, points);
            player.getEventPlayer().sendActionBar("<gold>+" + points + (points == 1 ? " point" : " points"));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        SkyWarsRole skyWarsPlayer = roleMap.as(event.getPlayer().getUniqueId(), Spectator.class);
        if (skyWarsPlayer != null) {
            event.setCancelled(true);
        }
    }

    private CompletableFuture<Void> clearArena() {
        World world = arena.getWorld();
        int minX = (int) Math.floor(arena.getMinX());
        int maxX = (int) Math.floor(arena.getMaxX());
        int minZ = (int) Math.floor(arena.getMinZ());
        int maxZ = (int) Math.floor(arena.getMaxZ());
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(arena.getMinY()));
        int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.floor(arena.getMaxY()));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                int cx = chunkX;
                int cz = chunkZ;
                CompletableFuture<Void> future = new CompletableFuture<>();
                world.getChunkAtAsync(cx, cz).whenComplete((chunk, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        return;
                    }
                    Executors.runSync(world, cx, cz, () -> {
                        try {
                            for (Entity entity : chunk.getEntities()) {
                                if (!(entity instanceof Player) && arena.contains(entity.getLocation())) {
                                    entity.remove();
                                }
                            }

                            // cheaper to do this
                            ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                            for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++) {
                                for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                                    for (int y = minY; y <= maxY; y++) {
                                        if (!snapshot.getBlockType(x & 15, y, z & 15).isAir()) {
                                            chunk.getBlock(x & 15, y, z & 15).setType(Material.AIR, false);
                                        }
                                    }
                                }
                            }
                            future.complete(null);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                });
                futures.add(future);
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }


    private @Nullable Location islandOf(Location chest) {
        Location island = null;
        double closest = islandRadius * islandRadius;
        for (Location spawn : spawnPoints) {
            double dx = spawn.getX() - chest.getX();
            double dz = spawn.getZ() - chest.getZ();
            double distance = dx * dx + dz * dz;
            if (distance <= closest) {
                closest = distance;
                island = spawn;
            }
        }
        return island;
    }

    private Set<SkyWarsTeam> aliveTeams() {
        return roleMap.getMatching(ActivePlayer.class).stream()
            .map(SkyWarsRole::getTeam)
            .collect(Collectors.toSet());
    }
}
