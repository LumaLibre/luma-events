package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.lumas.lumacore.utility.Logging;
import dev.lumas.lumacore.utility.Text;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.configurable.MaterialCount;
import dev.lumas.events.configurable.sectors.TowersDefinition;
import dev.lumas.events.configurable.sectors.TowersItems;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.structures.BoundCircularStructureGrid;
import dev.lumas.events.games.obj.CountdownBossBar;
import dev.lumas.events.games.obj.Scoreboard;
import dev.lumas.events.games.tokenformula.TowersTokenFormula;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

// TODO: Test items, bounding box sizes, add game ending & scoreboard.
public final class Towers extends InventoryUnifiedMinigame {

    private static final int TICK_INTERVAL = 2;
    private static final String KEY_STRING = "towers_game";
    private static final Particle.DustTransition DUST_TRANSITION = new Particle.DustTransition(Color.ORANGE, Color.RED, 4.0f);


    private final Location spawnLocation;
    private final TowersItems towersItems;
    private final int maxRadius;
    private final Location centerPoint;
    private final Map<UUID, TowersPlayer> towersPlayers;
    private final Scoreboard<EventPlayer> scoreboard;
    private final TowersTokenFormula tokenFormula;

    private CountdownBossBar newItemTimer;
    private boolean started = false;

    private double forceGameArenaYLevel;
    private List<Location> gridLocations;

    public Towers(TowersDefinition def) {
        super("Escalation Towers", "Don't fall.", 480000, TICK_INTERVAL, false, false, false, false);

        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.towersItems = def.getTowersItems();
        this.maxRadius = def.getMaxRadius();
        this.centerPoint = def.getCenterPoint();
        this.towersPlayers = new HashMap<>();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new TowersTokenFormula();
        this.forceGameArenaYLevel = this.centerPoint.getY() - 10;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int finalScore = this.scoreboard.getScore(participant);

        this.tokenFormula.giveTokens(participant, finalScore);
        participant.addPermanentScore(MinigameConstant.TOWERS, finalScore);
    }

    @Override
    protected void handleStart() {
        Logging.log("[Towers] Game started with " + this.participants.size() + " participants.");
        List<EventPlayer> shuffledParticipants = new ArrayList<>(this.participants);
        Collections.shuffle(shuffledParticipants, RANDOM);
        for (EventPlayer eventPlayer : shuffledParticipants) {
            ActivePlayer towersPlayer = new ActivePlayer(eventPlayer, this);
            this.towersPlayers.put(eventPlayer.getUuid(), towersPlayer);
            eventPlayer.operatePlayer(player -> {
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(5);
                player.setAllowFlight(false);
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
        }

        BoundCircularStructureGrid grid = new BoundCircularStructureGrid(this.centerPoint, this.maxRadius, 20, 22);
        this.gridLocations = grid.generateSpawnLocations(this.towersPlayers.size());
        List<Location> locations = new ArrayList<>(this.gridLocations);

        for (TowersPlayer towersPlayer : this.towersPlayers.values()) {
            if (!(towersPlayer instanceof ActivePlayer activePlayer)) {
                Logging.errorLog("Expected all players to be an ActivePlayer at game start.");
                continue;
            }
            Location spawnLoc = locations.removeFirst();
            activePlayer.onNewRound(spawnLoc);
        }

        AtomicInteger count = new AtomicInteger(3);
        Executors.runRepeatingAsync(1, TimeUnit.SECONDS, task -> {
            if (count.get() <= 0) {
                task.cancel();
                Executors.runSync(() -> this.newItem());
                this.started = true;
                this.sendAudienceTitle("<b>Go!", "");
                return;
            }
            this.sendAudienceTitle( "<b>" + count.get(), "Get ready!");
            count.getAndDecrement();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.started) return;


        if (this.getActivePlayers().size() <= 1) {
            this.stop();
            return;
        }

        for (TowersPlayer towersPlayer : towersPlayers.values()) {
            towersPlayer.onTick(timeLeft);
        }

        for (Location location : this.gridLocations) {
            Location locationAtYLevel = location.clone();
            locationAtYLevel.setY(this.forceGameArenaYLevel);

            locationAtYLevel.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, locationAtYLevel, 300, 40, 0.0, 40, 0.1, DUST_TRANSITION);
        }

        if (this.newItemTimer == null || this.newItemTimer.isCancelled()) {
            this.newItemTimer = this.newItemTimer();
            this.newItemTimer.start();
        }
    }

    @Override
    protected void handleStop() {
        this.getActivePlayers().forEach(activePlayer -> {
            this.scoreboard.addScore(activePlayer.getEventPlayer(), 2);
        });

        Executors.runSync(() -> {
            this.towersPlayers.values().forEach(towersPlayer -> {
                towersPlayer.cleanup();
                towersPlayer.getEventPlayer().operatePlayer(player -> {
                    player.setFallDistance(0f);
                    player.teleportAsync(this.spawnLocation);
                });
            });

            this.boundingBox.operate(block -> {
                if (!block.isEmpty()) {
                    block.setType(Material.AIR);
                }
            });
            this.boundingBox.getEntities(Entity.class).stream().filter(it -> !(it instanceof Player)).forEach(Entity::remove);
        });
        if (this.newItemTimer != null) {
            this.newItemTimer.stop(false);
        }

        Executors.runSync(() -> {
            for (EventPlayer participant : this.participants) {
                releaseHidden(participant.getPlayer());
            }
        });


        this.scoreboard.handleGameEnd(this.audience, () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<blue><b>Game Over")
                    .seconds(15)
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

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        TowersPlayer towersPlayer = this.towersPlayers.remove(participant.getUuid());
        if (towersPlayer != null) {
            towersPlayer.cleanup();
        }
        Executors.runSync(() -> releaseHidden(participant.getPlayer()));
        return super.removeParticipant(participant);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getEntity();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer == null) {
            Util.sendMsg(bukkitPlayer, "You are not participating in this minigame.");
            return;
        }

        towersPlayer.onDeath(event);
    }


    @EventHandler
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getDamageSource().getCausingEntity() instanceof LivingEntity entity)) {
            return;
        }

        // super lazy checks but i'm in a rush to add this
        UUID unboxedAttacker;

        if (entity instanceof Player attackingPlayer) {
            unboxedAttacker = attackingPlayer.getUniqueId();
        } else {
            String unboxedUUID = Util.getPersistentKey(entity, KEY_STRING, PersistentDataType.STRING);
            if (unboxedUUID == null) return;
            unboxedAttacker = UUID.fromString(unboxedUUID);
        }

        TowersPlayer attackerTowersPlayer = this.towersPlayers.get(unboxedAttacker);
        if ((attackerTowersPlayer == null || attackerTowersPlayer instanceof Spectator) && !(entity instanceof Player)) {
            entity.setHealth(0);
            return;
        }

        if (attackerTowersPlayer instanceof Spectator spectator) {
            spectator.getEventPlayer().sendMessage("You cannot attack players while spectating.");
            event.setCancelled(true);
        }


        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        TowersPlayer victimTowersPlayer = this.towersPlayers.get(victim.getUniqueId());
        if (victimTowersPlayer instanceof ActivePlayer activePlayer) {
            activePlayer.setLastAttacker(unboxedAttacker);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer == null) return;

        if (towersPlayer instanceof Spectator) {
            Util.sendMsg(bukkitPlayer, "You cannot interact while spectating.");
            event.setCancelled(true);
            return;
        }

        ItemStack itemStack = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR && itemStack != null && itemStack.getType().name().contains("_SPAWN_EGG")) {
            Egg egg = bukkitPlayer.launchProjectile(Egg.class);
            egg.setItem(itemStack);
            String entityType = itemStack.getType().name().replace("_SPAWN_EGG", "");
            Util.setPersistentKey(egg, "spawn_egg", PersistentDataType.STRING, entityType);
            itemStack.setAmount(itemStack.getAmount() - 1);
            EquipmentSlot hand = event.getHand();
            if (hand != null) {
                bukkitPlayer.swingHand(hand);
            }
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {
        this.ensureNotIllegal();
        UUID ownerUuid = event.getEntity().getOwnerUniqueId();
        if (!(event.getEntity() instanceof Egg egg) || ownerUuid == null) {
            return;
        }
        String entityType = Util.getPersistentKey(egg, "spawn_egg", PersistentDataType.STRING);
        if (entityType == null) {
            return;
        }
        EntityType type = Util.getEnumFromString(EntityType.class, entityType);
        if (type == null) {
            return;
        }
        Location hitLocation = event.getHitBlock() != null ? event.getHitBlock().getLocation() : egg.getLocation();
        Entity entity = hitLocation.getWorld().spawnEntity(hitLocation.add(0, 1, 0), type);
        Util.setPersistentKey(entity, KEY_STRING, PersistentDataType.STRING, ownerUuid.toString());
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        this.ensureNotIllegal();
        if (!this.started) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        this.ensureNotIllegal();
        TowersPlayer towersPlayer = this.towersPlayers.get(event.getPlayer().getUniqueId());
        if (towersPlayer == null) return;
        if (towersPlayer instanceof Spectator) {
            return;
        }

        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.PLAYER && type != InventoryType.CREATIVE && type != InventoryType.CRAFTING && type != InventoryType.WORKBENCH) {
            event.setCancelled(true);
            towersPlayer.getEventPlayer().sendMessage("You cannot open this type of inventory during this game.");
        }
    }


    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getTarget() instanceof Player targetPlayer)) {
            return;
        }
        TowersPlayer towersPlayer = this.towersPlayers.get(targetPlayer.getUniqueId());
        if (towersPlayer instanceof Spectator) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player targetPlayer)) {
            return;
        }
        TowersPlayer towersPlayer = this.towersPlayers.get(targetPlayer.getUniqueId());
        if (towersPlayer instanceof Spectator) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer == null) return;

        if (towersPlayer instanceof Spectator) {
            Util.sendMsg(bukkitPlayer, "You cannot drop items while spectating.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer == null) return;

        if (towersPlayer instanceof Spectator) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onPlayerAddPotionEffect(EntityPotionEffectEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player targetPlayer)) {
            return;
        }
        TowersPlayer towersPlayer = this.towersPlayers.get(targetPlayer.getUniqueId());
        if (towersPlayer instanceof Spectator && event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        this.ensureNotIllegal();

        Player player = event.getPlayer();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (this.getParticipants().contains(eventPlayer)
                && !player.hasPermission("lumaevents.bypass")
                && !event.getMessage().contains("quit") // super lazy check to allow quitting
        ) {
            event.setCancelled(true);
            eventPlayer.sendMessage("You can't use commands while participating in this minigame. Use /event quit to leave.");
        }
    }



    private <T extends TowersPlayer> T swapRole(Class<? extends TowersPlayer> ifRole, TowersPlayer towersPlayer, Supplier<? extends TowersPlayer> newRoleSupplier) {
        if (ifRole.isInstance(towersPlayer)) {
            return this.swapRole(towersPlayer.getEventPlayer(), newRoleSupplier);
        }
        return (T) towersPlayer;
    }
    private <T extends TowersPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends TowersPlayer> newRoleSupplier) {
        TowersPlayer currentRole = towersPlayers.get(eventPlayer.getUuid());
        if (currentRole != null) {
            Executors.runSync(currentRole::cleanup);
        }
        TowersPlayer newRole = newRoleSupplier.get();
        towersPlayers.put(eventPlayer.getUuid(), newRole);
        return (T) newRole;
    }

    private List<ActivePlayer> getActivePlayers() {
        List<ActivePlayer> activePlayers = new ArrayList<>();
        for (TowersPlayer towersPlayer : this.towersPlayers.values()) {
            if (towersPlayer instanceof ActivePlayer activePlayer) {
                activePlayers.add(activePlayer);
            }
        }
        return activePlayers;
    }

    private CountdownBossBar newItemTimer() {
        return CountdownBossBar.builder()
                .title("<b><yellow>Next drop in: %s | The floor will rise!")
                .seconds(9)
                .color(BossBar.Color.YELLOW)
                .audience(this.audience)
                .callback(() -> {
                    Executors.runSync(this::newItem);
                    this.forceGameArenaYLevel++;
                })
                .build();
    }

    private void newItem() { // FIXME
        for (ActivePlayer activePlayer : this.getActivePlayers()) {
            ItemStack itemStack;
            if (RANDOM.nextInt(100) <= 20) {
                if (RANDOM.nextInt(100) > 40) {
                    List<MaterialCount> materialCounts = Util.getRandom(towersItems.getAllMaterialPackages());
                    for (MaterialCount materialCount : materialCounts) {
                        ItemStack local = ItemStack.of(materialCount.getMaterial(), materialCount.getCount());
                        Executors.sync(() -> activePlayer.giveItem(local));
                        activePlayer.getEventPlayer().sendMessage("You got: " + Util.formatMaterialName(local.getType().toString()) + " x" + local.getAmount());
                    }
                    continue;
                } else {
                    itemStack = ItemStack.of(Util.getRandom(towersItems.getRandomMaterials()));
                }
            } else {
                Material material = Util.getRandom(Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .filter(Material::isSolid)
                        .toList());
                itemStack = ItemStack.of(material, RANDOM.nextInt(3, 5));
            }
            final ItemStack finalItemStack = itemStack;
            activePlayer.getEventPlayer().sendMessage("You got: " + Util.formatMaterialName(itemStack.getType().toString()) + " x" + itemStack.getAmount());
            activePlayer.giveItem(finalItemStack);
        }
    }

    public void releaseHidden(Player player) {
        if (player == null) return;
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(EventMain.getInstance(), player);
        }
    }


    @Getter
    private abstract static class TowersPlayer {
        protected final EventPlayer eventPlayer;
        protected final Towers context;
        protected Location respawnLocation;

        public TowersPlayer(EventPlayer eventPlayer, Towers context) {
            this.eventPlayer = eventPlayer;
            this.context = context;
        }

        public UUID getUuid() {
            return this.eventPlayer.getUuid();
        }

        public abstract void onTick(long timeLeft);
        public abstract void onDeath(PlayerDeathEvent event);
        public abstract void cleanup();
    }


    @Getter
    @Setter
    private static class ActivePlayer extends TowersPlayer {

        private static final int MINECRAFT_MIN_Y = -63;
        private UUID lastAttacker = null;
        private int kills = 0;
        private boolean dirty = false;

        public ActivePlayer(EventPlayer eventPlayer, Towers context) {
            super(eventPlayer, context);
        }

        public void onNewRound(Location spawnLocation) {
            Executors.sync(() -> {
                for (int i = spawnLocation.getBlockY() - 1; i >= MINECRAFT_MIN_Y; i--) {
                    Block block = spawnLocation.getWorld().getBlockAt(spawnLocation.getBlockX(), i, spawnLocation.getBlockZ());
                    block.setType(Material.BEDROCK);
                }
                this.eventPlayer.operatePlayer(player -> {
                    player.setVelocity(new Vector());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 100, false, false, true));
                    Executors.runDelayedAsync(300, TimeUnit.MILLISECONDS, (taks) -> {
                        player.teleportAsync(spawnLocation.add(0, 1, 0).toCenterLocation());
                        player.setFlying(false);
                        player.setAllowFlight(false);
                    });
                });
            });
            this.respawnLocation = spawnLocation;
        }

        public void giveItem(ItemStack itemStack) {
            this.eventPlayer.operatePlayer(player -> {
                player.getWorld().dropItem(player.getEyeLocation(), itemStack);
            });
        }

        @Override
        public void onTick(long timeLeft) {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) {
                return;
            }

            if (player.getLocation().getY() <= this.context.forceGameArenaYLevel) {
                player.damage(5.0);
            }

            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }

            String floorString = ((int)player.getLocation().getY()) + "/" + ((int) this.context.forceGameArenaYLevel);
            player.sendActionBar(Text.mm("<yellow>Time left: " + Util.millisToSecs(timeLeft) + "s | <green>Kills: " + this.kills + " <yellow>| <aqua>Floor: " + floorString));
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            this.context.swapRole(ActivePlayer.class, this, () -> new Spectator(this.getEventPlayer(), this.context));

            if (!dirty) {
                this.dirty = true;
                this.context.scoreboard.addScore(this.eventPlayer, 2);
            }

            event.setCancelled(true);
            this.context.sendAudienceMessage(event.deathMessage());
            if (this.respawnLocation != null) {
                this.eventPlayer.teleportAsync(this.respawnLocation);
            }

            Player victimBukkit = event.getEntity();
            ItemStack[] victimItems = victimBukkit.getInventory().getContents();
            victimBukkit.getInventory().clear();

            if (this.lastAttacker == null || this.lastAttacker.equals(this.getUuid())) {
                return;
            }

            TowersPlayer attackerPlayer = this.context.towersPlayers.get(this.lastAttacker);
            this.context.scoreboard.addScore(attackerPlayer.getEventPlayer(), 2);
            if (attackerPlayer instanceof ActivePlayer activePlayer) {
                activePlayer.kills++;
                Player bukkitAttacker = activePlayer.getEventPlayer().getPlayer();
                if (bukkitAttacker != null) {
                    for (ItemStack victimItem : victimItems) {
                        if (victimItem == null) continue;
                        bukkitAttacker.getWorld().dropItem(bukkitAttacker.getLocation(), victimItem);
                    }
                }
            }
        }

        @Override
        public void cleanup() {

        }
    }

    private static class Spectator extends TowersPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, false, true, true);

        private boolean hidden = false;

        public Spectator(EventPlayer eventPlayer, Towers context) {
            super(eventPlayer, context);
            this.hidePlayer();
            this.eventPlayer.operatePlayer(player -> Executors.runSync(() -> {
                player.getInventory().clear();
            }));
        }

        public void hidePlayer() {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) return;
            for (EventPlayer participant : this.context.getParticipants()) {
                if (participant.getUuid().equals(this.eventPlayer.getUuid())) {
                    continue;
                }
                participant.operatePlayer(pPlayer -> {
                    Executors.runSync(() -> {
                        pPlayer.hidePlayer(EventMain.getInstance(), player);
                    });
                });
            }
            this.hidden = true;
        }

        public void showPlayer() {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) return;
            for (EventPlayer participant : this.context.getParticipants()) {
                if (participant.getUuid().equals(this.eventPlayer.getUuid())) {
                    continue;
                }
                participant.operatePlayer(pPlayer -> {
                    Executors.runSync(() -> {
                        pPlayer.showPlayer(EventMain.getInstance(), player);
                    });
                });
            }
            this.hidden = false;
        }


        @Override
        public void onTick(long timeLeft) {
            this.eventPlayer.operatePlayer(player -> {
                Executors.runSync(() -> {
                    player.addPotionEffect(INVISIBILITY);

                    if (!player.getAllowFlight()) {
                        player.setAllowFlight(true);
                    }

                    if (!player.isFlying()) {
                        player.setFlying(true);
                    }

                    player.sendActionBar(Text.mm("<yellow>Time left: " + Util.millisToSecs(timeLeft) + "s"));
                });
            });
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            event.setCancelled(true);
            this.eventPlayer.operatePlayer(player -> {
                player.teleportAsync(this.context.centerPoint);
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
                player.setFlying(true);
            });
        }


        @Override
        public void cleanup() {
            Executors.runSync(() -> {
                this.showPlayer();
                this.eventPlayer.operatePlayer(player -> {
                    player.teleportAsync(this.context.spawnLocation);
                    player.setAllowFlight(false);
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                });
            });
        }
    }

}
