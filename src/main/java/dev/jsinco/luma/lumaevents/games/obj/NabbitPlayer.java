package dev.jsinco.luma.lumaevents.games.obj;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import lombok.Getter;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.RabbitWatcher;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.potion.PotionEffectType;

@Getter
public class NabbitPlayer {

    private static final int MAX_HITS = 3;

    private final EventPlayer eventPlayer;

    private Role role;
    private double score;
    private int hitsTaken;

    private MobDisguise rabbitDisguise;

    public NabbitPlayer(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.role = Role.FLEEING;
        this.score = 0.0;
        this.hitsTaken = 0;
    }

    public void addScore(double extraPoints) {
        this.score += extraPoints * this.role.getScoreMultiplier();
    }


    public boolean isNabbit() {
        return this.role == Role.NABBIT || this.role == Role.NABBIT_BOOTSTRAP;
    }

    public String getName() {
        Player player = this.eventPlayer.getPlayer();
        if (player == null) {
            return "OfflinePlayer";
        }
        return player.getName();
    }

    public void handleGameEnd(Runnable callback) {
        if (this.rabbitDisguise != null) {
            this.rabbitDisguise.stopDisguise();
            this.rabbitDisguise = null;
        }

        if (this.isNabbit()) {
            Player bukkitPlayer = this.eventPlayer.getPlayer();
            if (bukkitPlayer != null) {
                ColorManager.updatePlayersColor(bukkitPlayer);
                bukkitPlayer.removePotionEffect(PotionEffectType.GLOWING);
            }
        }

        // TODO: Handle score
        System.out.println(this.getName() + ", Score: " + this.score);
    }

    /**
     * Someone's been hit by a Nabbit!
     * @return true if the player was caught, false if they couldn't be caught.
     */
    public boolean tryNabbitCatch(NabbitPlayer victim, Runnable callback) {
        if (victim.role != Role.FLEEING ) {
            return false;
        }

        this.eventPlayer.sendActionBar(victim.getHitStars("<purple>"));
        victim.eventPlayer.sendActionBar(victim.getHitStars("<red>"));

        if (hitsTaken++ < MAX_HITS) {
            return false;
        }

        // 80% chance to become a rabbit, 20% chance to become a nabbit
        if (Math.random() < 0.8) {
            victim.changeRole(Role.RABBIT);
        } else {
            victim.changeRole(Role.NABBIT);
        }

        // Reward the catcher
        this.addScore(1.5);
        callback.run();
        return true;
    }

    public void changeRole(Role newRole) {
        if (this.role == newRole) {
            return;
        }

        this.role = newRole;

        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }

        switch (newRole) {
            case NABBIT_BOOTSTRAP, NABBIT -> {
                eventPlayer.sendTitle(
                        "<dark_purple>You're a Nabbit!",
                        "<green>Catch as many players as you can."
                );
            }
            case FLEEING -> {
                eventPlayer.sendTitle(
                        "<green>Fleeing!",
                        "<aqua>Run away and collect carrots."
                );
            }
            case RABBIT -> {
                eventPlayer.sendTitle(
                        "<red>Rabbit...",
                        "<dark_green>Collect carrots to score points."
                );

                this.rabbitDisguise = new MobDisguise(DisguiseType.RABBIT);
                RabbitWatcher watcher = (RabbitWatcher) rabbitDisguise.getWatcher();
                watcher.setType(Util.getRandom(Rabbit.Type.values()));
                rabbitDisguise.setEntity(player);
                rabbitDisguise.setViewSelfDisguise(true);
                rabbitDisguise.startDisguise();
            }
        }
    }

    public String getHitStars(String color) {
        // Show 3 stars. '★' is a hit, '☆' is a not-yet-hit our of 3
        StringBuilder stars = new StringBuilder(color);
        for (int i = 0; i < MAX_HITS; i++) {
            if (i < hitsTaken) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }

    public enum Role {
        /**
         * The original Nabbit chosen at the beginning of the minigame.
         * <br />
         * If all nabbits disconnect from the game, we'll choose a new one and they'll become a bootstrap nabbit.
         * <br />
         * Earns points by catching FLEEING players.
         */
        NABBIT_BOOTSTRAP(1.4),
        /**
         * A Nabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by catching FLEEING players.
         */
        NABBIT(1.0),
        /**
         * A Rabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by collecting carrots.
         */
        RABBIT(0.8),
        /**
         * A player that has not yet been caught by a Nabbit.
         * <br />
         * Earns points by collecting carrots and not being caught by the Nabbit.
         */
        FLEEING(1.4);

        @Getter
        private final double scoreMultiplier;

        Role(double scoreMultiplier) {
            this.scoreMultiplier = scoreMultiplier;
        }
    }
}
