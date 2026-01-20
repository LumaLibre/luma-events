package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TNTRunDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRole;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRoleMap;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.WorldEditStructure;
import dev.jsinco.luma.lumaevents.games.interfaces.packet.BlockAnimationPlatformConfig;
import dev.jsinco.luma.lumaevents.games.interfaces.packet.BlockAnimationPlatform;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.lumas.lumacore.utility.Logging;
import dev.lumas.lumacore.utility.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;

// no wildcard imports please
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TNTRun extends InventoryUnifiedMinigame {

    private static final BlockData AIR_BLOCK_DATA = Material.AIR.createBlockData();
    private static final double DECAY_PRECISION = 1.0e-4;
    private static final NamespacedKey POWERUP_ID_KEY = new NamespacedKey(EventMain.getInstance(), "tnt-run-powerup");

    private final WorldEditStructure worldEditStructure;
    private final Location lobbyLocation;
    private final Location arenaOrigin;
    private final int decayDelayTicks;
    private final int eliminationHeight;

    private final int powerupMaxAlive;
    private final int powerupSpawnAttempts;

    private final int slowFallingTicks;
    private final int updraftCooldownTicks;
    private final int jumpSpeedTicks;
    private final int platformTicks;
    private final double smallUpdraftY;
    private final double bigUpdraftY;

    private volatile boolean arenaReady = false;
    private volatile boolean decayArmed = false;

    private CountdownBossBar countdownBossBar;
    private CountdownBossBar gameTimerBossBar;

    private final Scoreboard<EventPlayer> scoreboard = new Scoreboard<>();
    private final MinigameRoleMap<AbstractTNTRunPlayer> roleMap = new MinigameRoleMap<>(AbstractTNTRunPlayer::cleanup);
    private final Map<BlockPos, Long> decayQueue = new HashMap<>();
    private long tickCounter = 0L;

    private float powerupSpinAngle = 0f;
    private final Map<UUID, String> powerupByEntity = new HashMap<>();
    private final Map<UUID, ItemStack> powerupItemByEntity = new HashMap<>();
    private final Map<UUID, Long> updraftCooldownUntilTick = new HashMap<>();
    private final BlockAnimationPlatform tempPlatforms;

    public TNTRun(TNTRunDefinition def) {
        super("TNT Run", "Don't fall down!", def.getTimeLimitSeconds() * 1000L, 1,
                false, true, false, true);

        this.decayDelayTicks = def.getDecayDelayTicks();
        this.lobbyLocation = def.getLobbyLocation();
        this.arenaOrigin = def.getArenaOrigin();
        this.eliminationHeight = def.getEliminationHeight();
        this.worldEditStructure = new WorldEditStructure(arenaOrigin, def.getMapSchematic());
        this.boundingBox = worldEditStructure.getBoundingBox();

        this.powerupMaxAlive = def.getPowerupMaxAlive();
        this.powerupSpawnAttempts = def.getPowerupSpawnAttempts();

        this.slowFallingTicks = def.getSlowFallingTicks();
        this.updraftCooldownTicks = def.getUpdraftCooldownTicks();
        this.jumpSpeedTicks = def.getJumpSpeedTicks();
        this.platformTicks = def.getPlatformTicks();
        this.smallUpdraftY = def.getSmallUpdraftY();
        this.bigUpdraftY = def.getBigUpdraftY();
        this.tempPlatforms = BlockAnimationPlatform.builder()
                .config(BlockAnimationPlatformConfig.defaultBedrock3x3(platformTicks, Math.min(40, platformTicks)))
                .replacePredicate(block -> true) // replace all types of blocks
                .viewersSupplier(() -> this.participants.stream()
                        .map(EventPlayer::getPlayer)
                        .filter(Objects::nonNull)
                        .toList())
                .isActiveSupplier(() -> this.decayArmed) // active condition
                .build();
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: Implement
        // For Mitality: See other games/tokenformulas. Determine how many tokens should be given based on score
    }


    @Override
    protected void handleStart() {

        for (EventPlayer eventPlayer : this.participants) {
            ActiveTNTRunPlayer role = new ActiveTNTRunPlayer(eventPlayer, this);
            this.roleMap.put(role);

            eventPlayer.operatePlayer(LivingEntity::clearActivePotionEffects);
        }

        worldEditStructure.pasteAsync().whenComplete((vo, thr) -> {
            Executors.runSync(() -> {
                if (this.isCancelled()) return;
                this.arenaReady = true;
                Logging.log("[TNTRun] Arena ready.");


                // Teleport players after arena is done being pasted.
                this.teleportPlayersToArenaThenStartCountdown();
            });
        });

    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.arenaReady) return;
        tickCounter++;

        processDecayQueue();

        if (decayArmed && boundingBox != null && powerupByEntity.size() < powerupMaxAlive) {
            for (int i = 0; i <= powerupSpawnAttempts; i++) trySpawnRandomPowerup();
        }
        spinPowerups();

        if (tickCounter % 5 != 0) return;

        for (AbstractTNTRunPlayer tntRunPlayer : this.roleMap) {
            tntRunPlayer.tick();
        }

        if (this.roleMap.getMatching(ActiveTNTRunPlayer.class).size() <= 1) {
            this.stop();
        }
    }

    @Override
    protected void handleStop() {
        this.decayArmed = false;

        unsafe(this::despawnAllPowerups);
        tempPlatforms.cleanup();

        unsafe(() -> {

            if (countdownBossBar != null && !countdownBossBar.isCancelled()) {
                countdownBossBar.stop(false);
            }
            if (gameTimerBossBar != null && !gameTimerBossBar.isCancelled()) {
                gameTimerBossBar.stop(false);
            }
        });

        for (AbstractTNTRunPlayer tntRunPlayer : this.roleMap) {
            tntRunPlayer.cleanup();
            tntRunPlayer.getEventPlayer().teleportAsync(this.lobbyLocation);
        }

        this.worldEditStructure.remove();
        this.decayQueue.clear();

        this.scoreboard.handleGameEnd(this.audience, () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Game Over")
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

    @Override
    protected boolean handleParticipantJoin(EventPlayer eventPlayer) {
        eventPlayer.teleportAsync(this.lobbyLocation);
        return super.handleParticipantJoin(eventPlayer);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        AbstractTNTRunPlayer tntRunPlayer = this.roleMap.remove(participant.getUuid());
        tntRunPlayer.cleanup();
        participant.operatePlayer(p -> p.setFallDistance(0f));
        return super.removeParticipant(participant);
    }

    private void teleportPlayersToArenaThenStartCountdown() {
        for (EventPlayer eventPlayer : this.participants) {
            eventPlayer.teleportAsync(this.arenaOrigin);
        }

        this.countdownBossBar = CountdownBossBar.builder()
                .audience(this.audience)
                .title("<yellow><b>Starting in %ss</b>")
                .color(BossBar.Color.YELLOW)
                .seconds(10)
                .callback(() -> {
                    this.sendAudienceMessage("<green>TNT Run started!</green>");
                    this.decayArmed = true;
                    this.startGameTimerBossBar();
                })
                .build()
                .start();
    }

    private void startGameTimerBossBar() {
        this.gameTimerBossBar = CountdownBossBar.builder()
                .title("<green>Time Left: %ss")
                .color(BossBar.Color.GREEN)
                // correct for the 10s at the start
                .miliseconds(this.getDuration() - 10_000L)
                .audience(this.audience)
                .callback(() -> {
                    this.sendAudienceMessage("<yellow>Time is up!</yellow>");
                    this.stop();
                })
                .build()
                .start();
    }

    private void scheduleDecayUnderFootprint(Player player) {
        if (!this.decayArmed) {
            return;
        }

        BoundingBox bb = player.getBoundingBox();
        int minX = (int) Math.floor(bb.getMinX() + DECAY_PRECISION);
        int maxX = (int) Math.floor(bb.getMaxX() - DECAY_PRECISION);
        int minZ = (int) Math.floor(bb.getMinZ() + DECAY_PRECISION);
        int maxZ = (int) Math.floor(bb.getMaxZ() - DECAY_PRECISION);
        int y0 = (int) Math.floor(bb.getMinY() - DECAY_PRECISION);

        org.bukkit.World w = player.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int dy = 0; dy <= 1; dy++) {
                    Block block = w.getBlockAt(x, y0 - dy, z);
                    Block above = w.getBlockAt(x, y0 - dy + 1, z);
                    if (block.getType().isAir() || !above.getType().isAir()) continue;
                    if (!block.getType().hasGravity()) continue;
                    scheduleDecay(block);
                }
            }
        }
    }

    private void processDecayQueue() {
        if (!this.arenaReady || !this.decayArmed || this.decayQueue.isEmpty()) return;

        World world = arenaOrigin.getWorld();
        if (world == null) return;

        long nowTick = this.tickCounter;

        Iterator<Map.Entry<BlockPos, Long>> it = decayQueue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (e.getValue() > nowTick) continue;

            BlockPos pos = e.getKey();
            it.remove();

            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            Material type = block.getType();

            if (type.isAir() || !type.hasGravity()) continue;

            block.setBlockData(AIR_BLOCK_DATA, false);

            Block under = block.getRelative(BlockFace.DOWN);
            if (under.getType() == Material.TNT) {
                under.setBlockData(AIR_BLOCK_DATA, false);
            }
        }
    }

    private void scheduleDecay(Block block) {
        decayQueue.putIfAbsent(
                new BlockPos(block.getX(), block.getY(), block.getZ()),
                tickCounter + decayDelayTicks
        );
    }


    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !this.decayArmed) return;
        this.ensureNotIllegal();

        Player player = event.getPlayer();
        ActiveTNTRunPlayer activeTntRunPlayer = this.roleMap.as(player.getUniqueId(), ActiveTNTRunPlayer.class);

        if (activeTntRunPlayer == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        // Only process when moving across block borders
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Block blockBelow = to.getBlock().getRelative(BlockFace.DOWN);
        Material type = blockBelow.getType();
        if (type.isAir() || !type.hasGravity()) {
            return;
        }

        this.scheduleDecay(blockBelow);
        this.tryPickupPowerup(player);
    }


    public static abstract class AbstractTNTRunPlayer extends MinigameRole {
        protected final TNTRun context;

        protected AbstractTNTRunPlayer(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer);
            this.context = context;
        }

        public abstract void cleanup();

        public abstract void tick();
    }


    public static class ActiveTNTRunPlayer extends AbstractTNTRunPlayer {
        protected ActiveTNTRunPlayer(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer, context);
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void tick() {
            Player player = this.eventPlayer.getPlayer();

            if (player == null) {
                return;
            }

            player.setFoodLevel(20);

            if (player.getLocation().getY() < this.context.eliminationHeight && this.context.decayArmed) {
                this.eliminate();
                return;
            }

            if (this.context.decayArmed) {
                this.context.scheduleDecayUnderFootprint(player);
            }

            // add score
            this.context.scoreboard.addScore(this.eventPlayer, 1);
        }


        public void eliminate() {
            // swap to spectator role
            this.context.roleMap.swapRole(this, () -> new TNTRunSpectator(this.eventPlayer, this.context));
            this.eventPlayer.teleportAsync(this.context.arenaOrigin);
            this.eventPlayer.operatePlayer(player -> {
                player.playSound(player, Sound.ENTITY_ALLAY_DEATH, SoundCategory.MASTER, 1.0f, 1.0f);
            });
            this.eventPlayer.sendMessage("<red>You have been eliminated!");
        }
    }

    private static class TNTRunSpectator extends AbstractTNTRunPlayer {

        protected TNTRunSpectator(EventPlayer eventPlayer, TNTRun context) {
            super(eventPlayer, context);
            this.hide();
        }

        @Override
        public void cleanup() {
            this.show();
            this.eventPlayer.operatePlayer(player -> {
                player.setAllowFlight(false);
                player.setFlying(false);
            });
        }

        public void hide() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;

                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;

                    bukkitOther.hidePlayer(EventMain.getInstance(), self);
                }
            });
        }

        public void show() {
            Executors.runSync(() -> {
                Player self = this.getEventPlayer().getPlayer();
                if (self == null) return;

                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther == null) continue;

                    bukkitOther.showPlayer(EventMain.getInstance(), self);
                }
            });
        }

        @Override
        public void tick() {
            this.eventPlayer.operatePlayer(player -> {
                if (!player.getAllowFlight()) { // Other plugins may interfere
                    player.setAllowFlight(true);
                }

                if (!player.isFlying()) { // Some players are not aware that they can fly, force them to fly
                    player.setFlying(true);
                }
            });
        }
    }

    private enum PowerupType {
        UPDRAFT_SMALL(Material.FEATHER, "<aqua><b>Small Updraft") {
            @Override
            public boolean handle(TNTRun ctx, Player player) {
                if (!ctx.checkAndApplyUpdraftCooldown(player)) return false;
                org.bukkit.util.Vector v = player.getVelocity();
                player.setVelocity(new Vector(v.getX(), Math.max(v.getY(), ctx.smallUpdraftY), v.getZ()));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, SoundCategory.MASTER, 1.0f, 1.1f);
                return true;
            }
        },
        UPDRAFT_BIG(Material.FIREWORK_ROCKET, "<aqua><b>Big Updraft") {
            @Override
            public boolean handle(TNTRun ctx, Player player) {
                if (!ctx.checkAndApplyUpdraftCooldown(player)) return false;
                org.bukkit.util.Vector v = player.getVelocity();
                player.setVelocity(new org.bukkit.util.Vector(v.getX(), Math.max(v.getY(), ctx.bigUpdraftY), v.getZ()));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 1.0f, 1.1f);
                return true;
            }
        },
        SLOW_FALL(Material.PHANTOM_MEMBRANE, "<aqua><b>Slow Falling") {
            @Override
            public boolean handle(TNTRun ctx, Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, ctx.slowFallingTicks, 0, false, false, true));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.MASTER, 1.0f, 1.2f);
                return true;
            }
        },
        JUMP_SPEED(Material.RABBIT_FOOT, "<aqua><b>Jump & Speed") {
            @Override
            public boolean handle(TNTRun ctx, Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ctx.jumpSpeedTicks, 1, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ctx.jumpSpeedTicks, 0, false, false, true));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RABBIT_JUMP, SoundCategory.MASTER, 1.0f, 1.1f);
                return true;
            }
        },
        PLATFORM(Material.BEDROCK, "<aqua><b>Temporary Platform") {
            @Override
            public boolean handle(TNTRun ctx, Player player) {
                ctx.tempPlatforms.spawnUnderPlayer(player);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, SoundCategory.MASTER, 0.9f, 0.8f);
                return true;
            }
        };

        final Material material;
        final Component displayName;



        PowerupType(Material material, String displayName) {
            this.material = material;
            this.displayName = Text.mmNoItalic(displayName);
        }

        public abstract boolean handle(TNTRun ctx, Player player);


        public ItemStack createPowerupItem() {
            return Util.createItem(this.material, meta -> {
                meta.getPersistentDataContainer().set(POWERUP_ID_KEY, PersistentDataType.STRING, this.toString());
                meta.displayName(this.displayName);
                meta.lore(Text.mml("<gray>Right-click to activate"));
            });
        }


        @Nullable
        static PowerupType fromId(String id) {
            return Util.getEnumFromString(PowerupType.class, id);
        }

        static PowerupType random() {
            return Util.getRandom(values());
        }
    }

    private void spinPowerups() {
        if (!arenaReady || !decayArmed) return;
        org.bukkit.World w = arenaOrigin.getWorld();
        if (w == null) return;
        if (powerupByEntity.isEmpty()) return;

        powerupSpinAngle += 0.12f;
        if (powerupSpinAngle > (float) (Math.PI * 2)) powerupSpinAngle -= (float) (Math.PI * 2);

        for (UUID id : new ArrayList<>(powerupByEntity.keySet())) {
            Entity e = w.getEntity(id);
            if (!(e instanceof ItemDisplay d)) continue;
            Transformation t = d.getTransformation();
            t.getScale().set(0.67f, 0.67f, 0.67f);
            t.getLeftRotation().set(new AxisAngle4f(powerupSpinAngle, 0f, 1f, 0f));
            d.setTransformation(t);
        }
    }


    private void despawnAllPowerups() {
        if (arenaOrigin.getWorld() == null) return;
        org.bukkit.World w = arenaOrigin.getWorld();

        for (UUID id : new ArrayList<>(powerupByEntity.keySet())) {
            Entity e = w.getEntity(id);
            if (e != null) e.remove();
        }
        powerupByEntity.clear();
    }

    private void trySpawnRandomPowerup() {
        org.bukkit.World w = arenaOrigin.getWorld();
        if (w == null || this.boundingBox == null ||
                !(this.boundingBox instanceof WorldTiedBoundingBox box)) return;

        Location randomLocation = box.getRandomLocation();

        Block air = w.getBlockAt(randomLocation);
        if (!air.getType().isAir()) return;

        Block below = air.getRelative(BlockFace.DOWN);
        Material base = below.getType();
        if (base.isAir() || !base.hasGravity()) return;

        if (isPowerupAlreadyAt(w, randomLocation.getBlockX(),
                randomLocation.getBlockY(), randomLocation.getBlockZ())) return;

        PowerupType type = PowerupType.random();
        spawnPowerupDisplay(w, randomLocation.getBlockX(),
                randomLocation.getBlockY(), randomLocation.getBlockZ(), type);
    }

    private boolean isPowerupAlreadyAt(org.bukkit.World w, int x, int y, int z) {
        Location center = new Location(w, x + 0.5, y + 0.5, z + 0.5);
        for (UUID id : powerupByEntity.keySet()) {
            Entity e = w.getEntity(id);
            if (e == null) continue;
            if (e.getLocation().distanceSquared(center) < 0.5 * 0.5) return true;
        }
        return false;
    }

    private void spawnPowerupDisplay(org.bukkit.World w, int x, int y, int z, PowerupType type) {
        Location loc = new Location(w, x + 0.5, y + 0.75, z + 0.5);
        ItemStack reward = type.createPowerupItem();


        ItemDisplay display = w.spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(reward.clone());
            d.getPersistentDataContainer().set(POWERUP_ID_KEY, PersistentDataType.STRING, type.toString());
            d.setBillboard(Display.Billboard.CENTER);
            Transformation t = d.getTransformation();
            t.getScale().set(0.67f, 0.67f, 0.67f);
            d.setTransformation(t);
        });

        powerupByEntity.put(display.getUniqueId(), type.toString());
        powerupItemByEntity.put(display.getUniqueId(), reward);
    }

    private void tryPickupPowerup(Player player) {
        if (!arenaReady || !decayArmed) return;
        ActiveTNTRunPlayer active = this.roleMap.as(player.getUniqueId(), ActiveTNTRunPlayer.class);
        if (active == null) return;
        org.bukkit.World w = player.getWorld();

        Collection<Entity> nearby = w.getNearbyEntities(player.getBoundingBox().expand(0.5, 0.5, 0.5),
                e -> e instanceof ItemDisplay && powerupByEntity.containsKey(e.getUniqueId()));

        if (nearby.isEmpty()) return;

        for (Entity e : nearby) {
            ItemDisplay d = (ItemDisplay) e;

            String id = d.getPersistentDataContainer().get(POWERUP_ID_KEY, PersistentDataType.STRING);
            if (id == null) continue;

            PowerupType type = PowerupType.fromId(id);
            if (type == null) continue;

            UUID entId = d.getUniqueId();
            ItemStack reward = powerupItemByEntity.remove(entId);
            powerupByEntity.remove(entId);
            d.remove();

            if (reward != null && !reward.getType().isAir()) {
                giveOrDrop(player, reward.clone());
                player.playSound(player, Sound.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUsePowerup(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ActiveTNTRunPlayer active = this.roleMap.as(player.getUniqueId(), ActiveTNTRunPlayer.class);

        if (active == null || !this.decayArmed) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String id = meta.getPersistentDataContainer().get(POWERUP_ID_KEY, PersistentDataType.STRING);

        if (id == null) return;

        PowerupType type = PowerupType.fromId(id);
        if (type == null) return;

        event.setCancelled(true);
        if (!type.handle(this, player)) return;
        consumeOneMainHand(player);
    }

    private void consumeOneMainHand(Player p) {
        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) return;

        inHand.setAmount(inHand.getAmount() - 1);
    }

    private boolean checkAndApplyUpdraftCooldown(Player player) {
        long now = tickCounter;
        long until = updraftCooldownUntilTick.getOrDefault(player.getUniqueId(), 0L);
        if (now < until) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1f, 0.7f);
            return false;
        }
        updraftCooldownUntilTick.put(player.getUniqueId(), now + updraftCooldownTicks);
        return true;
    }

    private static void giveOrDrop(Player player, ItemStack toGive) {
        if (player == null || toGive == null || toGive.getType().isAir() || toGive.getAmount() <= 0) return;

        Util.giveItem(player, toGive);
    }

    private record BlockPos(int x, int y, int z) {}
}
