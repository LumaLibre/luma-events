package dev.jsinco.luma.lumaevents.bunnyarena;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.BunnyArenaDefinition;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Getter
@Setter
public class BunnyArenaRegionHandler {

    private static final int EXTRA_BUNNIES_PER_PLAYER = 9;
    private static final int DEFAULT_MAX_BUNNIES = 60;

    private final WorldTiedBoundingBox playArea;
    private final WorldTiedBoundingBox spawnArea;
    private boolean spawnedBunny = false;

    public BunnyArenaRegionHandler(BunnyArenaDefinition def) {
        this.playArea = WorldTiedBoundingBox.of(def.getPlayRegion().getLoc1(), def.getPlayRegion().getLoc2());
        this.spawnArea = WorldTiedBoundingBox.of(def.getSpawnRegion().getLoc1(), def.getSpawnRegion().getLoc2());
    }


    /**
     * Automatically spawns as many bunnies as possible in the arena.
     */
    public void autoSpawnBunnies() {
        if (this.getPlayersInRegionSize() < 1) {
            return; // no players in the area
        }

        int max = DEFAULT_MAX_BUNNIES + (this.getPlayersInRegionSize() * EXTRA_BUNNIES_PER_PLAYER);
        int current = this.getBunnyCount();
        if (current >= max) {
            return; // already at max bunnies
        }

        int amountToSpawn = max - current;

        for (int i = 0; i < amountToSpawn; i++) {
            this.spawnBunnyAsynchronously(null);
        }
    }



    /**
     * Spawns a bunny in the arena using a random bunny type. Using as many async operations as possible.
     * @param consumer the consumer to call after the bunny is spawned
     */
    public void spawnBunnyAsynchronously(@Nullable Consumer<Rabbit> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(EventMain.getInstance(), () -> {
            Location location = this.getValidSpawnLocation();
            BunnyType bunnyType = BunnyType.randomType();
            handleRabbitSpawn(consumer, location, bunnyType);
        });
    }


    /**
     * Spawns a bunny in the arena with the given bunny type. Using as many async operations as possible.
     * @param bunnyType the type of bunny to spawn
     * @param consumer the consumer to call after the bunny is spawned
     */
    public void spawnBunnyAsynchronously(BunnyType bunnyType, @Nullable Consumer<Rabbit> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(EventMain.getInstance(), () -> {
            Location location = this.getValidSpawnLocation();
            handleRabbitSpawn(consumer, location, bunnyType);
        });
    }

    /**
     * Handles the bunny kill event. This should be called when a bunny is killed by a player.
     * @param rabbit the bunny that was killed
     * @param killer the player that killed the bunny
     */
    public void rewardBunnyKill(Entity rabbit, Player killer) {
        BunnyType bunnyType = BunnyType.getBunnyType(rabbit);
        if (bunnyType == null) {
            return; // not ours
        }

        int amount = Util.RANDOM.nextInt(bunnyType.getTokenMin(), bunnyType.getTokenMax());
        TokenExchanging.give(killer, bunnyType.getTokenType(), amount);
    }

    public void removeAllBunnies() {
        for (Rabbit rabbit : playArea.getEntities(Rabbit.class)) {
            if (rabbit.isValid()) {
                rabbit.remove();
            }
        }
        if (spawnedBunny) {
            spawnedBunny = false;
        }
    }


    public Location getValidSpawnLocation() {
        Location location = this.spawnArea.getRandomLocation();
        int attempts = 0;
        while (!isValidSpawnLocation(location)) {
            location = this.spawnArea.getRandomLocation();
            if (attempts++ > 10) {
                return location; // return the last location if we can't find a valid one after 10 attempts
            }
        }
        return location;
    }

    public int getPlayersInRegionSize() {
        return this.playArea.getPlayers().size();
    }

    public int getBunnyCount() {
        return this.playArea.getEntities(Rabbit.class).size();
    }


    private void handleRabbitSpawn(@Nullable Consumer<Rabbit> consumer, Location location, BunnyType bunnyType) {
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            Rabbit bunny = bunnyType.createBunny(location);
            location.getWorld().addEntity(bunny);
            if (consumer != null) {
                consumer.accept(bunny);
            }
        });
        if (!spawnedBunny) {
            spawnedBunny = true;
        }
    }


    private boolean isValidSpawnLocation(Location location) {
        return location.getBlock().isEmpty(); // super lazy check for rn, should make this better later
    }
}
