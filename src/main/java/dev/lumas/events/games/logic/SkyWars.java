package dev.lumas.events.games.logic;

import com.google.common.base.Preconditions;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsDefinition;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot.LootPackage;
import dev.lumas.events.configurable.sectors.skywars.SkyWarsLoot.LootPool;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: dynamic loot chests
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
    private static final long DURATION = 600000; // 10 minutes
    private static final long END_DELAY_SECONDS = 10; // results are shown this long before everyone is sent back
    private static final double THROWABLE_KNOCKBACK = 0.4; // snowballs and eggs, the same as an unenchanted punch
    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();
    private static final BlockData AIR = Material.AIR.createBlockData();
    private static final NamespacedKey TEAM_HELMET_KEY = new NamespacedKey(EventMain.getInstance(), "skywars_team_helmet");

    private final Location lobby;
    private final GenericStructure map;
    private final List<Location> spawnPoints;
    private final WorldTiedBoundingBox arena;
    private final double islandRadius;
    private final LootChests lootChests;

    private final MinigameRoleMap<SkyWarsPlayer> roleMap = new MinigameRoleMap<>(SkyWarsPlayer::cleanup);
    private volatile @Nullable Component setupStatus;
    private final AtomicBoolean ended = new AtomicBoolean();
    private final Scoreboard<SkyWarsPlayer> scoreboard = new Scoreboard<>();

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

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(lobby);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        SkyWarsPlayer skyWarsPlayer = roleMap.remove(participant.getUuid());
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
            roleMap.put(new ActivePlayer(participants.get(i), teams.get(i % teamCount)));
        }

        // teams that ended up a player short get their island's chests bumped up a tier
        Map<SkyWarsTeam, Long> teamSizes = roleMap.values().stream()
                .collect(Collectors.groupingBy(SkyWarsPlayer::getTeam, Collectors.counting()));
        long largestTeam = Collections.max(teamSizes.values());
        Set<Location> shortHandedIslands = teams.stream()
                .filter(team -> teamSizes.get(team) < largestTeam)
                .map(SkyWarsTeam::spawn)
                .collect(Collectors.toSet());

        this.showSetupProgress("Pasting Arena", 0);
        CompletableFuture<Void> paste = map.pasteAsync(
                (position, data) -> true,
                data -> data.getMaterial() == Material.TEST_BLOCK ? AIR : data,
                progress -> showSetupProgress("Pasting Arena", progress));
        CompletableFuture.allOf(lootChests.find(), paste)
                .thenCompose(v -> {
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

                    for (SkyWarsPlayer skyWarsPlayer : roleMap) {
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
        for (SkyWarsPlayer skyWarsPlayer : roleMap) {
            skyWarsPlayer.tick();
        }
    }

    private void showSetupProgress(String label, double progress) {
        this.setupStatus = Util.color(Util.getTextColor() + label + ": " + (int) (progress * 100) + "%");
    }

    private void checkForWinner() {
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

        for (SkyWarsPlayer skyWarsPlayer : roleMap) {
            skyWarsPlayer.cleanup();
        }
        for (EventPlayer participant : participants) {
            participant.teleportAsync(lobby);
        }

        map.removeAsync()
                .thenCompose(v -> clearArena())
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to clean up the SkyWars arena", throwable);
                    return null;
                });
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

    @Nullable
    private Location islandOf(Location chest) {
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
                .map(SkyWarsPlayer::getTeam)
                .collect(Collectors.toSet());
    }

    private static void heal(Player player) { // TODO
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
        player.setFallDistance(0f);
        player.setFireTicks(0);
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

        SkyWarsPlayer attacker = roleMap.get(damager.getUniqueId());
        if (attacker == null) return;

        SkyWarsPlayer victim = roleMap.get(event.getEntity().getUniqueId());
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
        SkyWarsPlayer victim = roleMap.get(event.getPlayer().getUniqueId());
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
        SkyWarsPlayer skyWarsPlayer = roleMap.as(event.getPlayer().getUniqueId(), Spectator.class);
        if (skyWarsPlayer != null) {
            event.setCancelled(true);
        }
    }


    public record SkyWarsTeam(TeamColor color, Location spawn) {

        public Component displayName() {
            return Component.text(color.getDisplayName(), color.getTextColor());
        }

        public ItemStack helmet() {
            ItemStack helmet = ItemStack.of(Material.LEATHER_HELMET);
            helmet.editMeta(LeatherArmorMeta.class, meta -> {
                meta.setColor(color.getArmorColor());
                meta.displayName(displayName().append(Component.text(" Team")).decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                meta.setEnchantmentGlintOverride(false);
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(TEAM_HELMET_KEY, PersistentDataType.BOOLEAN, true);
            });
            return helmet;
        }
    }


    @Getter
    private abstract class SkyWarsPlayer extends MinigameRole {
        protected final SkyWarsTeam team;

        protected SkyWarsPlayer(EventPlayer eventPlayer, SkyWarsTeam team) {
            super(eventPlayer);
            this.team = team;
        }

        public boolean isTeammate(SkyWarsPlayer other) {
            return team.equals(other.team);
        }

        public abstract void play();
        public abstract void tick();
        public abstract boolean attack(SkyWarsPlayer victim);
        public abstract void die(PlayerDeathEvent event, @Nullable SkyWarsPlayer killer);
        public abstract void cleanup(); // TODO: CompletableFuture
    }

    private class ActivePlayer extends SkyWarsPlayer {

        protected ActivePlayer(EventPlayer eventPlayer, SkyWarsTeam team) {
            super(eventPlayer, team);
        }

        @Override
        public void play() {
            this.operatePlayer(player -> {
                this.teleportAsync(team.spawn()).thenAccept(success -> {
                    if (success) player.getInventory().setHelmet(team.helmet());
                });
            });
        }

        @Override
        public void cleanup() {
            this.operatePlayer(player -> {
                if (isTeamHelmet(player.getInventory().getHelmet())) {
                    player.getInventory().setHelmet(null);
                }
            });
        }

        @Override
        public void tick() {

        }

        @Override
        public boolean attack(SkyWarsPlayer victim) {
            if (this.isTeammate(victim)) {
                this.eventPlayer.sendActionBar("That's your teammate.");
                return false; // don't harm teammates
            }
            return true;
        }

        @Override
        public void die(PlayerDeathEvent event, @Nullable SkyWarsPlayer killer) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            player.getWorld().strikeLightningEffect(player.getLocation());
            for (ItemStack drop : event.getDrops()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            event.getDrops().clear();
            player.getInventory().clear();
            heal(player);

            SkyWarsPlayer spectator = roleMap.swapRole(this, () -> new Spectator(this.eventPlayer, this.team));
            spectator.play();
            Component deathMessage = event.deathMessage();
            sendAudienceMessage(deathMessage);
            event.deathMessage(null);
            checkForWinner();
        }

        private static boolean isTeamHelmet(@Nullable ItemStack item) {
            return item != null && Util.hasPersistentKey(item, TEAM_HELMET_KEY);
        }
    }

    private class Spectator extends SkyWarsPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, true);

        private volatile boolean returning = false;

        protected Spectator(EventPlayer eventPlayer, SkyWarsTeam team) {
            super(eventPlayer, team);
        }

        @Override
        public void play() {
            Player self = this.eventPlayer.getPlayer();
            if (self == null) return;

            for (EventPlayer other : participants) {
                if (!other.getUuid().equals(this.getUuid())) {
                    other.operatePlayer(viewer -> viewer.hidePlayer(EventMain.getInstance(), self));
                }
            }
            this.operatePlayer(player -> {
                player.getInventory().clear();
                player.addPotionEffect(INVISIBILITY);
                player.setAllowFlight(true);
                player.setFlying(true);
                if (!arena.contains(player.getLocation())) {
                    returnToArena(player);
                }
            });
        }

        @Override
        public void cleanup() {
            Player self = this.eventPlayer.getPlayer();
            if (self == null) return;

            for (EventPlayer other : participants) {
                if (!other.getUuid().equals(this.getUuid())) {
                    other.operatePlayer(viewer -> viewer.showPlayer(EventMain.getInstance(), self));
                }
            }
            this.operatePlayer(player -> {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.setFlying(false);
                player.setAllowFlight(false);
            });
        }

        @Override
        public void tick() {
            this.operatePlayer(player -> {
                if (!arena.contains(player.getLocation())) {
                    returnToArena(player);
                }
            });
        }


        @Override
        public boolean attack(SkyWarsPlayer victim) {
            return false;
        }

        @Override
        public void die(PlayerDeathEvent event, @Nullable SkyWarsPlayer killer) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            heal(player);
            returnToArena(player);
        }

        private void returnToArena(Player player) {
            if (returning || stopping) return;
            returning = true;
            player.setFallDistance(0f);
            Location location = player.getLocation();
            if (location.getY() < arena.getMinY()) {
                location = aboveVoid(location);
            }
            this.teleportAsync(clampToArena(location)).whenComplete((success, throwable) -> returning = false);
        }

        private Location aboveVoid(Location location) {
            World world = location.getWorld();
            int highestY = world.getHighestBlockYAt(location);
            Location up = location.clone();
            up.setY(highestY >= world.getMinHeight() ? highestY + 2 : team.spawn().getY());
            return up;
        }

        private Location clampToArena(Location location) {
            return new Location(arena.getWorld(),
                Math.clamp(location.getX(), arena.getMinX() + 1, arena.getMaxX() - 1),
                Math.clamp(location.getY(), arena.getMinY() + 1, arena.getMaxY() - 1),
                Math.clamp(location.getZ(), arena.getMinZ() + 1, arena.getMaxZ() - 1),
                location.getYaw(), location.getPitch());
        }
    }

    private static final class LootChests {

        private final GenericStructure map;
        private final SkyWarsLoot loot;
        private volatile List<Location> chests = List.of();
        private final Map<Location, LootPool> unopened = new ConcurrentHashMap<>();

        LootChests(GenericStructure map, SkyWarsLoot loot) {
            this.map = map;
            this.loot = loot;
        }

        @Nullable
        LootPool poolFor(Material material) {
            return switch (material) {
                case COPPER_CHEST, WAXED_COPPER_CHEST -> loot.getCommon();
                case EXPOSED_COPPER_CHEST, WAXED_EXPOSED_COPPER_CHEST -> loot.getUncommon();
                case WEATHERED_COPPER_CHEST, WAXED_WEATHERED_COPPER_CHEST -> loot.getRare();
                case OXIDIZED_COPPER_CHEST, WAXED_OXIDIZED_COPPER_CHEST -> loot.getEpic();
                default -> null;
            };
        }

        CompletableFuture<Void> find() {
            return map.findBlocksAsync(data -> poolFor(data.getMaterial()) != null)
                    .thenAccept(found -> this.chests = found);
        }

        CompletableFuture<Void> fillAll(Predicate<Location> shouldUpgrade, DoubleConsumer onProgress) {
            List<Location> chests = this.chests;
            if (chests.isEmpty()) {
                LOGGER.warning("No copper chests found in the SkyWars schematic " + map.getLocalSchemPath());
            }
            AtomicInteger done = new AtomicInteger();
            AtomicInteger filled = new AtomicInteger();
            List<CompletableFuture<Void>> futures = new ArrayList<>(chests.size());
            for (Location location : chests) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                Executors.runSync(location, () -> {
                    try {
                        Block block = location.getBlock();
                        if (shouldUpgrade.test(location)) {
                            upgrade(block);
                        }
                        LootPool pool = poolFor(block.getType());
                        if (pool != null && block.getState(false) instanceof Chest chest) {
                            fill(pool, chest.getBlockInventory());
                            unopened.put(block.getLocation(), pool);
                            filled.incrementAndGet();
                        } else {
                            LOGGER.warning("Expected a copper chest at " + describe(location) + " but found " + block.getType());
                        }
                        onProgress.accept((double) done.incrementAndGet() / chests.size());
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
                futures.add(future);
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenRun(() -> LOGGER.info("Filled " + filled.get() + " of " + chests.size() + " SkyWars chests"
                            + (chests.isEmpty() ? "" : " (e.g. at " + describe(chests.getFirst()) + ")")));
        }

        private static String describe(Location location) {
            return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
        }

        private void upgrade(Block block) {
            Material next = switch (block.getType()) {
                case COPPER_CHEST -> Material.EXPOSED_COPPER_CHEST;
                case EXPOSED_COPPER_CHEST -> Material.WEATHERED_COPPER_CHEST;
                case WEATHERED_COPPER_CHEST -> Material.OXIDIZED_COPPER_CHEST;
                case WAXED_COPPER_CHEST -> Material.WAXED_EXPOSED_COPPER_CHEST;
                case WAXED_EXPOSED_COPPER_CHEST -> Material.WAXED_WEATHERED_COPPER_CHEST;
                case WAXED_WEATHERED_COPPER_CHEST -> Material.WAXED_OXIDIZED_COPPER_CHEST;
                default -> null;
            };
            if (next == null) return;

            BlockData upgraded = next.createBlockData();
            block.getBlockData().copyTo(upgraded);
            block.setBlockData(upgraded, false);
        }

        int claim(Inventory inventory) {
            InventoryHolder holder = inventory.getHolder(false);
            List<InventoryHolder> halves = holder instanceof DoubleChest doubleChest
                    ? Arrays.asList(doubleChest.getLeftSide(false), doubleChest.getRightSide(false))
                    : Collections.singletonList(holder);

            int points = 0;
            for (InventoryHolder half : halves) {
                LootPool pool = half instanceof Chest chest ? unopened.remove(chest.getLocation()) : null;
                if (pool != null) {
                    points += pool.getPoints();
                }
            }
            return points;
        }

        void fill(LootPool pool, Inventory inventory) {
            inventory.clear();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inventory.getSize(); i++) {
                slots.add(i);
            }
            Collections.shuffle(slots, random);

            List<ItemStack> items = spread(roll(pool, random), slots.size(), random);
            for (int i = 0; i < items.size() && i < slots.size(); i++) {
                inventory.setItem(slots.get(i), items.get(i));
            }
        }

        private List<ItemStack> roll(LootPool pool, Random random) {
            List<ItemStack> items = new ArrayList<>();
            for (Map<String, LootPackage> category : pool.getGuaranteed().values()) {
                items.addAll(toItemStacks(pick(category.values(), random)));
            }

            int rolls = random.nextInt(pool.getMinRandomRolls(), pool.getMaxRandomRolls() + 1);
            for (int i = 0; i < rolls; i++) {
                items.addAll(toItemStacks(pick(pool.getRandom().values(), random)));
            }
            return items;
        }

        private List<ItemStack> spread(List<ItemStack> rolled, int slotCount, Random random) {
            List<ItemStack> items = new ArrayList<>();
            List<ItemStack> splittable = new ArrayList<>();
            for (ItemStack item : rolled) {
                (item.getAmount() > 1 ? splittable : items).add(item);
            }

            while (slotCount - items.size() - splittable.size() > 0 && !splittable.isEmpty()) {
                ItemStack stack = splittable.remove(random.nextInt(splittable.size()));
                int splitAmount = random.nextInt(1, stack.getAmount() / 2 + 1);
                ItemStack split = stack.asQuantity(splitAmount);
                stack.setAmount(stack.getAmount() - splitAmount);

                for (ItemStack part : List.of(stack, split)) {
                    (part.getAmount() > 1 && random.nextBoolean() ? splittable : items).add(part);
                }
            }

            items.addAll(splittable);
            Collections.shuffle(items, random);
            return items;
        }

        private LootPackage pick(Collection<LootPackage> packages, Random random) {
            int total = 0;
            for (LootPackage lootPackage : packages) {
                total += lootPackage.getWeight();
            }

            int roll = random.nextInt(total);
            for (LootPackage lootPackage : packages) {
                roll -= lootPackage.getWeight();
                if (roll < 0) {
                    return lootPackage;
                }
            }
            throw new IllegalStateException("Unreachable");
        }

        private List<ItemStack> toItemStacks(LootPackage lootPackage) {
            return lootPackage.getItems().stream()
                .map(item -> ItemStack.of(item.getMaterial(), item.getCount()))
                .toList();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum TeamColor {
        RED("Red", NamedTextColor.RED, DyeColor.RED.getColor()),
        BLUE("Blue", NamedTextColor.BLUE, DyeColor.BLUE.getColor()),
        LIME("Lime", NamedTextColor.GREEN, DyeColor.LIME.getColor()),
        YELLOW("Yellow", NamedTextColor.YELLOW, DyeColor.YELLOW.getColor()),
        ORANGE("Orange", NamedTextColor.GOLD, DyeColor.ORANGE.getColor()),
        AQUA("Aqua", NamedTextColor.AQUA, DyeColor.LIGHT_BLUE.getColor()),
        PINK("Pink", NamedTextColor.LIGHT_PURPLE, DyeColor.PINK.getColor()),
        PURPLE("Purple", NamedTextColor.DARK_PURPLE, DyeColor.PURPLE.getColor()),
        CYAN("Cyan", NamedTextColor.DARK_AQUA, DyeColor.CYAN.getColor()),
        GREEN("Green", NamedTextColor.DARK_GREEN, DyeColor.GREEN.getColor()),
        WHITE("White", NamedTextColor.WHITE, DyeColor.WHITE.getColor()),
        GRAY("Gray", NamedTextColor.GRAY, DyeColor.LIGHT_GRAY.getColor()),
        BROWN("Brown", TextColor.color(0xA0673D), DyeColor.BROWN.getColor()),
        MAROON("Maroon", NamedTextColor.DARK_RED, Color.fromRGB(0x6B1419)),
        NAVY("Navy", NamedTextColor.DARK_BLUE, Color.fromRGB(0x1C2459)),
        BLACK("Black", NamedTextColor.DARK_GRAY, DyeColor.BLACK.getColor());

        private final String displayName;
        private final TextColor textColor;
        private final Color armorColor;
    }
}
