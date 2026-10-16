package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.ManorTeam;
import dev.lumas.events.games.logic.manor2.light.Flashlight;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.latency.tracing.ProximityTraceResult;
import dev.lumas.events.utility.latency.tracing.TraceShooter;
import dev.lumas.glowapi.model.GlowColorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class GhostHunter extends Manor2Role {

    private int ghostConsumedTime = 0;
    private static final int FROST_PER_TICK = 4;
    private static final double HAUNT_RADIUS = 10.0;

    private final Flashlight flashlight;
    private final Manor2 manor2;
    private volatile boolean isDead = false;

    public GhostHunter(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.manor2 = manor2;
        this.flashlight = new Flashlight(manor2, eventPlayer.getPlayer());
    }

    @Override
    public void tick() {
        eventPlayer.operatePlayer(player -> {
            double ghostCloseness = hauntCloseness(player);
            flashlight.tick(ghostCloseness);
            freeze(player, ghostCloseness);
        });
    }

    @Override
    public void die() {
        if (manor2.isStopping()) {
            return;
        }
        if (isDead) {
            return;
        }
        isDead = true;
        manor2.getRoleMap().swapRole(this, () ->
            manor2.getDefinition().isArtifactPlay() ? new Artifact(manor2, eventPlayer) : new Spectator(manor2, eventPlayer)
        );
    }

    @Override
    public void initiatePlay() {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        player.getInventory().setItem(0, flashlight.createItem());
        player.showTitle(ManorTeam.HUNTERS.assignmentTitle());
        GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.WHITE);
        manor2.getPacketGlow().addTarget(player);
    }

    private void freeze(Player player, double ghostCloseness) {
        int target = (int) Math.round((player.getMaxFreezeTicks() - 1) * ghostCloseness);
        int current = player.getFreezeTicks();
        if (current < target) {
            player.setFreezeTicks(Math.min(target, current + FROST_PER_TICK));
        }
    }

    private double hauntCloseness(Player player) {
        Collection<ProximityTraceResult> result = manor2.getTracing().traceProximity(
            new TraceShooter.PlayerTraceShooter(player),
            participant -> manor2.getRoleMap().as(participant.getUniqueId(), Ghost.class) != null,
            HAUNT_RADIUS
        );
        double closestProximity = result.stream().map(ProximityTraceResult::distance)
            .min(Double::compare)
            .orElse(HAUNT_RADIUS);
        return 1 - (closestProximity / HAUNT_RADIUS);
    }
}
