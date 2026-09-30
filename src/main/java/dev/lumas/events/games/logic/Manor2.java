package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.Manor2Definition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseEffects;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Manor2 extends InventoryUnifiedMinigame {
    private static final String[] SPLASH = {
            "Are you afraid of ghosts?",
            "Boooooooooooo!!!!",
            "Ghost Busters!",
            "Who you gonna call?",
            "House of Dracula!",
            "Trick or treat?",
            "Here's Johny!!!",
            "Put these foolish ambitions to rest",
            "Go eat candy instead :(",
            "Wazzzuup!",
            "Did you pray enough?",
            "It's alive! It's alive!",
            "Be afraid. Be very afraid",
            "What is the color of night?",
            "This is not a dream!",
            "Someone must extinguish thy flame."
    };
    private static final long TICK_INTERVAL = 1L;
    private static final int TELEPORTS_PER_TICK = 5;
    private static final long END_DELAY_SECONDS = 10;
    private static final NamespacedKey TAGGED = new NamespacedKey(EventMain.getInstance(), "manor2/item_tag");
    private final Location lobbyLocation;
    private final List<Location> huntersStartLocations;
    private final List<Location> ghostStartLocations;
    private final int playerRatio;
    private final MinigameRoleMap<Manor2Role> roleMap = new MinigameRoleMap<>();
    private final int ghostRevealTime;
    private final int hunterDeathTime;
    private final LocalizedFakeBlockCache blockCache = new LocalizedFakeBlockCache(32);
    private final Map<Player, Flashlight> flashlights = new HashMap<>();
    private CountdownBossBar timer;

    public Manor2(Manor2Definition definition) {
        super("Manor 2", Util.getRandom(SPLASH), definition.getDuration(), TICK_INTERVAL, true, true, false, false);
        this.lobbyLocation = definition.getLobby();
        this.huntersStartLocations = definition.getHunterStartLocations();
        this.ghostStartLocations = definition.getGhostStartLocations();
        this.playerRatio = definition.getHunterRatio();
        this.ghostRevealTime = definition.getGhostRevealTime();
        this.hunterDeathTime = definition.getHunterDeathTime();
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    @Override
    protected void handleStart() {
        Collections.shuffle(participants);
        int ghostsToSpecify = Math.ceilDiv(participants.size(), playerRatio);
        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i).getPlayer();
            if (player == null) {
                continue;
            }
            Manor2Role role;
            List<Location> teleportTargets;
            if (ghostsToSpecify-- > 0) {
                role = new Ghost(participants.get(i));
                teleportTargets = ghostStartLocations;
            } else {
                role = new GhostHunter(participants.get(i));
                teleportTargets = huntersStartLocations;
            }
            roleMap.put(role);
            Location target = teleportTargets.get(i % teleportTargets.size());
            Executors.delayedSync(player, Math.floorDiv(i, TELEPORTS_PER_TICK) + 1, () -> {
                        Executors.teleportSafely(player, target);
                        role.initiatePlay();
                    }
            );
        }
        Bukkit.getRegionScheduler().runAtFixedRate(
                EventMain.getInstance(),
                huntersStartLocations.getFirst(),
                scheduledTask -> {
                    if (stopping) {
                        scheduledTask.cancel();
                        return;
                    }
                    blockCache.showSync();
                }, 1L, 1L);

        timer = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
                .miliseconds(getDuration())
                .audience(audience)
                .build();
        timer.start();
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(lobbyLocation);
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer player, boolean doTeleport) {
        if (player.getPlayer() != null && this.timer != null) {
            this.timer.getBossBar().removeViewer(player.getPlayer());
        }
        return super.removeParticipant(player, doTeleport);
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.stopping) return;
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.handleStop();
        }
        for (Manor2Role role : roleMap) {

            role.operatePlayer(player -> {
                Flashlight flashlight = flashlights.get(player);
                if (flashlight != null) {
                    flashlight.tick();
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 0, false, false, false));
            });

            if (role instanceof GhostHunter ghostHunter) {
                ghostHunter.getEventPlayer().operatePlayer(player -> {

                });
            } else {

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (!Flashlight.isFlashlight(event.getItem())) return;
        Flashlight flashlight = flashlights.get(event.getPlayer());
        if (flashlight != null) flashlight.startUsing();
    }

    @EventHandler
    public void onTridentThrow(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof Trident)) return;
        if (!Flashlight.isFlashlight(event.getItemStack())) return;
        event.setCancelled(true);
        Flashlight flashlight = flashlights.get(event.getPlayer());
        if (flashlight != null) flashlight.stopUsing();
    }

    @Override
    protected void handleStop() {
        if (timer != null) timer.stop(false);

        Bukkit.getRegionScheduler().run(
                EventMain.getInstance(),
                huntersStartLocations.getFirst(),
                ignore -> blockCache.shutdown()
        );
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(role.retire());
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .join();

    }

    private abstract class Manor2Role extends MinigameRole {

        protected Manor2Role(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        public CompletableFuture<Void> retire() {
            CompletableFuture<Void> completed = new CompletableFuture<>();
            this.operatePlayer(player -> {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) instanceof ItemStack itemStack && Util.hasPersistentKey(itemStack, TAGGED)) {
                        inventory.setItem(i, null);
                    }
                }
                player.clearActivePotionEffects();
                completed.complete(null);
            });
            return completed;
        }

        public abstract void die();

        public abstract CompletableFuture<Void> initiatePlay();
    }

    private class Ghost extends Manor2Role {

        private int revealedTime = 0;
        private volatile boolean revealed = false;

        protected Ghost(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        @Override
        public void die() {

        }

        private void showRevealedResult() {
            eventPlayer.operatePlayer(player -> {
                AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    double newHealth = maxHealth.getValue() * (1 - (double) revealedTime / ghostRevealTime);
                    player.setHealth(newHealth);
                } else {
                    EventMain.getInstance().getLogger().warning("Player has no max health attribute - can not take damage");
                }
            });
        }

        @Override
        public CompletableFuture<Void> initiatePlay() {

            // testing
            eventPlayer.operatePlayer(player -> {
                Flashlight flashlight = new Flashlight(player);
                player.getInventory().setItem(0, flashlight.getItem());
                flashlights.put(player, flashlight);
            });

            CompletableFuture<Void> future = new CompletableFuture<>();
            eventPlayer.operatePlayer(player -> {
                //player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0));
                future.complete(null);
            });
            return future;
        }

        public void reveal() {
            eventPlayer.operatePlayer(player -> player.setInvisible(false));
            revealed = true;
        }
    }

    private class GhostHunter extends Manor2Role {

        private int ghostConsumedTime = 0;

        protected GhostHunter(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        @Override
        public void die() {

        }

        @Override
        public CompletableFuture<Void> initiatePlay() {
            eventPlayer.operatePlayer(player -> {
                Flashlight flashlight = new Flashlight(player);
                player.getInventory().setItem(0, flashlight.getItem());
                flashlights.put(player, flashlight);
            });
            return null;
        }

        public void consumeTick(Ghost attacker) {

        }
    }

    private class Spectator extends Manor2Role {

        protected Spectator(EventPlayer eventPlayer) {
            super(eventPlayer);
        }

        @Override
        public void die() {

        }

        @Override
        public CompletableFuture<Void> initiatePlay() {
            return null;
        }
    }

    private class Flashlight {

        private static final int MIN_ILLUMINANCE = 4;
        private static final int MAX_ILLUMINANCE = 12;
        private static final float FLICKER_START = 25f;
        private static final float HEAVY_FLICKER_START = 10f;
        private static final float MAX_BATTERY_CHARGE = 100f;
        private static final float BATTERY_DRAIN_PER_TICK = 100f / (20f * 120f);
        private static final int LIGHT_BLOCK_LIFETIME = 4;
        private static final int FLASHLIGHT_DISTANCE = 8;

        private final Player player;

        @Getter
        private float batteryCharge = 50f; // TODO: set to 100% after testing

        @Getter
        private boolean active = false;

        private int flickerTicksRemaining = 0;
        private boolean flickerOff = false;

        public Flashlight(Player player) {
            this.player = player;
        }

        public void startUsing() {
            if (batteryCharge <= 0f) return;
            if (!isHeld()) return;
            active = true;
        }

        public void stopUsing() {
            active = false;
        }

        public void tick() {
            if (!active) return;

            if (!isHeld()) {
                stopUsing();
                return;
            }

            drainBattery();
            updateItem();

            if (batteryCharge <= 0f) {
                stopUsing();
                return;
            }

            int illuminance = getFlickeringIlluminance();
            if (illuminance <= 0) return;
            illuminate(illuminance);
        }

        private void drainBattery() {
            batteryCharge = Math.max(batteryCharge - BATTERY_DRAIN_PER_TICK, 0f);
        }

        private boolean isHeld() {
            return isFlashlight(player.getInventory().getItemInMainHand());
        }

        private int getBaseIlluminance() {
            if (batteryCharge <= 0f) return 0;
            double percentage = batteryCharge / MAX_BATTERY_CHARGE;
            double brightness = Math.sqrt(percentage);
            return MIN_ILLUMINANCE + (int) Math.round((MAX_ILLUMINANCE - MIN_ILLUMINANCE) * brightness);
        }

        private int getFlickeringIlluminance() {
            int base = getBaseIlluminance();
            if (base <= 0) return 0;

            float chargePercent = batteryCharge / MAX_BATTERY_CHARGE * 100f;
            if (chargePercent > FLICKER_START) {
                flickerTicksRemaining = 0;
                flickerOff = false;
                return base;
            }

            if (flickerTicksRemaining > 0) {
                flickerTicksRemaining--;
                if (flickerOff) return 0;
                return Math.max(base - 3, MIN_ILLUMINANCE);
            }

            double severity = 1.0 - chargePercent / FLICKER_START;
            double startChance = 0.01 + severity * 0.1;

            if (Math.random() < startChance) {
                flickerTicksRemaining = 1 + (int) Math.round(severity * 5);

                if (chargePercent <= HEAVY_FLICKER_START) {
                    flickerOff = Math.random() < 0.6;
                } else {
                    flickerOff = Math.random() < 0.2;
                }

                if (flickerOff) return 0;
                return Math.max(base - 2, MIN_ILLUMINANCE);
            }

            return base;
        }

        private void illuminate(int illuminance) {
            Location location = player.getLocation();
            Location head = location.toCenterLocation().add(0, 1, 0);
            Set<Location> alreadyPlaced = new HashSet<>();

            Location headBlock = head.toBlockLocation();
            if (headBlock.getBlock().getType().isAir()) {
                alreadyPlaced.add(headBlock);
                blockCache.setBlock(headBlock, BlockType.LIGHT.createBlockData(light -> light.setLevel(illuminance)), LIGHT_BLOCK_LIFETIME);
            }

            int firstHit = 0;

            for (int i = 0; i < FLASHLIGHT_DISTANCE * 2; i++) {
                Location posToCheck = head.clone().add(location.getDirection().clone().multiply((double) i / 2));
                if (posToCheck.getBlock().getType().isOccluding()) break;
                firstHit = i;
            }

            if (firstHit == 0) return;

            for (int i = firstHit; i > 0; i--) {
                int lightLevel = Math.floorDiv(i * illuminance, firstHit);
                if (lightLevel <= 0) continue;

                Location lightLoc = head.clone().add(location.getDirection().clone().multiply((double) i / 2)).toBlockLocation();
                Block block = lightLoc.getBlock();

                if (block.getType().isAir()) {
                    if (alreadyPlaced.contains(lightLoc)) continue;
                    alreadyPlaced.add(lightLoc);
                    blockCache.setBlock(lightLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
                }
                else if (!block.getType().isOccluding() && block.getRelative(BlockFace.UP).getType().isAir()) {
                    Location newLoc = lightLoc.clone().add(0, 1, 0);
                    if (!alreadyPlaced.add(newLoc)) continue;
                    blockCache.setBlock(newLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
                }
            }
        }

        private void updateItem() {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isFlashlight(item)) return;
            item.setData(DataComponentTypes.DAMAGE, 100 - Math.round(batteryCharge));
        }

        public ItemStack getItem() {
            ItemStack item = ItemStack.of(Material.TRIDENT);
            item.editMeta(meta -> {
                meta.setItemModel(NamespacedKey.minecraft("spyglass"));
                meta.displayName(Util.color("<gold><bold>Flashlight"));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            });
            item.editPersistentDataContainer(pdc -> {
                pdc.set(TAGGED, PersistentDataType.STRING, "flashlight");
            });
            item.setData(DataComponentTypes.MAX_DAMAGE, 100);
            item.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                    .canSprint(true)
                    .speedMultiplier(1.0f)
                    .interactVibrations(true)
                    .build());
            item.setData(DataComponentTypes.DAMAGE, 100 - Math.round(batteryCharge));
            return item;
        }

        public static boolean isFlashlight(ItemStack item) {
            if (item == null || item.isEmpty()) return false;
            String tag = item.getPersistentDataContainer().get(TAGGED, PersistentDataType.STRING);
            return "flashlight".equals(tag);
        }

    }

}
