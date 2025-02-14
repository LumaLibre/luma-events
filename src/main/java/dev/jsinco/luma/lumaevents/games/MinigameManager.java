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
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends BukkitRunnable {

    private static MinigameManager instance;

    private final Config cfg = EventMain.getOkaeriConfig();

    private final Map<Class<? extends Minigame>, Supplier<Minigame>> minigameSupplier = Map.of(
            Envoys.class, () -> new Envoys(cfg.getEnvoys()),
            Paintball.class, () -> new Paintball(cfg.getPaintball()),
            BoatRace.class, () -> new BoatRace(cfg.getBoatRace())
    );


    @NotNull
    @Getter
    private Minigame current = new NonActiveMinigame();


    public boolean newMinigame(Class<? extends Minigame> game, boolean force, int seconds) throws GameAlreadyStartedException {
        if (this.current.isActive()) {
            if (!force) {
                throw new GameAlreadyStartedException("Minigame: " + this.current.getName() + " is already active!");
            }
            this.current.stop();
        }

        Util.broadcast("<hover:show_text:'Click me!'><click:run_command:/event join>A minigame is starting! Use <gold>/valentide join</gold> to participate!");
        this.cfg.setLastGameLaunchTime(System.currentTimeMillis());
        this.cfg.save();
        this.current = this.minigameSupplier.get(game).get();
        return this.current.start(seconds);
    }

    public boolean newMinigame(Class<? extends Minigame> game, boolean force) throws GameAlreadyStartedException {
        return this.newMinigame(game, force, 90);
    }

    public boolean tryNewMinigameSafely(Class<? extends Minigame> game, boolean ignoreCooldown, int seconds) {
        if (!this.canSafelyStartMinigame(ignoreCooldown)) {
            return false;
        }

        try {
            this.newMinigame(game, false, seconds);
            return true;
        } catch (GameAlreadyStartedException oopsie) {
            oopsie.printStackTrace();
            return false;
        }
    }

    public boolean tryNewMinigameSafely(Class<? extends Minigame> game, boolean ignoreCooldown) {
        return this.tryNewMinigameSafely(game, ignoreCooldown, 90);
    }

    public boolean canSafelyStartMinigame(boolean ignoreCooldown) {
        if (this.current.isActive() || this.current.isOpen()) {
            return false; // We can't start another minigame if one is active or has a queue open!
        }

        if (ignoreCooldown) {
            return true; // We can start a new minigame if we're ignoring the cooldown!
        }

        // We can't start another minigame if the cooldown hasn't passed!
        long currentTime = System.currentTimeMillis();
        long lastMinigameTime = this.cfg.getLastGameLaunchTime();


        return (currentTime - lastMinigameTime) >= this.cfg.getAutomaticMinigameCooldown();
    }

    @Override
    public void run() {
        if (this.canSafelyStartMinigame(false)) {
            if (cfg.isAutomaticMinigames()) {
                this.newMinigame(Util.getRandom(this.minigameSupplier.keySet()), false);
            } else {
                Util.sendMsg(Bukkit.getConsoleSender(), "Tried to start an automatic minigame, but it's disabled in the config!");
            }

        }
    }

    public static MinigameManager getInstance() {
        if (instance == null) {
            instance = new MinigameManager();
        }
        return instance;
    }
}
