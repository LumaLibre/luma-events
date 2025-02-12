package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.games.exceptions.GameAlreadyStartedException;
import dev.jsinco.luma.lumaevents.games.logic.BoatRace;
import dev.jsinco.luma.lumaevents.games.logic.Envoys;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.NonActiveMinigame;
import dev.jsinco.luma.lumaevents.games.logic.Paintball;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

// TODO: start runnable for this class from main class
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends BukkitRunnable {

    private static MinigameManager instance;

    private final Config cfg = EventMain.getOkaeriConfig();
    // TODO: Pass in config instead of methods
    private final Map<Class<? extends Minigame>, Supplier<Minigame>> minigameSupplier = Map.of(
            Envoys.class, () -> new Envoys(cfg.getEnvoys()),
            Paintball.class, () -> new Paintball(cfg.getPaintball()),
            BoatRace.class, () -> new BoatRace(cfg.getBoatRace())
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

        Util.broadcast("A minigame is starting! Use <gold>/valentide join</gold> to participate!");
        this.current = this.minigameSupplier.get(game).get();
        return this.current.start();
    }

    public boolean tryNewMinigameSafely(Class<? extends Minigame> game, boolean ignoreCooldown) {
        if (!this.canSafelyStartMinigame(ignoreCooldown)) {
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

    public boolean canSafelyStartMinigame(boolean ignoreCooldown) {
        if (this.current.isActive() || this.current.isOpen()) {
            return false; // We can't start another minigame if one is active or has a queue open!
        }

        if (ignoreCooldown) {
            return true; // We can start a new minigame if we're ignoring the cooldown!
        }

        // We can't start another minigame if the cooldown hasn't passed!
        long timeSinceLast = System.currentTimeMillis() - this.current.getStartTime();
        return timeSinceLast >= cfg.getAutomaticMinigameCooldown(); // Passed all checks, we can start a new minigame!
    }

    @Override
    public void run() {
        if (cfg.isAutomaticMinigames() && this.canSafelyStartMinigame(false)) {
            this.newMinigame(Util.getRandom(this.minigameSupplier.keySet()), false);
        }
    }

    public static MinigameManager getInstance() {
        if (instance == null) {
            instance = new MinigameManager();
        }
        return instance;
    }
}
