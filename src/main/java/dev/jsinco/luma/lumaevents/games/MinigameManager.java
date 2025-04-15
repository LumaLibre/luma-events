package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.games.exceptions.GameAlreadyStartedException;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.NonActiveMinigame;
import dev.jsinco.luma.lumaevents.games.logic.TheNabbits;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends BukkitRunnable {

    private static MinigameManager instance;

    private final Config cfg = EventMain.getOkaeriConfig();

    private final Map<Class<? extends Minigame>, Supplier<Minigame>> minigameSupplier = Map.of(
            TheNabbits.class, () -> new TheNabbits(cfg.getTheNabbits())
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

        Util.broadcast("<hover:show_text:'Click me!'><click:run_command:/event join>A minigame is starting! Use <gold>/easter join</gold> to participate!");
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1f, 0.75f);
        });
        this.current = this.minigameSupplier.get(game).get();
        return this.current.start(seconds);
    }

    public boolean newMinigame(Class<? extends Minigame> game, boolean force) throws GameAlreadyStartedException {
        return this.newMinigame(game, force, 90);
    }

    public boolean newMinigame(boolean force) throws GameAlreadyStartedException {
        return this.newMinigame(force, 90);
    }

    public boolean newMinigame(boolean force, int seconds) throws GameAlreadyStartedException {
        // This is lazy, but it's our only minigame for this event.
        return this.newMinigame(TheNabbits.class, force, seconds);
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

    public boolean tryNewMinigameSafely(boolean ignoreCooldown, int seconds) {
        if (!this.canSafelyStartMinigame(ignoreCooldown)) {
            return false;
        }

        try {
            this.newMinigame(false, seconds);
            return true;
        } catch (GameAlreadyStartedException oopsie) {
            oopsie.printStackTrace();
            return false;
        }
    }

    public boolean tryNewMinigameSafely(Class<? extends Minigame> game, boolean ignoreCooldown) {
        return this.tryNewMinigameSafely(game, ignoreCooldown, 90);
    }

    public boolean tryNewMinigameSafely(boolean ignoreCooldown) {
        return this.tryNewMinigameSafely(ignoreCooldown, 90);
    }

    public boolean canSafelyStartMinigame(boolean ignoreCooldown) {
        if (!cfg.isAutomaticMinigames()) {
            return false;
        }

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
                this.cfg.setLastGameLaunchTime(System.currentTimeMillis());
                this.cfg.save();
                this.newMinigame(false);
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
