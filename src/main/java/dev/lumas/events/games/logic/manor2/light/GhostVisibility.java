package dev.lumas.events.games.logic.manor2.light;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GhostVisibility {

    private static final boolean DEBUG = false;
    private static final int HIDE_AFTER_UNLIT_TICKS = 10;
    private static final double EXPOSED_MIN_CENTEREDNESS = 0.75;
    private static final double SILHOUETTE_MIN_CENTEREDNESS = 0.5;
    private static final int EXPOSED_MIN_LIGHT_LEVEL = 7;
    private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, true, false);
    private static final PotionEffect GLOW = new PotionEffect(PotionEffectType.GLOWING, 3, 0, true, true, false);

    private final Manor2 manor2;
    private final EventPlayer ghost;
    private final AtomicReference<@Nullable LightHitRecord> pendingLight = new AtomicReference<>(); // strongest light from any hunter
    @Getter
    private volatile RevealState state = RevealState.HIDDEN;
    private int unlitTicks = 0;

    private final Set<UUID> hiddenFrom = ConcurrentHashMap.newKeySet();
    private final Object vanishLock = new Object(); // TODO: probably a better way to do this, i didnt want to deal with the race conditions - Jsinco
    private volatile boolean stopping = false;
    private final AtomicInteger forcedExposedTicks = new AtomicInteger();

    public GhostVisibility(Manor2 manor2, EventPlayer ghost) {
        this.manor2 = manor2;
        this.ghost = ghost;
    }

    public void light(LightHit hit, UUID playerUuid) {
        if (stopping) return;
        LightHitRecord entry = new LightHitRecord(hit, playerUuid);
        pendingLight.accumulateAndGet(entry, (current, next) -> current == null || isStronger(next.hit(), current.hit()) ? next : current);
    }

    public void tick() {
        if (stopping) return;
        LightHitRecord hitRecord = pendingLight.getAndSet(null);
        LightHit hit = hitRecord == null ? null : hitRecord.hit();
        RevealState previous = state;
        if (hit != null) {
            unlitTicks = 0;
            state = stateOf(hit);
        } else if (++unlitTicks >= HIDE_AFTER_UNLIT_TICKS) {
            state = RevealState.HIDDEN;
        }
        if (forcedExposedTicks.getAndUpdate(ticks -> Math.max(0, ticks - 1)) > 0) {
            unlitTicks = 0;
            state = RevealState.EXPOSED;
        }
        boolean changed = state != previous;

        if (DEBUG) {
            Bukkit.broadcastMessage("Ghost visibility tick (" + ghost.getName() + "): " + state + ", unlitTicks: " + unlitTicks + (hit == null
                    ? ", not lit"
                    : String.format(", distance: %.2f, centeredness: %.2f, light level: %d", hit.distance(), hit.centeredness(), hit.lightLevel())));
        }
        final RevealState current = state;
        ghost.operatePlayer(player -> {
            // ghost.tickDamage(hitRecord.shooter());
            if (stopping) return; // Released while this was queued
            if (current == RevealState.EXPOSED) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                if (changed) {
                    Location playerLocation = player.getLocation();
                    playerLocation.getWorld().spawnParticle(Particle.DUST_PILLAR, playerLocation, 10, BlockType.RED_CONCRETE.createBlockData());
                }
            } else {
                player.addPotionEffect(INVISIBILITY);
            }

            if (current.isRevealed()) {
                player.addPotionEffect(GLOW);
                Location playerLocation = player.getLocation();
                playerLocation.getWorld().spawnParticle(
                        Particle.DUST,
                        playerLocation.clone().add(
                                ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                                ThreadLocalRandom.current().nextDouble(0, 2),
                                ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                        ),
                        1,
                        new Particle.DustOptions(Color.GRAY, 1f)
                );
            } else {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }

            setVanished(player, current == RevealState.HIDDEN);
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
        state = RevealState.EXPOSED;

        ghost.operatePlayer(player -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.GLOWING);
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
