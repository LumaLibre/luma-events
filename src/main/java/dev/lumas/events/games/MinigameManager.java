package dev.lumas.events.games;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.PersistentStates;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.exceptions.GameAlreadyStartedException;
import dev.lumas.events.games.exceptions.NoAvailableMinigames;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.logic.NonActiveMinigame;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import eu.okaeri.configs.OkaeriConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends AsynchronousRunnable {


    private static MinigameManager instance;

    private final Config cfg = EventMain.getOkaeriConfig();
    private final PersistentStates persistentStates = EventMain.getPersistentStates();


    @NotNull
    @Getter
    private Minigame current = new NonActiveMinigame();


    public boolean newMinigame(MinigameConstant game, boolean force, int seconds) throws GameAlreadyStartedException {
        OkaeriConfig definition = game.randomEnabledDefinition();
        if (definition == null) {
            throw new NoAvailableMinigames("Cannot start " + game.getDisplayName() + ": every one of its maps is disabled!");
        }
        return this.newMinigame(game, definition, force, seconds);
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

        try {
            this.current = game.instantiate(definition);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            // FIXME
            HandlerList.unregisterAll(this.current);
            this.current = new NonActiveMinigame();
            return false;
        }
        return this.current.timedStart(seconds);
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
        long lastMinigameTime = this.persistentStates.getLastGameLaunchTime();


        return (currentTime - lastMinigameTime) >= this.cfg.getAutomaticMinigameCooldown();
    }

    @Nullable
    public MinigameConstant getNextAutomaticMinigame() {
        List<MinigameConstant> availableMinigames = cfg.getEnabledAutomaticMinigames();
        if (availableMinigames.isEmpty()) return null;

        int lastIndex = availableMinigames.indexOf(this.persistentStates.getLastMinigame());
        if (lastIndex == -1) lastIndex = 0;

        return availableMinigames.get((lastIndex + 1) % availableMinigames.size());
    }

    @Override
    public void accept(ScheduledTask task) {
        if (!cfg.isAutomaticMinigames() || !this.canSafelyStartMinigame(false)) {
            return;
        }


        MinigameConstant nextMinigame = this.getNextAutomaticMinigame();
        if (nextMinigame == null) {
            throw new NoAvailableMinigames("Cannot start automatic minigame: No available minigames configured!");
        }

        this.persistentStates.setLastGameLaunchTime(System.currentTimeMillis());
        this.persistentStates.setLastMinigame(nextMinigame);
        this.persistentStates.save();
        this.newMinigame(nextMinigame, false);
    }

    public static MinigameManager getInstance() {
        if (instance == null) {
            instance = new MinigameManager();
        }
        return instance;
    }
}
