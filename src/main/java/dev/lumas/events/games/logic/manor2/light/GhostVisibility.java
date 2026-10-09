package dev.lumas.events.games.logic.manor2.light;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GhostVisibility {

    private static final int HIDE_AFTER_UNLIT_TICKS = 10;
    private static final int MAX_LIT_TICKS = 20 * 15;
    private static final double EXPOSED_MIN_CENTEREDNESS = 0.75;
    private static final double SILHOUETTE_MIN_CENTEREDNESS = 0.5;
    private static final int SILHOUETTE_MIN_LIGHT_LEVEL = 5;
    private static final int EXPOSED_MIN_LIGHT_LEVEL = 7;
    private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, true, false);
    private static final PotionEffect GLOW = new PotionEffect(PotionEffectType.GLOWING, 3, 0, true, true, false);
    private static final NamespacedKey SNOW_SPEED_MODIFIER_NAME = NamespacedKey.minecraft("powder_snow");

    private final Manor2 manor2;
    private final EventPlayer ghost;
    private final AtomicReference<@Nullable LightHit> pendingLight = new AtomicReference<>(); // strongest light from any hunter
    private volatile RevealState flashLight = RevealState.HIDDEN;
    private volatile RevealState staticLight = RevealState.HIDDEN;
    private int litTicks = 0;
    private int unlitTicks = 0;

    private final Set<UUID> hiddenFrom = ConcurrentHashMap.newKeySet();
    private final Object vanishLock = new Object(); // TODO: probably a better way to do this, i didnt want to deal with the race conditions - Jsinco
    private volatile boolean stopping = false;
    private final AtomicInteger forcedExposedTicks = new AtomicInteger();
    private final Map<UUID, Integer> shooterTicks = new ConcurrentHashMap<>();

    public GhostVisibility(Manor2 manor2, EventPlayer ghost) {
        this.manor2 = manor2;
        this.ghost = ghost;
    }

    public void light(LightHit hit, UUID shooter) {
        if (stopping) return;
        if (stateOf(hit) == RevealState.EXPOSED) {
            shooterTicks.merge(shooter, 1, Integer::sum);
        }
        pendingLight.accumulateAndGet(hit, (current, next) -> current == null || isStronger(next, current) ? next : current);
    }

    public void tick() {
        if (stopping) return;
        LightHit hit = pendingLight.getAndSet(null);
        RevealState previous = flashLight;
        RevealState newState = RevealState.HIDDEN;
        if (hit != null) {
            unlitTicks = 0;
            newState = stateOf(hit);
        }
        if (++unlitTicks < HIDE_AFTER_UNLIT_TICKS) {
            flashLight = newState == RevealState.EXPOSED ? RevealState.EXPOSED : previous.isRevealed() ? previous : newState;
        } else {
            flashLight = RevealState.HIDDEN;
        }

        if (forcedExposedTicks.getAndUpdate(ticks -> Math.max(0, ticks - 1)) > 0) {
            unlitTicks = 0;
            flashLight = RevealState.EXPOSED;
        }
        boolean changed = flashLight != previous;

        final RevealState current = flashLight;
        if (flashLight == RevealState.HIDDEN) {
            shooterTicks.clear();
        } else if (flashLight == RevealState.EXPOSED && manor2.getDefinition().getGhostRevealTime() <= shootTicks() && manor2.getRoleMap().as(ghost.getUuid(), Ghost.class) instanceof Ghost ghostGhost) {
            manor2.getScoreboard().ghostKilled(ghostGhost, shooterTicks);
            ghostGhost.die();
        }
        ghost.operatePlayer(player -> {
            if (stopping) return; // Released while this was queued
            this.staticLight = staticLight(player.getLocation());
            RevealState toUse = staticLight.moreRevealed(current) ? staticLight : current;
            if (toUse == RevealState.EXPOSED) {
                litTicks = Math.min(litTicks + 1, MAX_LIT_TICKS);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                if (changed) {
                    Location playerLocation = player.getLocation();
                    playerLocation.getWorld().spawnParticle(Particle.DUST_PILLAR, playerLocation, 10, BlockType.RED_CONCRETE.createBlockData());
                    Util.getRandom(manor2.getDefinition().getGhostRevealSounds()).playAt(player);
                }
            } else {
                player.addPotionEffect(INVISIBILITY);
                if (litTicks > 0) {
                    litTicks--;
                }
            }
            double litProgress = (double) litTicks / MAX_LIT_TICKS;
            player.setFreezeTicks((int) (player.getMaxFreezeTicks() - litProgress * player.getMaxFreezeTicks()));
            player.lockFreezeTicks(true);
            AttributeInstance instance = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (instance != null) {
                instance.removeModifier(SNOW_SPEED_MODIFIER_NAME);
            }
            if (toUse.isRevealed()) {
                player.addPotionEffect(GLOW);
                Location playerLocation = player.getLocation();
                playerLocation.getWorld().spawnParticle(Particle.DUST, playerLocation.clone().add(ThreadLocalRandom.current().nextDouble(-0.5, 0.5), ThreadLocalRandom.current().nextDouble(0, 2), ThreadLocalRandom.current().nextDouble(-0.5, 0.5)), 1, new Particle.DustOptions(Color.GRAY, 1f));
            } else {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }

            setVanished(player, toUse == RevealState.HIDDEN);
        });
    }

    public void stop() {
        List<UUID> toShow;
        synchronized (vanishLock) {
            if (stopping) return;
            stopping = true;
            toShow = List.copyOf(hiddenFrom);
        }
        pendingLight.set(null);
        flashLight = RevealState.EXPOSED;

        ghost.operatePlayer(player -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.GLOWING);
            player.lockFreezeTicks(false);
            player.setFreezeTicks(0);
        });

        Player self = ghost.getPlayer();
        if (self == null) return;
        for (UUID viewerId : toShow) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) {
                hiddenFrom.remove(viewerId);
                continue;
            }

            Executors.runSync(viewer, () -> showTo(viewer, self));
        }
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
        return shooterTicks.values().stream().reduce(0, Integer::sum);
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

    private void setVanished(Player self, boolean vanished) {
        for (Manor2Role role : manor2.getRoleMap()) {
            if (role instanceof Ghost) continue; // TODO: ghosts can always see each other?
            Player viewer = role.getEventPlayer().getPlayer();
            if (viewer == null || viewer == self) continue;

            boolean hide = vanished && manor2.getParticipants().contains(role.getEventPlayer());
            if (viewer.canSee(self) != hide) continue;

            role.operatePlayer(target -> {
                if (hide) hideFrom(target, self);
                else showTo(target, self);
            });
        }
    }

    private void hideFrom(Player viewer, Player self) {
        synchronized (vanishLock) {
            if (stopping) return;
            viewer.hidePlayer(EventMain.getInstance(), self);
            hiddenFrom.add(viewer.getUniqueId());
        }
    }

    private void showTo(Player viewer, Player self) {
        viewer.showPlayer(EventMain.getInstance(), self);
        hiddenFrom.remove(viewer.getUniqueId());
    }


}
