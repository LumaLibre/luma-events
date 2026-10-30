package dev.lumas.events.games.logic.manor2.light;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsNearPredicate;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsRolePredicate;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GhostVisibility {

    private static final int HIDE_AFTER_UNLIT_TICKS = 60;
    private static final double EXPOSED_MIN_CENTEREDNESS = 0.6;
    private static final double SILHOUETTE_MIN_CENTEREDNESS = 0.5;
    private static final int SILHOUETTE_MIN_LIGHT_LEVEL = 5;
    private static final int EXPOSED_MIN_LIGHT_LEVEL = 7;
    private static final double MAX_REVEAL_DISTANCE = 20;
    private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, true, false);
    private static final PotionEffect GLOW = new PotionEffect(PotionEffectType.GLOWING, 3, 0, true, true, false);
    private static final double MIN_LIT_HEALTH = 1.0;
    private static final NamespacedKey SPEED_BUFF_KEY = Preconditions.checkNotNull(NamespacedKey.fromString("manor2/extra_speed", EventMain.getInstance()));
    private static final double SPEED_BUFF_EXPOSED = 0.05;
    private static final double SPEED_BUFF_DAMAGED = 0.10;

    private final Manor2 manor2;
    private final Visibility visibility;
    private final EventPlayer ghost;
    private final AtomicReference<@Nullable LightHit> pendingLight = new AtomicReference<>(); // strongest light from any hunter
    private volatile RevealState flashLight = RevealState.HIDDEN;
    private volatile RevealState staticLight = RevealState.HIDDEN;
    private int unlitTicks = 0;
    private volatile boolean stopping = false;

    private final AtomicInteger forcedExposedTicks = new AtomicInteger();
    private final Map<UUID, Double> shooterTicks = new ConcurrentHashMap<>();

    public GhostVisibility(Manor2 manor2, EventPlayer ghost) {
        this.manor2 = manor2;
        this.ghost = ghost;
        this.visibility = new Visibility(manor2, ghost);
    }

    public void light(LightHit hit, UUID shooter) {
        if (stopping) return;
        if (stateOf(hit) == RevealState.EXPOSED) {
            shooterTicks.merge(shooter, 1D, Double::sum);
        }
        pendingLight.accumulateAndGet(hit, (current, next) -> current == null || isStronger(next, current) ? next : current);
    }

    public void tick() {
        if (stopping) return;
        LightHit hit = pendingLight.getAndSet(null);
        RevealState previous = flashLight;
        final RevealState newState;
        if (hit != null) {
            unlitTicks = 0;
            newState = stateOf(hit);
        } else {
            newState = RevealState.HIDDEN;
        }
        if (++unlitTicks < HIDE_AFTER_UNLIT_TICKS) {
            flashLight = previous.moreRevealed(newState) ? previous : newState;
        } else {
            flashLight = RevealState.HIDDEN;
        }

        if (forcedExposedTicks.getAndUpdate(ticks -> Math.max(0, ticks - 1)) > 0) {
            unlitTicks = 0;
            flashLight = RevealState.EXPOSED;
        }
        boolean changed = flashLight != previous;

        final RevealState current = flashLight;
        final double litProgress = Math.min(1.0, (double) shootTicks() / manor2.getDefinition().getGhostRevealTime());
        if (flashLight == RevealState.HIDDEN) {
            decreaseShoot();
        } else if (flashLight == RevealState.EXPOSED && manor2.getDefinition().getGhostRevealTime() <= shootTicks() && manor2.getRoleMap().as(ghost.getUuid(), Ghost.class) instanceof Ghost ghostGhost) {
            manor2.getScoreboard().ghostKilled(ghostGhost, shooterTicks);
            ghostGhost.die();
        }
        ghost.operatePlayer(player -> {
            if (stopping) return; // Released while this was queued
            this.staticLight = staticLight(player.getLocation());
            RevealState toUse = staticLight.moreRevealed(current) ? staticLight : current;
            if (toUse == RevealState.EXPOSED) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                if (changed) {
                    Location playerLocation = player.getLocation();
                    playerLocation.getWorld().spawnParticle(Particle.DUST_PILLAR, playerLocation, 10, BlockType.RED_CONCRETE.createBlockData());
                    Util.getRandom(manor2.getDefinition().getGhostRevealSounds()).playAt(player);
                }
                if (newState == RevealState.EXPOSED) {
                    speedModifier(player, SPEED_BUFF_DAMAGED);
                } else {
                    speedModifier(player, SPEED_BUFF_EXPOSED);
                }
            } else {
                player.addPotionEffect(INVISIBILITY);
                speedModifier(player, 0D);
            }
            if (toUse.isRevealed()) {
                player.addPotionEffect(GLOW);
                Location playerLocation = player.getLocation();
                playerLocation.getWorld().spawnParticle(Particle.DUST, playerLocation.clone().add(ThreadLocalRandom.current().nextDouble(-0.5, 0.5), ThreadLocalRandom.current().nextDouble(0, 2), ThreadLocalRandom.current().nextDouble(-0.5, 0.5)), 1, new Particle.DustOptions(Color.GRAY, 1f));
            } else {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }

            showLitDamage(player, litProgress);

            visibility.setVanished(new IsRolePredicate(GhostHunter.class)
                .and(new IsNearPredicate(player.getLocation(), MAX_REVEAL_DISTANCE).negate()
                    .or(ignored -> !toUse.isRevealed())
                )
            );
        });
    }

    public void speedModifier(Player player, double amountAdded) {
        AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        instance.removeModifier(SPEED_BUFF_KEY);
        if (amountAdded == 0) {
            return;
        }
        instance.addModifier(new AttributeModifier(SPEED_BUFF_KEY, amountAdded, AttributeModifier.Operation.ADD_NUMBER));
    }

    public void decreaseShoot() {
        Set<UUID> toRemove = new HashSet<>();
        double decreement = 1D / shooterTicks.size();
        for (Map.Entry<UUID, Double> doubleEntry : shooterTicks.entrySet()) {
            double previous = doubleEntry.getValue();
            if (previous < decreement) {
                toRemove.add(doubleEntry.getKey());
            } else {
                doubleEntry.setValue(doubleEntry.getValue() - decreement);
            }
        }
        toRemove.forEach(shooterTicks::remove);
    }

    public void stop() {
        stopping = true;
        visibility.stop();
        pendingLight.set(null);
        flashLight = RevealState.EXPOSED;

        ghost.operatePlayer(player -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.GLOWING);
            speedModifier(player, 0D);
            player.setHealth(maxHealth(player));
        });
    }

    private static void showLitDamage(Player player, double litProgress) {
        double maxHealth = 20.0;
        double target = Math.max(MIN_LIT_HEALTH, maxHealth * (1.0 - litProgress));
        double fixedTarget = (double) Math.round(target * 2) / 2;
        double health = player.getHealth();
        if (fixedTarget < health) {
            player.damage(health - fixedTarget, DamageSource.builder(DamageType.MAGIC).build());
        } else if (fixedTarget > health) {
            player.setHealth(target);
        }
    }

    private static double maxHealth(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        return instance != null ? instance.getValue() : 20.0;
    }

    public void exposeFor(int ticks) {
        forcedExposedTicks.accumulateAndGet(ticks, Math::max);
    }

    public RevealState getState() {
        RevealState staticLight = this.staticLight;
        RevealState flashLight = this.flashLight;
        return staticLight.moreRevealed(flashLight) ? staticLight : flashLight;
    }

    public int shootTicks() {
        return (int) Math.floor(shooterTicks.values().stream().reduce(0D, Double::sum));
    }

    private RevealState staticLight(Location location) {
        int lightLevel = location.getBlock().getLightFromBlocks();
        if (lightLevel > EXPOSED_MIN_LIGHT_LEVEL) {
            return RevealState.EXPOSED;
        }
        if (lightLevel > SILHOUETTE_MIN_LIGHT_LEVEL) {
            return RevealState.SILHOUETTE;
        }
        return RevealState.HIDDEN;
    }

    private static RevealState stateOf(LightHit hit) {
        if (hit.centeredness() < SILHOUETTE_MIN_CENTEREDNESS) return RevealState.HIDDEN;
        boolean exposed = hit.centeredness() >= EXPOSED_MIN_CENTEREDNESS || hit.lightLevel() >= EXPOSED_MIN_LIGHT_LEVEL;
        return exposed ? RevealState.EXPOSED : RevealState.SILHOUETTE;
    }

    private static boolean isStronger(LightHit hit, LightHit than) {
        int byState = stateOf(hit).compareTo(stateOf(than));
        return byState != 0 ? byState > 0 : hit.lightLevel() > than.lightLevel();
    }


}
