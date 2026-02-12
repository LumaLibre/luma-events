package dev.lumas.events.games.obj;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.colormanagers.ColorManager;
import lombok.Getter;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.RabbitWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Particle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.function.Consumer;

@Getter
public class NabbitPlayer implements Scorer {

    private static final int MAX_HITS = 3;
    private static final ChatColor[] ALT_COLORS = {
            ChatColor.RED,
            ChatColor.GOLD,
            ChatColor.YELLOW,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.BLUE,
            ChatColor.DARK_BLUE,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_GREEN,
            ChatColor.WHITE,
            ChatColor.GRAY,
            ChatColor.DARK_GRAY,
            ChatColor.BLACK,
    };
    private static final PotionEffect GLOWING_5S = new PotionEffect(
            PotionEffectType.GLOWING,
            100,
            1,
            false,
            false,
            false
    );
    private static final PotionEffect GLOWING_1S = new PotionEffect(
            PotionEffectType.GLOWING,
            20,
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
    }


    public boolean isNabbit() {
        return this.role == Role.NABBIT || this.role == Role.NABBIT_BOOTSTRAP;
    }

    public String getName() {
        Player player = this.eventPlayer.getPlayer();
        if (player == null) {
            return "OfflinePlayer@" + this.eventPlayer.hashCode();
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
    }

    /**
     * Someone's been hit by a Nabbit!
     * @return true if the player was caught, false if they couldn't be caught.
     */
    public boolean tryNabbitCatch(NabbitPlayer victim, Consumer<Role> consumer) {
        if (victim.role != Role.FLEEING) {
            return false;
        }

        victim.hitsTaken++;
        this.eventPlayer.sendActionBar(victim.getHitStars("<dark_purple>"));
        victim.eventPlayer.sendActionBar(victim.getHitStars("<red>"));
        Player p = victim.eventPlayer.getPlayer();
        if (p != null) {
            p.getWorld().spawnParticle(Particle.CRIT, p.getLocation(), 8, 0.5, 0.5, 0.5, 0.1);
        }

        if (victim.hitsTaken < MAX_HITS) {
            return false;
        }

        //
        if (Math.random() < 0.5) {
            victim.changeRole(Role.RABBIT, true);
        } else {
            victim.changeRole(Role.NABBIT, true);
        }

        // Reward the catcher
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer != null) {
            Firework firework = bukkitPlayer.getWorld().spawn(bukkitPlayer.getLocation(), Firework.class);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.addEffect(
                    FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL)
                            .withFlicker()
                            .withTrail()
                            .withColor(Color.PURPLE)
                            .build()
            );
            fireworkMeta.setPower(1);
            firework.setFireworkMeta(fireworkMeta);
            firework.detonate();
        }
        consumer.accept(victim.role);
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
                this.addNabbitEffects();
            }
            case FLEEING -> {
                this.endDisguise();
            }
        }

        if (sendRoleTitle) {
            this.sendRoleTitle();
        }
    }

    public void sendActionBarTip(int secsUntilNextLocReveal) {

        String nextLocReveal = "<aqua>" + secsUntilNextLocReveal + "</aqua>s";

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

    public void addNabbitEffects() {
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return;
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            ColorManager.setTempPlayerColor(bukkitPlayer, ChatColor.DARK_PURPLE);
            bukkitPlayer.addPotionEffect(GLOWING_5S);
            bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, false, false));
        });
    }

    public void addStandardGlow() {
        Player bukkitPlayer = this.eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return;
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            ColorManager.setTempPlayerColor(bukkitPlayer, Util.getRandom(ALT_COLORS));
            bukkitPlayer.addPotionEffect(GLOWING_1S);
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
                1.2,
                "<dark_purple>The Nabbit",
                "<green>Catch as many players as you can."
        ),
        /**
         * A Nabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by catching FLEEING players.
         */
        NABBIT(0.8,
                "<dark_purple>You've become a Nabbit",
                "<green>Catch as many players as you can."
        ),
        /**
         * A Rabbit that has been created by a Nabbit catching a player.
         * <br />
         * Earns points by collecting carrots.
         */
        RABBIT(0.5,
                "<red>You've become a Rabbit",
                "<green>Collect carrots to score points."
        ),
        /**
         * A player that has not yet been caught by a Nabbit.
         * <br />
         * Earns points by collecting carrots and not being caught by the Nabbit.
         */
        FLEEING(1.0,
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
