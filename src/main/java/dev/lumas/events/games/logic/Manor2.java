package dev.lumas.events.games.logic;

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
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Manor2 extends InventoryUnifiedMinigame {
    private static final String[] SPLASH = {
            "Are you afraid of ghosts?",
            "Boooooooooooo!!!!",
            "Ghost Busters!",
            "Who you gonna call?",
            "House of Dracula!"
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
    private static final int LIGHT_BLOCK_LIFETIME = 4;
    private static final int FLASHLIGHT_DISTANCE = 8;
    private static final int ILLUMINANCE = 8;
    private final LocalizedFakeBlockCache blockCache = new LocalizedFakeBlockCache(32);
    private CountdownBossBar timer;

    public Manor2(Manor2Definition definition) {
        super("Manor 2", Util.getRandom(SPLASH), definition.getDuration(), TICK_INTERVAL, true, true, true, false);
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
            if (role instanceof GhostHunter ghostHunter) {
                ghostHunter.getEventPlayer().operatePlayer(player -> {
                    illuminateSync(player.getLocation(), ghostHunter);
                });
            } else {

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

    }

    @Override
    protected void handleStop() {
        if (timer != null) timer.stop(false);
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(role.retire());
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .join();
    }

    private void illuminateSync(Location location, GhostHunter manor2Role) {
        Location head = location.toCenterLocation().add(0, 1, 0);
        Set<Location> alreadyPlaced = new HashSet<>();
        int firstHit = 0;
        for (int i = 0; i < FLASHLIGHT_DISTANCE * 2; i++) {
            Location posToCheck = head.clone().add(location.getDirection().clone().multiply((double) i / 2));
            if (posToCheck.getBlock().getType().isOccluding()) {
                break;
            }
            firstHit = i;
        }
        for (int i = firstHit; i > 0; i--) {
            int lightLevel = Math.floorDiv(i * ILLUMINANCE, firstHit);
            Location lightLoc = head.clone().add(location.getDirection().clone().multiply((double) i / 2)).toBlockLocation();
            Block block = lightLoc.getBlock();
            if (block.getType().isAir()) {
                if (alreadyPlaced.contains(lightLoc)) {
                    continue;
                }
                alreadyPlaced.add(lightLoc);
                blockCache.setBlock(lightLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
            } else if (!block.getType().isOccluding() && block.getRelative(BlockFace.UP).getType().isAir()) {
                Location newLoc = lightLoc.clone().add(0, 1, 0);
                if (alreadyPlaced.contains(newLoc)) {
                    continue;
                }
                blockCache.setBlock(newLoc, BlockType.LIGHT.createBlockData(light -> light.setLevel(lightLevel)), LIGHT_BLOCK_LIFETIME);
                alreadyPlaced.add(lightLoc);
            }
        }
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
            CompletableFuture<Void> future = new CompletableFuture<>();
            eventPlayer.operatePlayer(player -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0));
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

}
