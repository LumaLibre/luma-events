package dev.lumas.events.games;

import dev.lumas.events.EventMain;
import dev.lumas.events.annotation.Depend;
import dev.lumas.events.configurable.GlobalStates;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.exceptions.GameAlreadyStartedException;
import dev.lumas.events.games.exceptions.NoAvailableMinigames;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.interfaces.TokenPayout;
import dev.lumas.events.games.logic.NonActiveMinigame;
import dev.lumas.events.utility.Externals;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinigameManager extends AsynchronousRunnable {


    private static MinigameManager instance;


    @NotNull
    @Getter
    private volatile Minigame current = new NonActiveMinigame();


    public boolean newMinigame(MinigameConstant game, boolean force, int seconds) throws GameAlreadyStartedException {
        Object definition = game.randomEnabledDefinition();
        if (definition == null) {
            throw new NoAvailableMinigames("Cannot start " + game.getDisplayName() + ": every one of its maps is disabled!");
        }
        return this.newMinigame(game, definition, force, seconds);
    }

    public boolean newMinigame(MinigameConstant game, Object definition, boolean force, int seconds) throws GameAlreadyStartedException {
        return this.newMinigame(game, definition, force, seconds, TokenPayout.NORMAL);
    }

    public boolean newMinigame(MinigameConstant game, Object definition, boolean force, int seconds, TokenPayout tokenPayout) throws GameAlreadyStartedException {
        if (this.current.isActive()) {
            if (!force) {
                throw new GameAlreadyStartedException("Minigame: " + this.current.getName() + " is already active!");
            }
            this.current.stop();
        }

        try {
            Minigame minigame = game.instantiate(definition);
            Depend depend = minigame.getClass().getAnnotation(Depend.class);
            if (depend != null) {
                for (String plugin : depend.value()) {
                    if (!Externals.pluginExists(plugin)) {
                        throw new NoAvailableMinigames("Cannot start " + game.getDisplayName() + ": it depends on the plugin '" + plugin + "' which is not installed!");
                    }
                }
            }
            this.current = minigame;
            this.current.setTokenPayout(tokenPayout);
            if (!tokenPayout.isNormal()) {
                EventMain.getInstance().getLogger().info(game.getDisplayName() + " was started with a token payout of " + tokenPayout);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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
        List<MinigameConstant> minigames = EventMain.getConfiguration().getEnabledAutomaticMinigames();
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

    public boolean tryNewMinigameSafely(MinigameConstant game, Object definition, boolean ignoreCooldown, int seconds) {
        return this.tryNewMinigameSafely(game, definition, ignoreCooldown, seconds, TokenPayout.NORMAL);
    }

    public boolean tryNewMinigameSafely(MinigameConstant game, Object definition, boolean ignoreCooldown, int seconds, TokenPayout tokenPayout) {
        if (!this.canSafelyStartMinigame(ignoreCooldown)) {
            return false;
        }

        try {
            this.newMinigame(game, definition, false, seconds, tokenPayout);
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
        if (this.current.isActive() || this.current.isOpen()) {
            return false; // We can't start another minigame if one is active or has a queue open!
        }

        if (ignoreCooldown) {
            return true; // We can start a new minigame if we're ignoring the cooldown!
        }

        // We can't start another minigame if the cooldown hasn't passed!
        long currentTime = System.currentTimeMillis();
        long lastMinigameTime = EventMain.getPersistentStates().getLastGameLaunchTime();


        return (currentTime - lastMinigameTime) >= EventMain.getConfiguration().getAutomaticMinigameCooldown();
    }

    @Nullable
    public MinigameConstant getNextAutomaticMinigame() {
        List<MinigameConstant> availableMinigames = EventMain.getConfiguration().getEnabledAutomaticMinigames();
        if (availableMinigames.isEmpty()) return null;

        int lastIndex = availableMinigames.indexOf(EventMain.getPersistentStates().getLastMinigame());
        if (lastIndex == -1) lastIndex = 0;

        return availableMinigames.get((lastIndex + 1) % availableMinigames.size());
    }

    @Override
    public void accept(ScheduledTask task) {
        if (!EventMain.getConfiguration().isAutomaticMinigames() || !this.canSafelyStartMinigame(false)) {
            return;
        }


        MinigameConstant nextMinigame = this.getNextAutomaticMinigame();
        if (nextMinigame == null) {
            throw new NoAvailableMinigames("Cannot start automatic minigame: No available minigames configured!");
        }

        GlobalStates persistentStates = EventMain.getPersistentStates();
        persistentStates.setLastGameLaunchTime(System.currentTimeMillis());
        persistentStates.setLastMinigame(nextMinigame);
        EventMain.getConfigManager().savePersistentStates();
        this.newMinigame(nextMinigame, false);
    }

    public static MinigameManager getInstance() {
        if (instance == null) {
            instance = new MinigameManager();
        }
        return instance;
    }
}
