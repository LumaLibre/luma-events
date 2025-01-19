package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.games.exceptions.GameAlreadyStartedException;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends BukkitRunnable {

    private static final long MINIGAME_COOLDOWN = 30000L;

    @Getter
    private static final MinigameManager instance = new MinigameManager();

    private final Config cfg = ConfigManager.getInstance().getConfig();

    private final Map<Class<? extends Minigame>, Supplier<Minigame>> minigameSupplier = Map.of(
            Envoys.class, () -> new Envoys(cfg.getEnvoys().getLoc1(), cfg.getEnvoys().getLoc2()),
            Paintball.class, () -> new Paintball(cfg.getPaintball().getRegion().getLoc1(), cfg.getPaintball().getRegion().getLoc2(), cfg.getPaintball().getSpawnPoint())
    );


    @NotNull
    @Getter
    private Minigame current = new NonActiveMinigame();


    public boolean newMinigame(Class<? extends Minigame> game, boolean force) throws GameAlreadyStartedException {
        if (this.current.isActive()) {
            if (!force) {
                throw new GameAlreadyStartedException("Minigame: " + this.current.getName() + " is already active!");
            }
            this.current.stop();
        }

        this.current = this.minigameSupplier.get(game).get();
        return this.current.start();
    }

    public boolean tryNewMinigameSafely(Class<? extends Minigame> game) {
        if (!this.canSafelyStartMinigame()) {
            return false;
        }

        try {
            this.newMinigame(game, false);
            return true;
        } catch (GameAlreadyStartedException oopsie) {
            oopsie.printStackTrace();
            return false;
        }
    }

    public boolean canSafelyStartMinigame() {
        if (this.current.isActive() || this.current.isOpen()) {
            return false; // We can't start another minigame if one is active or has a queue open!
        }

        // We can't start another minigame if the cooldown hasn't passed!
        long timeSinceLast = System.currentTimeMillis() - this.current.getStartTime();
        return timeSinceLast >= MINIGAME_COOLDOWN; // Passed all checks, we can start a new minigame!
    }

    @Override
    public void run() {
        if (this.canSafelyStartMinigame()) {
            this.newMinigame(Util.getRandom(this.minigameSupplier.keySet()), false);
        }
    }
}
