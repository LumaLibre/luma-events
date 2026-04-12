package dev.lumas.events.bunnyarena;

import dev.lumas.core.manager.Services;
import dev.lumas.core.model.Service;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;

@Getter
public class BunnyArenaSchedulerService extends AsynchronousRunnable implements Service {

    private final Config config = EventMain.getOkaeriConfig();

    private static BunnyArenaSchedulerService instance;
    private BunnyArenaRegionHandler bunnyArenaRegionHandler;

    private BunnyArenaSchedulerService() {
        try {
            this.bunnyArenaRegionHandler = new BunnyArenaRegionHandler(config.getBunnyArena());
        } catch (Throwable throwable) {
            EventMain.getInstance().getLogger().warning("BunnyArenaRegionHandler could not be initialized. (Null locations?)");
        }
    }

    @Override
    public void register() {
        instance = this;
        this.repeatingAsync(0, 30 * 20); // 30 seconds
    }

    @Override
    public void unregister() {
        this.cancel();
    }

    public void refreshBunnyArenaRegionHandler() {
        try {
            Boolean spawnedBunny = null;
            if (this.bunnyArenaRegionHandler != null) {
                spawnedBunny = this.bunnyArenaRegionHandler.isAnySpawned();
            }
            this.bunnyArenaRegionHandler = new BunnyArenaRegionHandler(config.getBunnyArena());

            if (spawnedBunny != null) {
                this.bunnyArenaRegionHandler.setAnySpawned(spawnedBunny);
            }
            EventMain.getInstance().getLogger().info("BunnyArenaRegionHandler refreshed.");
        } catch (Throwable throwable) {
            EventMain.getInstance().getLogger().warning("BunnyArenaRegionHandler could not be initialized. (Null locations?)");
        }
    }

    @Override
    public void accept(ScheduledTask task) {
        if (this.bunnyArenaRegionHandler == null) {
            return;
        }

        if (!config.getBunnyArena().isBunnyArenaEnabled()) {
            if (bunnyArenaRegionHandler.isAnySpawned()) {
                bunnyArenaRegionHandler.removeAllBunnies();
            }
            return;
        }
        bunnyArenaRegionHandler.autoSpawnBunnies();
    }


    public static BunnyArenaSchedulerService getInstance() {
        if (instance == null) {
            instance = (BunnyArenaSchedulerService) Services.getTracked(BunnyArenaSchedulerService.class);
        }
        return instance;
    }

}