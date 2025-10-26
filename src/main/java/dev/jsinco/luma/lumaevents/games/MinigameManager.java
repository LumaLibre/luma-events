package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.MinigameState;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.exceptions.GameAlreadyStartedException;
import dev.jsinco.luma.lumaevents.games.exceptions.NoAvailableMinigames;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.NonActiveMinigame;
import dev.jsinco.luma.lumaevents.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends BukkitRunnable {


    private static MinigameManager instance;

    private final Config cfg = EventMain.getOkaeriConfig();
    private final MinigameState minigameState = EventMain.getMinigameState();


    @NotNull
    @Getter
    private Minigame current = new NonActiveMinigame();


    public boolean newMinigame(MinigameConstant game, boolean force, int seconds) throws GameAlreadyStartedException {
        return this.newMinigame(game, Util.getRandom(game.getDefinitions().values()), force, seconds);
    }

    public boolean newMinigame(MinigameConstant game, OkaeriConfig definition, boolean force, int seconds) throws GameAlreadyStartedException {
        if (this.current.isActive()) {
            if (!force) {
                throw new GameAlreadyStartedException("Minigame: " + this.current.getName() + " is already active!");
            }
            this.current.stop();
        }

        Util.broadcast("<hover:show_text:'Click me!'><click:run_command:/event join>A minigame is starting! Use <gold>/event join</gold> to participate!");
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1f, 0.75f);
        });

        this.current = game.getSupplier(definition).get();
        return this.current.start(seconds);
    }

    public boolean newMinigame(MinigameConstant game, boolean force) throws GameAlreadyStartedException {
        return this.newMinigame(game, force, 90);
    }

    public boolean newMinigame(boolean force) throws GameAlreadyStartedException {
        return this.newMinigame(force, 90);
    }

    public boolean newMinigame(boolean force, int seconds) throws GameAlreadyStartedException {
        // Random minigame selection
        List<MinigameConstant> minigames = cfg.getEnabledAutomaticMinigames();
        if (minigames.isEmpty()) {
            throw new NoAvailableMinigames("Cannot start random minigame: No available minigames configured!");
        }
        return this.newMinigame(Util.getRandom(minigames), force, seconds);
    }

    public boolean tryNewMinigameSafely(MinigameConstant game, boolean ignoreCooldown, int seconds) {
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

    public boolean tryNewMinigameSafely(MinigameConstant game, OkaeriConfig definition, boolean ignoreCooldown, int seconds) {
        if (!this.canSafelyStartMinigame(ignoreCooldown)) {
            return false;
        }

        try {
            this.newMinigame(game, definition, false, seconds);
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

    public boolean tryNewMinigameSafely(MinigameConstant game, boolean ignoreCooldown) {
        return this.tryNewMinigameSafely(game, ignoreCooldown, 90);
    }

    public boolean tryNewMinigameSafely(boolean ignoreCooldown) {
        return this.tryNewMinigameSafely(ignoreCooldown, 90);
    }

    public boolean canSafelyStartMinigame(boolean ignoreCooldown) {
        if (this.current.isActive() || this.current.isOpen() || !cfg.isAutomaticMinigames()) {
            return false; // We can't start another minigame if one is active or has a queue open!
        }

        if (ignoreCooldown) {
            return true; // We can start a new minigame if we're ignoring the cooldown!
        }

        // We can't start another minigame if the cooldown hasn't passed!
        long currentTime = System.currentTimeMillis();
        long lastMinigameTime = this.minigameState.getLastGameLaunchTime();


        return (currentTime - lastMinigameTime) >= this.cfg.getAutomaticMinigameCooldown();
    }

    @Override
    public void run() {
        if (!cfg.isAutomaticMinigames() || !this.canSafelyStartMinigame(false)) {
            return;
        }


        List<MinigameConstant> availableMinigames = cfg.getEnabledAutomaticMinigames();
        if (availableMinigames.isEmpty()) {
            throw new NoAvailableMinigames("Cannot start automatic minigame: No available minigames configured!");
        }

        int lastIndex = availableMinigames.indexOf(this.minigameState.getLastMinigame());
        if (lastIndex == -1) {
            lastIndex = 0; // Start from the beginning if the last minigame is not found
        }
        MinigameConstant nextMinigame = availableMinigames.get((lastIndex + 1) % availableMinigames.size());

        this.minigameState.setLastGameLaunchTime(System.currentTimeMillis());
        this.minigameState.setLastMinigame(nextMinigame);
        this.minigameState.save();
        this.newMinigame(nextMinigame, false);
    }

    public static MinigameManager getInstance() {
        if (instance == null) {
            instance = new MinigameManager();
        }
        return instance;
    }
}
