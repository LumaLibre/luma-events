package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.ManorMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.games.tokenformula.PropHuntTokenFormula;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.BlockDisplayWatcher;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// TODO:
//  - better cues for seekers
//  - debug
public final class PropHunt extends InventoryUnifiedMinigame {

    private static final long STANDARD_DURATION = 300000; // in milliseconds
    private static final long TICK_INTERVAL = 10; // in ticks

    private final PropHuntPlayerMap propHuntPlayers;
    private final Scoreboard<EventPlayer> scoreboard;
    private final PropHuntTokenFormula tokenFormula;
    private final Location spawnLocation;
    private final Location startLocation;

    private CountdownBossBar initialBossBar;
    private CountdownBossBar countdownBossBar;

    public PropHunt(ManorMinigameDefinition def) {
        super("Prop Hunt", "Find all the disguised blocks.", STANDARD_DURATION, TICK_INTERVAL, true, false, false);
        this.propHuntPlayers = new PropHuntPlayerMap();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new PropHuntTokenFormula();
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = this.scoreboard.getScore(participant);
        tokenFormula.giveTokens(participant, score);
        participant.addPermanentScore(MinigameConstant.PROP_HUNT, score);
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.participants) {
            this.propHuntPlayers.put(new Hider(this, participant));
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    Executors.runSync(() -> player.setGameMode(GameMode.SURVIVAL));
                }
            });
        }

        PropHuntPlayer firstSeeker = Util.getRandom(this.propHuntPlayers.values());
        this.propHuntPlayers.swapRole(firstSeeker.getEventPlayer(), () -> new Seeker(this, firstSeeker.getEventPlayer()));
        firstSeeker.getEventPlayer().sendMessage("You are a <yellow>Seeker</yellow>. <red>Find and catch all the Hiders!</red>");

        this.propHuntPlayers.getHiders().forEach(hider -> {
            hider.getEventPlayer().teleportAsync(this.startLocation);
            hider.getEventPlayer().sendMessage("You are a Hider! Disguise yourself as a block and hide from <yellow>" + firstSeeker.getEventPlayer().getName() + "</yellow>!");
        });


        this.initialBossBar = CountdownBossBar.builder()
                .title("<red>Seeker spawns in: %ss")
                .color(BossBar.Color.RED)
                .seconds(15) // TODO: change to 30
                .audience(this.audience)
                .callback(() -> {
                    Preconditions.checkNotNull(this.countdownBossBar, "Countdown boss bar should not be null when initial countdown ends.");
                    this.countdownBossBar.start();
                    firstSeeker.getEventPlayer().teleportAsync(this.startLocation);
                    firstSeeker.getEventPlayer().sendMessage("<red>The game has started. Find and catch all the Hiders!");

                    this.propHuntPlayers.forEach(propHuntPlayer -> {
                        Player player = propHuntPlayer.bukkitPlayer();
                        if (player != null) {
                            // increase volume based on distance from start location
                            float distance = (float) player.getLocation().distance(this.startLocation);
                            // increase by 0.8 for every 10 blocks, cap at 15.0f
                            float volume = Math.min(0.8f + (distance / 10f) * 0.8f, 15.0f);
                            Executors.runSync(() -> {
                                player.playSound(this.startLocation, Sound.ENTITY_WARDEN_EMERGE, volume, 1.0f);
                            });
                        }
                    });
                })
                .build()
                .start();

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<green>Time Left: %ss")
                .color(BossBar.Color.GREEN)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .callback(this::stop)
                .build();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.propHuntPlayers.getHiders().isEmpty()) {
            this.sendAudienceMessage("All Hiders have been caught. Seekers win!");
            this.stop();
            return;
        }

        if (!this.propHuntPlayers.ensureAtLeast(Seeker.class, eventPlayer -> new Seeker(this, eventPlayer), 1, Hider.class)) {
            this.sendAudienceMessage("Not enough players to promote a Seeker. Ending game.");
            this.stop();
            return;
        }

        Executors.runSync(() -> {
            for (PropHuntPlayer propHuntPlayer : this.propHuntPlayers) {
                propHuntPlayer.onTick();
                propHuntPlayer.getEventPlayer().operatePlayer(player -> {
                    if (player.getFoodLevel() < 20) {
                        player.setFoodLevel(20);
                        player.setSaturation(10f);
                    }
                });
            }
        });
    }

    @Override
    protected void handleStop() {
        if (this.initialBossBar != null && !this.initialBossBar.isCancelled()) {
            unsafe(() -> this.initialBossBar.stop(false));
        }
        if (this.countdownBossBar != null && !this.countdownBossBar.isCancelled()) {
            unsafe(() -> this.countdownBossBar.stop(false));
        }

        Executors.runSync(() -> {
            for (PropHuntPlayer propHuntPlayer : this.propHuntPlayers) {
                propHuntPlayer.cleanup();
                propHuntPlayer.getEventPlayer().teleportAsync(this.spawnLocation);
            }
        });
        this.scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.forEach(participant -> participant.teleportAsync(this.spawnLocation));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
                    .seconds(15)
                    .callback(() -> {
                        this.participants.forEach(participant -> {
                            participant.teleportAsync(this.getGameDropOffLocation());
                            participant.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        PropHuntPlayer propHuntPlayer = this.propHuntPlayers.remove(participant.getUuid());
        if (propHuntPlayer != null) {
            propHuntPlayer.cleanup();
        }
        return super.removeParticipant(participant);
    }


    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        Block clickedBlock = event.getClickedBlock();


        PropHuntPlayer propHuntPlayer = this.propHuntPlayers.get(event.getPlayer().getUniqueId());

        if (propHuntPlayer == null) return;

        Entity clickedEntity = event.getPlayer().getTargetEntity(6);
        if (clickedEntity instanceof Player p) {
            Hider hider = this.propHuntPlayers.as(p.getUniqueId(), Hider.class);
            if (hider != null && !hider.isLocked()) {
                propHuntPlayer.onEntityInteract(event, p, hider);
                return;
            }
        }

        if (clickedBlock != null) {
            propHuntPlayer.onBlockInteract(event, clickedBlock);
        }
    }



    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();

        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        Seeker seeker = this.propHuntPlayers.as(attacker.getUniqueId(), Seeker.class);
        Hider hider = this.propHuntPlayers.as(victim.getUniqueId(), Hider.class);

        if (seeker == null || hider == null) {
            Util.sendMsg(attacker, "You cannot damage that entity.");
            event.setCancelled(true);
        } else {
            // Always instantly kill hiders when damaged by seekers
            event.setDamage(40);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();

        PropHuntPlayer propHuntPlayer = this.propHuntPlayers.get(event.getEntity().getUniqueId());
        if (propHuntPlayer == null) return;

        event.setCancelled(true);

        if (!(propHuntPlayer instanceof Hider hider)) {
            return;
        }

        // I miss nested kotlin functions :(
        String preventedDeath = "Your death was prevented because you were not killed by a seeker.";

        if (!(event.getDamageSource().getCausingEntity() instanceof Player bukkitAttacker)) {
            propHuntPlayer.getEventPlayer().sendMessage(preventedDeath);
            return;
        }

        Seeker seeker = this.propHuntPlayers.as(bukkitAttacker.getUniqueId(), Seeker.class);
        if (seeker == null) {
            propHuntPlayer.getEventPlayer().sendMessage(preventedDeath);
            return;
        }

        seeker.catchHider(hider);
    }

    // Hider locking and unlocking

    @EventHandler
    public void onPlayerDismount(EntityDismountEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player player)) return;

        Hider hider = this.propHuntPlayers.as(player.getUniqueId(), Hider.class);
        if (hider == null) return;
        hider.unlock();
    }

    //@EventHandler
    public void onPlayerCrouch(PlayerToggleSneakEvent event) {
        this.ensureNotIllegal();
        if (event.isSneaking()) return;

        Hider hider = this.propHuntPlayers.as(event.getPlayer().getUniqueId(), Hider.class);
        if (hider != null && !hider.isLocked()) {
            hider.lock();
        }
    }


    @Getter
    @RequiredArgsConstructor
    private static abstract class PropHuntPlayer {

        protected static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 250, 0, true, true, true);

        protected final PropHunt context;
        protected final EventPlayer eventPlayer;
        protected boolean hidden;

        public abstract void cleanup();
        public abstract void onTick();
        public abstract void onBlockInteract(PlayerInteractEvent event, Block clickedBlock);
        public abstract void onEntityInteract(PlayerInteractEvent event, Player clickedEntity, Hider hider);

        @Nullable
        public Player bukkitPlayer() {
            return this.eventPlayer.getPlayer();
        }

        @SafeVarargs
        public final void hide(Class<? extends PropHuntPlayer>... fromTypes) {

            for (PropHuntPlayer other : context.propHuntPlayers.getPlayers(fromTypes)) {
                Player otherPlayer = other.getEventPlayer().getPlayer();
                Player selfPlayer = this.getEventPlayer().getPlayer();
                if (otherPlayer != null && selfPlayer != null) {
                    Executors.runSync(() -> otherPlayer.hidePlayer(EventMain.getInstance(), selfPlayer));
                }
            }
            this.hidden = true;
        }

        @SafeVarargs
        public final void show(Class<? extends PropHuntPlayer>... toTypes) {

            for (PropHuntPlayer other : context.propHuntPlayers.getPlayers(toTypes)) {
                Player otherPlayer = other.getEventPlayer().getPlayer();
                Player selfPlayer = this.getEventPlayer().getPlayer();
                if (otherPlayer != null && selfPlayer != null) {
                    Executors.runSync(() -> otherPlayer.showPlayer(EventMain.getInstance(), selfPlayer));
                }
            }
            this.hidden = false;
        }
    }

    @Getter
    private static class Hider extends PropHuntPlayer {

        private static final int DISGUISE_COOLDOWN = 450; // in ticks
        private static final int SCOREBOARD_POINT_INTERVAL = 800;

        private MiscDisguise disguise = null;
        private ArmorStand lockStand = null;
        private Block lockedBlock = null;
        private Material material = null;

        private int disguiseCooldownCounter = 0;
        private int scoreboardPointCounter = 0;

        public Hider(PropHunt context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
        }

        @Override
        public void cleanup() {
            this.removeBlockDisguise();
            this.show(PropHuntPlayer.class);
        }

        @Override
        public void onTick() {
            Player player = this.bukkitPlayer();
            if (player == null) return;
            if (this.isLocked()) {
                player.addPotionEffect(INVISIBILITY);
            }

            String msg = "<green>You can disguise yourself by left-clicking a block.";

            if (this.disguiseCooldownCounter > 0) {
                this.disguiseCooldownCounter = Math.max(0, this.disguiseCooldownCounter - (int) TICK_INTERVAL);
            }

            if (!this.isLocked() && this.material != null) {
                msg = "<green>You are disguised as a <yellow>" + Util.formatMaterialName(this.material.toString()) + "</yellow> block. Right-click to lock into place.";
            } else if (this.disguiseCooldownCounter > 0 && this.isLocked()) {
                msg = "<yellow>Disguise Cooldown: <red>" + Util.ticksToSecs(this.disguiseCooldownCounter) + "s</red>";
            }

            this.scoreboardPointCounter += (int) TICK_INTERVAL;
            if (this.scoreboardPointCounter >= SCOREBOARD_POINT_INTERVAL) {
                this.scoreboardPointCounter = 0;
                this.context.scoreboard.addScore(this.getEventPlayer(), 1);
            }

            this.getEventPlayer().sendActionBar(msg);

            // particles
            if (!this.isLocked()) {
                return;
            }

            Location location;
            BlockData blockData;
            if (this.lockedBlock != null) {
                location = this.lockedBlock.getLocation().toCenterLocation();
                blockData = this.lockedBlock.getBlockData();
            } else {
                location = player.getLocation();
                blockData = Material.STONE.createBlockData();

                Logging.errorLog("Locked block is inappropriately null for locked Hider " + this.getEventPlayer().getName());
                Thread.dumpStack();
            }

            location.getWorld().spawnParticle(Particle.BLOCK, location, 1, 0.6, 0.5, 0.6, 0.1, blockData);
        }

        @Override
        public void onBlockInteract(PlayerInteractEvent event, Block clickedBlock) {
            if (event.getAction().isRightClick()) {
                // moved
                Hider hider = this.context.propHuntPlayers.as(event.getPlayer().getUniqueId(), Hider.class);
                if (hider != null && !hider.isLocked()) {
                    hider.lock();
                }
                return;
            } else if (this.disguiseCooldownCounter > 0) {
                this.getEventPlayer().sendMessage("You must wait <red>" + Util.ticksToSecs(this.disguiseCooldownCounter) + "s</red> before disguising again.");
                return;
            }

            Material type = clickedBlock.getType();
            if (type.isSolid()) {
                this.disguiseAsBlock(clickedBlock.getType());
                this.disguiseCooldownCounter = DISGUISE_COOLDOWN;

                this.getEventPlayer().sendMessage("You have disguised yourself as a <yellow>" + Util.formatMaterialName(clickedBlock.getType().toString()) + "</yellow> block.");
            } else {
                this.getEventPlayer().sendMessage("You can only disguise as solid blocks.");
            }
        }

        @Override
        public void onEntityInteract(PlayerInteractEvent event, Player clickedEntity, Hider hider) {
            // No op

        }

        public void disguiseAsBlock(Material material) {
            if (this.isLocked()) {
                this.unlock();
            }

            Player player = this.bukkitPlayer();
            if (player == null) return;
            this.material = material;
            MiscDisguise disguise = new MiscDisguise(DisguiseType.BLOCK_DISPLAY);
            BlockDisplayWatcher watcher = (BlockDisplayWatcher) disguise.getWatcher();
            watcher.setBlock(material.createBlockData());
            disguise.setEntity(player);
            disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
            disguise.setViewSelfDisguise(true);
            disguise.setHidePlayer(false);
            //disguise.setScalePlayerToDisguise(true);
            //disguise.setVelocitySent(false);
            disguise.startDisguise();
            this.disguise = disguise;
        }

        public void removeBlockDisguise() {
            if (this.isLocked()) {
                this.unlock();
            }

            if (this.disguise != null) {
                this.disguise.stopDisguise();
                this.disguise = null;
            }
            this.material = null;
        }

        /**
         * Locks the player in place using an invisible armor stand.
         * @return true if the player was successfully locked, false if they were already locked.
         */
        public boolean lock() {
            Preconditions.checkState(lockStand == null || !lockStand.isValid(), "Lock stand should be null when locking.");
            Preconditions.checkNotNull(material, "Material should not be null when locking.");

            Player player = this.bukkitPlayer();
            if (player == null) return false;
            Block block = player.getLocation().getBlock();
            block.setType(material);

            ArmorStand stand = player.getWorld().spawn(this.tryFindBestAirPocket(block), ArmorStand.class);
            stand.setVisible(false);
            stand.setSmall(true);
            stand.setInvisible(true);
            stand.addDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
            stand.setCanMove(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setPersistent(false);
            stand.addPassenger(player);


            if (this.disguise != null) {
                this.disguise.stopDisguise(); // Just stop the disguise since we're locking in place
            }
            // Hide the player from all seekers when locked
            this.hide(Seeker.class);

            this.getEventPlayer().sendMessage("You have locked yourself in place as a block. Sneak to unlock.");
            this.lockStand = stand;
            this.lockedBlock = block;
            return true;
        }

        public boolean unlock() {
            Player player = this.bukkitPlayer();
            Preconditions.checkNotNull(lockStand, "Lock stand should not be null when unlocking.");
            Preconditions.checkNotNull(player, "Player should not be null when unlocking.");

            player.leaveVehicle();
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            if (lockedBlock != null) {
                player.teleportAsync(lockedBlock.getLocation().toCenterLocation());
                lockedBlock.setType(Material.AIR);
            }

            if (lockStand != null) {
                lockStand.remove();
            }

            if (this.disguise != null) {
                this.disguise.startDisguise(); // Reapply the disguise
            }
            // Show the player to all seekers when unlocked
            this.show(Seeker.class);
            this.lockStand = null;
            this.lockedBlock = null;
            return true;
        }


        public boolean isLocked() {
            return lockStand != null && lockStand.isValid();
        }

        // FIXME
        public boolean isDisguised() {
            return disguise != null;
        }

        private Block centerBlock(Player player) {
            return player.getLocation().add(0, 1, 0).getBlock();
        }

        private Location tryFindBestAirPocket(Block origin) {
            // Try to find the nearest air block. The top block is preferred, then sides, then bottom.
            BlockFace[] faces = {
                    BlockFace.UP,
                    BlockFace.NORTH,
                    BlockFace.EAST,
                    BlockFace.SOUTH,
                    BlockFace.WEST,
                    BlockFace.DOWN
            };
            for (BlockFace face : faces) {
                Block adjacent = origin.getRelative(face);
                if (adjacent.isEmpty() && adjacent.getRelative(BlockFace.UP).isEmpty()) {
                    return adjacent.getLocation().toCenterLocation();
                }
            }
            return origin.getLocation().toCenterLocation();
        }
    }

    private static class Seeker extends PropHuntPlayer {

        private static final int SEEKER_CLUE_INTERVAL = 450; // in ticks
        private static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 250, 0, true, false, true);
        private static final ItemStack STONE_SWORD = new ItemStack(Material.STONE_SWORD);

        private int kills;
        private int clueTickCounter;

        public Seeker(PropHunt context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
            this.getEventPlayer().operatePlayer(player -> {
                player.getInventory().setItemInMainHand(STONE_SWORD);
            });
        }

        @Override
        public void cleanup() {
            Player player = this.bukkitPlayer();
            if (player != null) {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.getInventory().setItemInMainHand(null);
            }
        }

        @Override
        public void onTick() {
            Player player = this.bukkitPlayer();
            if (player == null) {
                return;
            }

            player.addPotionEffect(SPEED);

            this.getEventPlayer().sendActionBar("<yellow>Kills: <green>" + this.kills + "</green> <gray>|</gray> Next clue in: <green>" + Util.ticksToSecs(SEEKER_CLUE_INTERVAL - this.clueTickCounter) + "s</green>");

            this.clueTickCounter += (int) TICK_INTERVAL;
            if (this.clueTickCounter >= SEEKER_CLUE_INTERVAL) {
                this.clueTickCounter = 0;
                // TODO: Volume scale based on distance to closest hider
                this.closestHider(
                        (closest, closestPlayer) -> player.playSound(closestPlayer.getLocation(), Sound.ENTITY_WARDEN_LISTENING, 0.8f, 1.0f),
                        (other, otherPlayer) -> player.playSound(otherPlayer.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 2.0f, 1.0f)
                );
            }
        }

        @Override
        public void onBlockInteract(PlayerInteractEvent event, Block clickedBlock) {
            if (!event.getAction().isLeftClick()) return;
            Player attacker = event.getPlayer();

            Seeker seeker = this.context.propHuntPlayers.as(attacker.getUniqueId(), Seeker.class);
            Hider hider = this.context.propHuntPlayers.fromLockedBlock(clickedBlock);

            if (seeker == null || hider == null) return;

            hider.unlock();

            // Artificially damage the hider
            Player victim = hider.bukkitPlayer();
            if (victim != null) {
                attacker.attack(victim);
            }
        }

        @Override
        public void onEntityInteract(PlayerInteractEvent event, Player victim, Hider hider) {
            event.getPlayer().attack(victim);
        }

        public void catchHider(@NotNull Hider hider) {
            EventPlayer hiderEventPlayer = hider.getEventPlayer();
            this.context.propHuntPlayers.swapRole(hider, () -> new Spectator(this.context, hiderEventPlayer));
            hiderEventPlayer.teleportAsync(this.context.startLocation);
            hiderEventPlayer.sendMessage("You have been caught by " + this.getEventPlayer().getName() + "! You are now a Spectator.");

            // TODO: Announce catch, give points, etc.
            this.context.sendAudienceMessage(this.getEventPlayer().getName() + " has caught " + hiderEventPlayer.getName()+ "!");

            this.kills++;
            int amt = Util.RANDOM.nextBoolean() ? 1 : 2;
            this.context.scoreboard.addScore(this.getEventPlayer(), amt);

            // Add 15 seconds to the game time for each catch
            this.context.sendAudienceMessage("15 seconds have been added to the game time!");
            this.context.setDuration(this.context.getDuration() + Util.secsToMillis(15));
            this.context.countdownBossBar.addSeconds(15);
        }


        public void closestHider(BiConsumer<Hider, Player> closest, BiConsumer<Hider, Player> other) {
            Player seekerPlayer = this.bukkitPlayer();
            if (seekerPlayer == null) return;

            double closestDistance = Double.MAX_VALUE;

            Hider closestHider = null;

            for (Hider hider : this.context.propHuntPlayers.getHiders()) {
                Player hiderPlayer = hider.bukkitPlayer();
                if (hiderPlayer == null) continue;

                double distance = seekerPlayer.getLocation().distanceSquared(hiderPlayer.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHider = hider;
                }
            }

            for (Hider hider : this.context.propHuntPlayers.getHiders()) {
                Player hiderPlayer = hider.bukkitPlayer();
                if (hiderPlayer == null) continue;

                if (hider == closestHider) {
                    closest.accept(hider, hiderPlayer);
                } else {
                    other.accept(hider, hiderPlayer);
                }
            }
        }
    }

    private static class Spectator extends PropHuntPlayer {

        // TODO: Spectator flight?

        public Spectator(PropHunt context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
            Executors.runSync(() -> {
                this.hide(Seeker.class, Hider.class);
            });
        }

        @Override
        public void cleanup() {
            this.show(PropHuntPlayer.class);
            Player player = this.bukkitPlayer();
            if (player != null) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        @Override
        public void onTick() {
            Player player = this.bukkitPlayer();
            if (player != null) {
                player.addPotionEffect(INVISIBILITY);
            }
            this.getEventPlayer().sendActionBar("<yellow>You are spectating. <gray>|</gray> <aqua>Use <blue>/event quit</blue> to quit.");
        }

        @Override
        public void onBlockInteract(PlayerInteractEvent event, Block clickedBlock) {

        }

        @Override
        public void onEntityInteract(PlayerInteractEvent event, Player clickedEntity, Hider hider) {

        }
    }



    private static class PropHuntPlayerMap extends HashMap<UUID, PropHuntPlayer> implements Iterable<PropHuntPlayer> {


        public PropHuntPlayer put(PropHuntPlayer value) {
            return super.put(value.getEventPlayer().getUuid(), value);
        }

        public List<Hider> getHiders() {
            return this.values().stream()
                    .filter(player -> player instanceof Hider)
                    .map(player -> (Hider) player)
                    .toList();
        }

        public List<Seeker> getSeekers() {
            return this.values().stream()
                    .filter(player -> player instanceof Seeker)
                    .map(player -> (Seeker) player)
                    .toList();
        }

        public List<Spectator> getSpectators() {
            return this.values().stream()
                    .filter(player -> player instanceof Spectator)
                    .map(player -> (Spectator) player)
                    .toList();
        }

        public List<PropHuntPlayer> getPlayers(Class<? extends PropHuntPlayer>... types) {
            if (Arrays.stream(types).anyMatch(type -> type == PropHuntPlayer.class)) {
                return this.values().stream().toList();
            }
            return this.values().stream()
                    .filter(player -> Util.isAssignableFromAny(player.getClass(), types))
                    .toList();
        }

        @Nullable
        public Hider fromLockedBlock(Block block) {
            for (PropHuntPlayer player : this.values()) {
                if (player instanceof Hider hider) {
                    if (hider.getLockedBlock() != null && hider.getLockedBlock().equals(block)) {
                        return hider;
                    }
                }
            }
            return null;
        }

        @Nullable
        public <T extends PropHuntPlayer> T as(UUID uuid, Class<T> type) {
            PropHuntPlayer player = this.get(uuid);
            if (type.isInstance(player)) {
                return type.cast(player);
            }
            return null;
        }

        public <T extends PropHuntPlayer> T swapRole(PropHuntPlayer propHuntPlayer, Supplier<? extends PropHuntPlayer> newRoleSupplier) {
            return this.swapRole(propHuntPlayer.getEventPlayer(), newRoleSupplier);
        }

        public <T extends PropHuntPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends PropHuntPlayer> newRoleSupplier) {
            PropHuntPlayer currentRole = this.get(eventPlayer.getUuid());
            if (currentRole != null) {
                Executors.runSync(() -> {
                    currentRole.cleanup();
                });
            }
            PropHuntPlayer newRole = newRoleSupplier.get();
            this.put(newRole.getEventPlayer().getUuid(), newRole);
            return (T) newRole;
        }

        /**
         * Ensures that there are at least {@code count} players of the specified {@code type}.
         * If there are not enough players of that type, it will promote players from the provided {@code fromTypes}.
         *
         * @param type The type of player to ensure.
         * @param newRoleConsumer A consumer that creates a new instance of the specified type.
         * @param count The minimum number of players of the specified type.
         * @param fromTypes The types of players to promote from.
         * @return true if the requirement was met or successfully promoted, false otherwise.
         */
        @SafeVarargs
        private <T extends PropHuntPlayer, S extends PropHuntPlayer> boolean ensureAtLeast(Class<T> type, NewRoleConsumer<T> newRoleConsumer, int count, Class<S>... fromTypes) {
            long currentCount = this.values().stream()
                    .filter(type::isInstance)
                    .count();
            if (currentCount >= count) {
                return true;
            }
            int difference = count - (int) currentCount;
            List<PropHuntPlayer> candidates = this.values().stream()
                    .filter(player -> Util.isAssignableFromAny(player.getClass(), fromTypes))
                    .toList();

            if (candidates.size() < difference) {
                return false; // Not enough candidates to promote
            }

            for (int i = 0; i < difference; i++) {
                PropHuntPlayer candidate = Util.getRandom(candidates);
                this.swapRole(candidate, () -> newRoleConsumer.accept(candidate.getEventPlayer()));
            }
            return true;
        }

        @NotNull
        @Override
        public Iterator<PropHuntPlayer> iterator() {
            return this.values().iterator();
        }

        @FunctionalInterface
        private interface NewRoleConsumer<T extends PropHuntPlayer> {
            T accept(EventPlayer eventPlayer);
        }
    }
}
