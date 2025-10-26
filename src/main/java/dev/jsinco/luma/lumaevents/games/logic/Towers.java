package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumacore.utility.Text;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.MaterialCount;
import dev.jsinco.luma.lumaevents.configurable.sectors.TowersDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.TowersItems;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.games.tokenformula.TowersTokenFormula;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// TODO: Test items, bounding box sizes, add game ending & scoreboard.
public final class Towers extends InventoryUnifiedMinigame {

    private static final int TICK_INTERVAL = 2;


    private final WorldTiedBoundingBox outerRegion;
    private final Location spawnLocation;
    private final TowersItems towersItems;
    private final TowersPlayerMap towersPlayers;
    private final Scoreboard<EventPlayer> scoreboard;
    private final TowersTokenFormula tokenFormula;

    private CountdownBossBar newItemTimer;
    private int initialPlayerCount;

    public Towers(TowersDefinition def) {
        super("Towers", "Don't fall.", 600000, TICK_INTERVAL, false, true, false, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.outerRegion = WorldTiedBoundingBox.of(def.getOuterRegion().getLoc1(), def.getOuterRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation().toCenterLocation();
        this.towersItems = def.getTowersItems();
        this.towersPlayers = new TowersPlayerMap();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new TowersTokenFormula();
    }

    @Override
    protected int minimumParticipants() {
        return 2;
    }

    @Override
    protected void handleTokens() {
        for (TowersPlayer manorPlayer : this.towersPlayers) {
            EventPlayer eventPlayer = manorPlayer.getEventPlayer();
            int finalScore = this.scoreboard.getScore(eventPlayer);

            this.tokenFormula.giveTokens(eventPlayer, finalScore);
            eventPlayer.addPermanentScore(MinigameConstant.TOWERS, finalScore);
        }
    }

    @Override
    protected void handleStart() {
        this.initialPlayerCount = this.participants.size();
        for (EventPlayer eventPlayer : this.participants) {
            ActivePlayer towersPlayer = new ActivePlayer(eventPlayer, this);
            this.towersPlayers.put(towersPlayer);
        }

        List<Location> locations = generateSpawnLocations(this.participants.size());


        for (TowersPlayer towersPlayer : this.towersPlayers.values()) {
            Location spawnLoc = locations.removeFirst();
            ActivePlayer newInstance = this.swapRole(Spectator.class, towersPlayer, () -> new ActivePlayer(towersPlayer.getEventPlayer(), this));
            newInstance.onNewRound(spawnLoc);
        }

        this.newItem();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.towersPlayers.getActivePlayers().size() <= 1) {
            this.stop();
            return;
        }

        for (TowersPlayer towersPlayer : towersPlayers.values()) {
            towersPlayer.onTick(timeLeft);
        }

        if (this.newItemTimer == null || this.newItemTimer.isCancelled()) {
            this.newItemTimer = this.newItemTimer();
            this.newItemTimer.start();
        }
    }

    @Override
    protected void handleStop() {
        this.towersPlayers.getActivePlayers().forEach(remaining ->{
            this.positionalBasedPoints(remaining.getEventPlayer());
        });

        this.towersPlayers.forEach(towersPlayer -> {
            towersPlayer.cleanup();
            towersPlayer.getEventPlayer().operatePlayer(player -> {
                player.setFallDistance(0f);
                player.teleportAsync(this.spawnLocation);
            });
        });
        Executors.runSync(() -> {
            this.outerRegion.operate(block -> {
                if (!block.isEmpty()) {
                    block.setType(Material.AIR);
                }
            });
            this.outerRegion.getEntities(Entity.class).stream().filter(it -> !(it instanceof Player)).forEach(Entity::remove);
        });
        if (this.newItemTimer != null) {
            this.newItemTimer.stop(false);
        }

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
        this.swapRole(ActivePlayer.class, towersPlayer, () -> new Spectator(towersPlayer.getEventPlayer(), this, towersPlayer.getRespawnLocation()));
    }


    @EventHandler
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        TowersPlayer victimTowersPlayer = this.towersPlayers.get(victim.getUniqueId());
        TowersPlayer attackerTowersPlayer = this.towersPlayers.get(attacker.getUniqueId());
        if (victimTowersPlayer == null || attackerTowersPlayer == null) {
            return;
        }

        if (attackerTowersPlayer instanceof Spectator spectator) {
            spectator.getEventPlayer().sendMessage("You cannot attack players while spectating.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (towersPlayer instanceof Spectator) {
            Util.sendMsg(bukkitPlayer, "You cannot interact while spectating.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        TowersPlayer towersPlayer = this.towersPlayers.get(bukkitPlayer.getUniqueId());
        if (!(towersPlayer instanceof Spectator)) {
            return;
        }
        if (!this.boundingBox.contains(event.getTo())) {
            EventPlayer eventPlayer = towersPlayer.getEventPlayer();
            eventPlayer.teleportAsync(towersPlayer.respawnLocation);
            eventPlayer.sendMessage("Out of bounds!");
        }
    }


    public void positionalBasedPoints(EventPlayer eventPlayer) {
        // players before this one is removed
        int remainingPlayers = this.towersPlayers.getActivePlayers().size() + 1; // 0-based
        // grant points based on how few players are left
        int position = this.initialPlayerCount - remainingPlayers;
        this.scoreboard.addScore(eventPlayer, position);
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

        // Minimum and maximum spacing between spawn points
        int minSpacing = 10;
        int maxSpacing = 15;

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

        // Check if spacing is too large - if so, increase grid density
        while ((spacingX > maxSpacing || spacingZ > maxSpacing) && (pointsX * pointsZ < desiredPoints * 4)) {
            if (spacingX > maxSpacing && spacingX >= spacingZ) {
                pointsX++;
            } else if (spacingZ > maxSpacing) {
                pointsZ++;
            } else {
                break;
            }

            // Recalculate spacing
            spacingX = (pointsX > 1) ? width / (pointsX - 1) : 0;
            spacingZ = (pointsZ > 1) ? length / (pointsZ - 1) : 0;
        }

        // Calculate jitter amount (random offset within cell)
        double jitterX = Math.min(spacingX * 0.4, (spacingX - minSpacing) / 2);
        double jitterZ = Math.min(spacingZ * 0.4, (spacingZ - minSpacing) / 2);

        // Generate grid of locations with randomness
        double startX = tiedBoundingBox.getMinX();
        double startZ = tiedBoundingBox.getMinZ();

        int generatedPoints = 0;
        for (int x = 0; x < pointsX && generatedPoints < desiredPoints; x++) {
            for (int z = 0; z < pointsZ && generatedPoints < desiredPoints; z++) {
                // Base grid position
                double locX = startX + (x * spacingX);
                double locZ = startZ + (z * spacingZ);

                // Add random jitter
                double offsetX = (RANDOM.nextDouble() * 2 - 1) * jitterX; // Random between -jitterX and +jitterX
                double offsetZ = (RANDOM.nextDouble() * 2 - 1) * jitterZ;

                // Clamp to bounding box
                locX = Math.max(tiedBoundingBox.getMinX(), Math.min(tiedBoundingBox.getMaxX(), locX + offsetX));
                locZ = Math.max(tiedBoundingBox.getMinZ(), Math.min(tiedBoundingBox.getMaxZ(), locZ + offsetZ));

                Location spawnLoc = new Location(center.getWorld(), locX, center.getY(), locZ);
                locations.add(spawnLoc);
                generatedPoints++;
            }
        }
        return locations;
    }

    private CountdownBossBar newItemTimer() {
        return CountdownBossBar.builder()
                .title("<b>Next drop in: %s")
                .seconds(9)
                .color(BossBar.Color.YELLOW)
                .audience(this.audience)
                .callback(() -> {
                    this.newItem();
                })
                .build();
    }

    private void newItem() { // FIXME
        for (ActivePlayer activePlayer : towersPlayers.getActivePlayers()) {
            ItemStack itemStack = ItemStack.of(Material.BEDROCK);
            if (RANDOM.nextInt(100) <= 20) {
                if (RANDOM.nextInt(100) > 40) {
                    List<MaterialCount> materialCounts = Util.getRandom(towersItems.getAllMaterialPackages());
                    for (MaterialCount materialCount : materialCounts) {
                        ItemStack local = ItemStack.of(materialCount.getMaterial(), materialCount.getCount());
                        Executors.sync(() -> activePlayer.giveItem(local));
                        activePlayer.getEventPlayer().sendMessage("debug item package: " + local.getType() + " x" + local.getAmount());
                    }
                    return;
                } else {
                    itemStack = ItemStack.of(Util.getRandom(towersItems.getRandomMaterials()));
                }
            } else {
                Material material = Util.getRandom(Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .filter(Material::isSolid)
                        .toList());
                itemStack = ItemStack.of(material, RANDOM.nextInt(1, 5));
            }
            final ItemStack finalItemStack = itemStack;
            activePlayer.getEventPlayer().sendMessage("debug item: " + itemStack.getType());
            Executors.sync(() -> activePlayer.giveItem(finalItemStack));
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


    private static class ActivePlayer extends TowersPlayer {

        private static final int MINECRAFT_MIN_Y = -63;

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
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 100, false, true, true));
                    player.teleportAsync(spawnLocation.add(0, 1, 0).toCenterLocation());
                });
            });
            this.respawnLocation = spawnLocation;
        }

        public void giveItem(ItemStack itemStack) {
            this.eventPlayer.operatePlayer(player -> Util.giveItem(player, itemStack));
        }

        @Override
        public void onTick(long timeLeft) {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) {
                return;
            }

            player.sendActionBar(Text.mm("<yellow>Time left: " + Util.millisToSecs(timeLeft) + "s"));
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            event.setCancelled(true);
            this.context.sendAudienceMessage(event.deathMessage());
            if (this.respawnLocation != null) {
                this.eventPlayer.teleportAsync(this.respawnLocation);
            }

            this.context.positionalBasedPoints(this.eventPlayer);
        }

        @Override
        public void cleanup() {
            this.eventPlayer.operatePlayer(player -> {
                player.getInventory().clear();
            });
        }
    }

    private static class Spectator extends TowersPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, TICK_INTERVAL * 3, 0, false, true, true);

        private boolean hidden = false;

        public Spectator(EventPlayer eventPlayer, Towers context, Location respawnLocation) {
            super(eventPlayer, context);
            this.respawnLocation = respawnLocation;
            this.hidePlayer();
            this.eventPlayer.operatePlayer(player -> player.setAllowFlight(true));
        }

        public void hidePlayer() {
            Player player = this.eventPlayer.getPlayer();
            if (player == null) return;
            for (EventPlayer participant : this.context.getParticipants()) {
                if (participant.getUuid().equals(this.eventPlayer.getUuid())) {
                    continue;
                }
                participant.operatePlayer(pPlayer -> {
                    Executors.sync(() -> {
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
                    Executors.sync(() -> {
                        pPlayer.showPlayer(EventMain.getInstance(), player);
                    });
                });
            }
            this.hidden = false;
        }


        @Override
        public void onTick(long timeLeft) {
            eventPlayer.operatePlayer(
                    player -> player.addPotionEffect(INVISIBILITY)
            );
        }

        @Override
        public void onDeath(PlayerDeathEvent event) {
            event.setCancelled(true);
            this.eventPlayer.teleportAsync(this.respawnLocation);
        }

        @Override
        public void cleanup() {
            this.showPlayer();
            this.eventPlayer.operatePlayer(player -> {
                player.teleportAsync(this.context.spawnLocation);
                player.setAllowFlight(false);
            });
        }
    }

    private static class TowersPlayerMap extends HashMap<UUID, TowersPlayer> implements Iterable<TowersPlayer> {
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

        @NotNull
        @Override
        public Iterator<TowersPlayer> iterator() {
            return this.values().iterator();
        }
    }

}
