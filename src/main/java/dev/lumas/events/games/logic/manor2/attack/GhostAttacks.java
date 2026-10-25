package dev.lumas.events.games.logic.manor2.attack;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.light.LightHit;
import dev.lumas.events.games.logic.manor2.light.RevealState;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GhostAttacks {

    private final Map<UUID, Grab> grabMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private volatile long currentTick = 0;
    private final Manor2 ctx;

    public GhostAttacks(Manor2 ctx) {
        this.ctx = ctx;
    }

    public boolean handleAttack(Player attacker, Entity attacked) {
        Ghost ghost = ctx.getRoleMap().as(attacker.getUniqueId(), Ghost.class);
        if (ghost == null || !(attacked instanceof Player victimPlayer)) return false;
        GhostHunter victim = ctx.getRoleMap().as(victimPlayer.getUniqueId(), GhostHunter.class);
        if (victim == null) return false;

        tryGrab(ghost, attacker, victim, victimPlayer);
        return true;
    }

    public boolean handleSwapHands(Player player) {
        Grab grab = grabMap.get(player.getUniqueId());
        if (grab == null) return false;

        if (grab.getGhost().getUuid().equals(player.getUniqueId())) {
            grab.backout(ctx);
        }
        return true;
    }

    public void onLit(Ghost ghost, LightHit hit, UUID shooter) {
        Grab grab = grabMap.get(ghost.getUuid());
        if (grab != null && grab.getGhost() == ghost) {
            grab.light(hit, shooter);
        }
    }

    public void tick() {
        currentTick++;
        for (Grab grab : Set.copyOf(grabMap.values())) {
            grab.tick(ctx);
        }
    }

    public void abort(UUID player) {
        Grab grab = grabMap.get(player);
        if (grab != null) {
            grab.abort(ctx);
        }
    }

    public void stop() {
        Set.copyOf(grabMap.values()).forEach(grab -> grab.abort(ctx));
    }

    private synchronized void tryGrab(Ghost ghost, Player ghostPlayer, GhostHunter victim, Player victimPlayer) {
        if (ctx.isStopping() || !isAvailable(ghost) || !isAvailable(victim) || ghost.getVisibility().getState() == RevealState.EXPOSED) {
            return;
        }

        Grab grab = new Grab(ghost, victim, ghostPlayer.getEyeLocation(), victimPlayer.getLocation());
        grabMap.put(ghost.getUuid(), grab);
        grabMap.put(victim.getUuid(), grab);
        grab.begin(ctx, ghostPlayer);
    }

    private boolean isAvailable(Manor2Role role) {
        return ctx.getParticipants().contains(role.getEventPlayer())
            && !grabMap.containsKey(role.getUuid())
            && cooldown.getOrDefault(role.getUuid(), 0L) <= currentTick;
    }

    public void cooldown(Manor2Role role, int ticks) {
        cooldown.merge(role.getUuid(), currentTick + ticks, Math::max);
    }

    public void releaseGrab(UUID target, Grab grab) {
        grabMap.remove(target, grab);
    }
}
