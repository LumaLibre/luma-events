package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumacore.utility.Text;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TowersMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.items.LocalCustomItemManager;
import dev.jsinco.luma.lumaevents.items.TowersItemNestItem;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.manager.CustomItem;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class Towers extends InventoryUnifiedMinigame {

    public static final int ROUND_DURATION = 450;
    private static final int MAX_ROUNDS = 2;
    private static final int TICK_INTERVAL = 2;

    private static final Map<CreativeCategory, Integer> creativeCategoryWeights = Map.of(
            CreativeCategory.BUILDING_BLOCKS, 80,
            CreativeCategory.DECORATIONS, 20,
            CreativeCategory.MISC, 30,
            CreativeCategory.COMBAT, 15,
            CreativeCategory.FOOD, 10,
            CreativeCategory.TRANSPORTATION, 4
    );


    private final TowersPlayerMap towersPlayers;
    private final WorldTiedBoundingBox outerRegion;
    private final Location spawnLocation;

    private CountdownBossBar newItemTimer;

    @Getter
    private int ticksElapsed;
    private int round;

    public Towers(TowersMinigameDefinition def) {
        super("Towers", "No Description", (ROUND_DURATION * MAX_ROUNDS * 2000), TICK_INTERVAL, false, true, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.outerRegion = WorldTiedBoundingBox.of(def.getOuterRegion().getLoc1(), def.getOuterRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.towersPlayers = new TowersPlayerMap();
    }

    @Override
    protected void handleTokens() {

    }

    @Override
    protected void handleStart() {
        for (EventPlayer eventPlayer : this.participants) {
            ActivePlayer towersPlayer = new ActivePlayer(eventPlayer, this);
            this.towersPlayers.put(towersPlayer);
        }
        // New Round
        newRound();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        ticksElapsed += TICK_INTERVAL;

        if (ticksElapsed >= ROUND_DURATION) {
            ticksElapsed = 0;
            // New Round
            newRound();
            return;
        }

        for (TowersPlayer towersPlayer : towersPlayers.values()) {
            towersPlayer.onTick();
        }

        if (this.newItemTimer == null || this.newItemTimer.isCancelled()) {
            this.newItemTimer = this.newItemTimer();
            this.newItemTimer.start();
        }
    }

    @Override
    protected void handleStop() {
        if (this.newItemTimer != null) {
            this.newItemTimer.stop(false);
        }
        this.sendAudienceMessage("This minigame has concluded.");
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        TowersPlayer towersPlayer = this.towersPlayers.remove(participant.getUuid());
        if (towersPlayer != null) {
            towersPlayer.cleanup();
        }
        return super.removeParticipant(participant);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getEntity();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer == null) {
            bukkitPlayer.sendMessage("You are not participating in this minigame.");
            return;
        }
        towersPlayer.onDeath(event);
        this.swapRole(ActivePlayer.class, towersPlayer, () -> new Spectator(towersPlayer.getEventPlayer(), this));
    }


    public void newRound() {
        if (this.round >= MAX_ROUNDS) {
            this.towersPlayers.values().forEach(towersPlayer -> {
                towersPlayer.cleanup();
                towersPlayer.getEventPlayer().operatePlayer(player -> player.teleportAsync(this.spawnLocation));
            });
            this.stop();
            return;
        }
        Executors.runAsync(() -> this.outerRegion.operate(block -> {
            if (!block.isEmpty()) {
                Executors.runSync (() -> block.setType(Material.AIR, false));
            }
        }));

        List<Location> locations = generateSpawnLocations(this.participants.size());


        for (TowersPlayer towersPlayer : this.towersPlayers.values()) {
            Location spawnLoc = locations.removeFirst();
            ActivePlayer newInstance = this.swapRole(Spectator.class, towersPlayer, () -> new ActivePlayer(towersPlayer.getEventPlayer(), this));
            newInstance.onNewRound(spawnLoc);
        }

        this.round++;
    }


    private <T extends TowersPlayer> T swapRole(Class<? extends TowersPlayer> ifRole, TowersPlayer towersPlayer, Supplier<? extends TowersPlayer> newRoleSupplier) {
        if (!ifRole.isInstance(towersPlayer)) {
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
        towersPlayers.put(newRole);
        return (T) newRole;
    }

    public List<Location> generateSpawnLocations(int desiredPoints) {
        WorldTiedBoundingBox tiedBoundingBox = (WorldTiedBoundingBox) this.boundingBox;
        Location center = tiedBoundingBox.getCenterLocation();
        List<Location> locations = new ArrayList<>();

        // Calculate the area dimensions
        double width = tiedBoundingBox.getMaxX() - tiedBoundingBox.getMinX();
        double length = tiedBoundingBox.getMaxZ() - tiedBoundingBox.getMinZ();

        // Minimum spacing between spawn points
        int minSpacing = 10;

        // Calculate grid dimensions (trying to keep it roughly square)
        int pointsX = (int) Math.ceil(Math.sqrt(desiredPoints * (width / length)));
        int pointsZ = (int) Math.ceil((double) desiredPoints / pointsX);

        // Adjust if we overshot the desired count
        while (pointsX * pointsZ > desiredPoints && pointsZ > 1) {
            pointsZ--;
        }

        // Calculate required spacing
        double spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
        double spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;

        // Check if spacing is too small
        if ((spacingX < minSpacing && pointsX > 1) || (spacingZ < minSpacing && pointsZ > 1)) {
            throw new IllegalStateException(
                    String.format("Cannot fit %d spawn points with minimum %d block spacing. " +
                                    "Bounding box is too small (%dx%d blocks). Maximum points: %d",
                            desiredPoints, minSpacing, (int)width, (int)length,
                            ((int)(width/minSpacing) + 1) * ((int)(length/minSpacing) + 1))
            );
        }

        // Generate grid of locations
        double startX = tiedBoundingBox.getMinX();
        double startZ = tiedBoundingBox.getMinZ();

        int generatedPoints = 0;
        for (int x = 0; x < pointsX && generatedPoints < desiredPoints; x++) {
            for (int z = 0; z < pointsZ && generatedPoints < desiredPoints; z++) {
                double locX = startX + (x * spacingX);
                double locZ = startZ + (z * spacingZ);

                Location spawnLoc = new Location(center.getWorld(), locX, center.getY(), locZ);
                locations.add(spawnLoc);
                generatedPoints++;
            }
        }
        return locations;
    }

    private CountdownBossBar newItemTimer() {
        return CountdownBossBar.builder()
                .title("<b>Round: " + (round + 1) + " | " + " Next item in: %s")
                .seconds(9)
                .color(BossBar.Color.YELLOW)
                .audience(this.audience)
                .callback(() -> {
                    for (Material material : Material.values()) {
                        CreativeCategory creativeCategory = material.getCreativeCategory();
                        if (creativeCategory == null) continue;
                        int weight = creativeCategoryWeights.getOrDefault(creativeCategory, 0);
                        if (weight == 0) continue;

                        for (ActivePlayer activePlayer : towersPlayers.getActivePlayers()) {
                            ItemStack itemStack;
                            if (Util.RANDOM.nextInt(100) <= 15) {
                                // Special Item
                                List<CustomItem> customItems =  LocalCustomItemManager.getCustomItems()
                                        .stream()
                                        .filter(it -> it instanceof TowersItemNestItem)
                                        .toList();
                                itemStack = Util.getRandom(customItems).createItem().getSecond();
                            } else {
                                itemStack = ItemStack.of(material);
                            }
                            activePlayer.giveItem(itemStack);
                        }

                    }
                })
                .build();
    }


    @Getter
    private abstract static class TowersPlayer {
        protected final EventPlayer eventPlayer;
        protected final Towers context;

        public TowersPlayer(EventPlayer eventPlayer, Towers context) {
            this.eventPlayer = eventPlayer;
            this.context = context;
        }

        public UUID getUuid() {
            return this.eventPlayer.getUuid();
        }

        public abstract void onTick();
        public abstract void onDeath(PlayerDeathEvent event);
        public abstract void cleanup();
    }


    private static class ActivePlayer extends TowersPlayer {

        private static final int MINECRAFT_MIN_Y = -64;
        private Location spawnLocation;

        public ActivePlayer(EventPlayer eventPlayer, Towers context) {
            super(eventPlayer, context);
        }

        public void onNewRound(Location spawnLocation) {
            for (int i = spawnLocation.getBlockY() - 1; i >= MINECRAFT_MIN_Y; i--) {
                Block block = spawnLocation.getWorld().getBlockAt(spawnLocation.getBlockX(), i, spawnLocation.getBlockZ());
                block.setType(Material.BEDROCK);
            }
            this.eventPlayer.teleportAsync(spawnLocation.toCenterLocation());
            this.spawnLocation = spawnLocation;
        }

        public void giveItem(ItemStack itemStack) {
            this.eventPlayer.operatePlayer(player -> player.give(itemStack));
        }

        @Override
        public void onTick() {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) {
                return;
            }
            player.sendActionBar(Text.mm("Time left: " + (ROUND_DURATION - Util.ticksToSecs(context.getTicksElapsed()))));
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            this.context.sendAudienceMessage(event.deathMessage());
            if (this.spawnLocation != null) {
                this.eventPlayer.teleportAsync(this.spawnLocation);
            }
            event.setCancelled(true);
        }

        @Override
        public void cleanup() {
            this.eventPlayer.operatePlayer(player -> {
                player.getInventory().clear();
            });
        }
    }

    private static class Spectator extends TowersPlayer {

        private boolean hidden = false;

        public Spectator(EventPlayer eventPlayer, Towers context) {
            super(eventPlayer, context);
            this.toggleHidden();
        }

        public void toggleHidden() {
            EventMain instance = EventMain.getInstance();
            this.eventPlayer.operatePlayer(player -> {
                for (EventPlayer participant : this.context.getParticipants()) {
                    if (participant.getUuid().equals(this.eventPlayer.getUuid())) {
                        continue;
                    }
                    participant.operatePlayer(pPlayer -> {
                        Executors.sync(() -> {
                            if (this.hidden) {
                                if (pPlayer.canSee(player)) {
                                    throw new IllegalStateException("Bad state: player can see other player already");
                                }
                                pPlayer.showPlayer(instance, player);
                            } else {
                                pPlayer.hidePlayer(instance, player);
                            }
                        });
                    });
                }
            });
            this.hidden = !this.hidden;
        }

        @Override
        public void onTick() {
            Player bukkitPlayer = this.eventPlayer.getPlayer();
            if (bukkitPlayer != null && bukkitPlayer.getFallDistance() > 0.1) {
                bukkitPlayer.setFlying(true);
                this.eventPlayer.sendActionBar("You are now flying!");
            }
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            event.setCancelled(true);
            this.eventPlayer.teleportAsync(this.context.boundingBox.getCenterLocation());
        }

        @Override
        public void cleanup() {
            if (this.hidden) {
                this.toggleHidden();
            }
            this.eventPlayer.operatePlayer(player -> {
                player.teleportAsync(this.context.spawnLocation);
                player.setFlying(false);
            });
        }
    }

    private static class TowersPlayerMap extends HashMap<UUID, TowersPlayer> {
        public TowersPlayer put(TowersPlayer value) {
            return super.put(value.getUuid(), value);
        }

        public List<ActivePlayer> getActivePlayers() {
            return this.values().stream()
                    .filter(it -> it instanceof ActivePlayer)
                    .map(ActivePlayer.class::cast)
                    .toList();
        }

        public List<Spectator> getSpectators() {
            return this.values().stream()
                    .filter(it -> it instanceof Spectator)
                    .map(Spectator.class::cast)
                    .toList();
        }
    }
}
