package dev.jsinco.luma.lumaevents.games.obj;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitChangeRole;
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitSurviveExtendedTimePeriod;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListeners;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import lombok.Getter;
import lombok.Setter;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.RabbitWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter
public class NabbitPlayer {

    private static final int MAX_HITS = 3;
    private static final PotionEffect GLOWING_5S = new PotionEffect(
            PotionEffectType.GLOWING,
            100,
            1,
            false,
            false,
            false
    );
    private static final PotionEffect GLOWING_2S = new PotionEffect(
            PotionEffectType.GLOWING,
            40,
            1,
            false,
            false,
            false
    );


    private final EventPlayer eventPlayer;

    private Role role;
    private double score;
    private int hitsTaken;
    private int ticksSurvived;

    private volatile MobDisguise rabbitDisguise;

    public NabbitPlayer(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.role = Role.FLEEING;
        this.score = 0.0;
        this.hitsTaken = 0;
        this.ticksSurvived = 0;
    }

    public void addTicksSurvived(int ticks) {
        this.ticksSurvived += ticks;
        if (this.ticksSurvived % 1200 == 0) {
            this.addScore(20.0);
            ExplorerListeners.fire(new NabbitSurviveExtendedTimePeriod(this.ticksSurvived), this.eventPlayer.getUuid());
        }
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
        this.endDisguise();

        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer != null) {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                ColorManager.updatePlayersColor(bukkitPlayer);
                bukkitPlayer.removePotionEffect(PotionEffectType.GLOWING);
            });
        }

        callback.run();
        // TODO: Handle score
        System.out.println(this.getName() + ", Score: " + this.score);
    }

    /**
     * Someone's been hit by a Nabbit!
     * @return true if the player was caught, false if they couldn't be caught.
     */
    public boolean tryNabbitCatch(NabbitPlayer victim, Runnable callback) {
        if (victim.role != Role.FLEEING) {
            return false;
        }

        victim.hitsTaken++;
        this.eventPlayer.sendActionBar(victim.getHitStars("<dark_purple>"));
        victim.eventPlayer.sendActionBar(victim.getHitStars("<red>"));

        if (victim.hitsTaken < MAX_HITS) {
            return false;
        }

        // 80% chance to become a rabbit, 20% chance to become a nabbit
        if (Math.random() < 0.8) {
            victim.changeRole(Role.RABBIT, true);
        } else {
            victim.changeRole(Role.NABBIT, true);
        }

        // Reward the catcher
        this.addScore(1.5);
        callback.run();
        return true;
    }

    public void changeRole(Role newRole, boolean sendRoleTitle) {
        if (this.role == newRole) {
            return;
        }

        this.role = newRole;

        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }

        switch (newRole) {
            case RABBIT -> {
                this.rabbitDisguise = new MobDisguise(DisguiseType.RABBIT);
                RabbitWatcher watcher = (RabbitWatcher) rabbitDisguise.getWatcher();
                watcher.setType(Util.getRandom(Rabbit.Type.values()));
                rabbitDisguise.setEntity(player);
                rabbitDisguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
                rabbitDisguise.setViewSelfDisguise(true);
                rabbitDisguise.setScalePlayerToDisguise(true);
                rabbitDisguise.startDisguise();
            }
            case NABBIT, NABBIT_BOOTSTRAP -> {
                this.endDisguise();
                this.addNabbitGlow();
            }
            case FLEEING -> {
                this.endDisguise();
            }
        }

        ExplorerListeners.fire(new NabbitChangeRole(this.role), player);
        if (sendRoleTitle) {
            this.sendRoleTitle();
        }
    }

    public void sendActionBarTip(int secsUntilNextLocReveal) {

        String nextLocReveal = "<gray>Next location reveal in <aqua>" + secsUntilNextLocReveal + "</aqua> seconds.";

        String str = switch (this.role) {
            case NABBIT_BOOTSTRAP, NABBIT -> "<dark_purple>Catch fleeing players! " +  nextLocReveal;
            case FLEEING -> "<green> Don't get caught by the Nabbits! " + nextLocReveal;
            case RABBIT -> "<green> Collect carrots!";
        };

        this.eventPlayer.sendActionBar(str);
    }

    public String getHitStars(String color) {
        // Show 3 stars. '★' is a hit, '☆' is a not-yet-hit out of 3
        StringBuilder stars = new StringBuilder(color);
        for (int i = 0; i < MAX_HITS; i++) {
            if (i < this.hitsTaken) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }

    public void sendRoleTitle() {
        eventPlayer.sendTitle(
                this.role.getTitle(),
                this.role.getSubtitle()
        );
    }

    public void addNabbitGlow() {
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return;
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            ColorManager.setTempPlayerColor(bukkitPlayer, ChatColor.DARK_PURPLE);
            bukkitPlayer.addPotionEffect(GLOWING_5S);
        });
    }

    public void addStandardGlow() {
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return;
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            ColorManager.setTempPlayerColor(bukkitPlayer, ChatColor.GREEN);
            bukkitPlayer.addPotionEffect(GLOWING_2S);
        });
    }

    private void endDisguise() {
        if (this.rabbitDisguise != null) {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                this.rabbitDisguise.stopDisguise();
                this.rabbitDisguise = null;
            });
        }
    }

    @Getter
    public enum Role {
        /**
         * The original Nabbit chosen at the beginning of the minigame.
         * <br />
         * If all nabbits disconnect from the game, we'll choose a new one and they'll become a bootstrap nabbit.
         * <br />
         * Earns points by catching FLEEING players.
         */
        NABBIT_BOOTSTRAP(
                1.4,
                "<dark_purple>The Nabbit",
                "<green>Catch as many players as you can."
        ),
        /**
         * A Nabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by catching FLEEING players.
         */
        NABBIT(1.0,
                "<dark_purple>You've become a Nabbit",
                "<green>Catch as many players as you can."
        ),
        /**
         * A Rabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by collecting carrots.
         */
        RABBIT(0.8,
                "<red>You've become a Rabbit",
                "<green>Collect carrots to score points."
        ),
        /**
         * A player that has not yet been caught by a Nabbit.
         * <br />
         * Earns points by collecting carrots and not being caught by the Nabbit.
         */
        FLEEING(1.4,
                "<green>Fleeing!",
                "<aqua>Run away and collect carrots."
        );

        private final double scoreMultiplier;
        private final String title;
        private final String subtitle;

        Role(double scoreMultiplier, String title, String subtitle) {
            this.scoreMultiplier = scoreMultiplier;
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}
