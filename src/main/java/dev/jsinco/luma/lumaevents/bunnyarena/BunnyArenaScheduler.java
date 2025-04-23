package dev.jsinco.luma.lumaevents.bunnyarena;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class BunnyArenaScheduler extends BukkitRunnable {

    private final Config config = EventMain.getOkaeriConfig();

    private static BunnyArenaScheduler instance;
    private BunnyArenaRegionHandler bunnyArenaRegionHandler;

    private BunnyArenaScheduler() {
        try {
            this.bunnyArenaRegionHandler = new BunnyArenaRegionHandler(config.getBunnyArena());
        } catch (Throwable throwable) {
            EventMain.getInstance().getLogger().warning("BunnyArenaRegionHandler could not be initialized. (Null locations?)");
        }
    }

    public void refreshBunnyArenaRegionHandler() {
        try {
            Boolean spawnedBunny = null;
            if (this.bunnyArenaRegionHandler != null) {
                spawnedBunny = this.bunnyArenaRegionHandler.isSpawnedBunny();
            }
            this.bunnyArenaRegionHandler = new BunnyArenaRegionHandler(config.getBunnyArena());

            if (spawnedBunny != null) {
                this.bunnyArenaRegionHandler.setSpawnedBunny(spawnedBunny);
            }
            Util.log("BunnyArenaRegionHandler refreshed.");
        } catch (Throwable throwable) {
            EventMain.getInstance().getLogger().warning("BunnyArenaRegionHandler could not be initialized. (Null locations?)");
        }
    }


    @Override
    public void run() {
        if (this.bunnyArenaRegionHandler == null) {
            return;
        }

        if (!config.getBunnyArena().isBunnyArenaEnabled()) {
            if (bunnyArenaRegionHandler.isSpawnedBunny()) {
                bunnyArenaRegionHandler.removeAllBunnies();
            }
            return;
        }
        bunnyArenaRegionHandler.autoSpawnBunnies();
    }


    public static BunnyArenaScheduler getInstance() {
        if (instance == null) {
            instance = new BunnyArenaScheduler();
        }
        return instance;
    }
}
