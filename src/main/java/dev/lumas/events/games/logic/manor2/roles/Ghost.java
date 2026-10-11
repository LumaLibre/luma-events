package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.ManorTeam;
import dev.lumas.events.games.logic.manor2.light.GhostVisibility;
import dev.lumas.events.games.logic.manor2.light.LightHit;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.glowapi.model.GlowColorManager;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Ghost extends Manor2Role {

    private static final int WHIRL_INTERVAL_TICKS = 80;
    private static final float WHIRL_VOLUME = 0.6f;

    @Getter
    private final GhostVisibility visibility;
    private Manor2 manor2;
    private int ticksUntilWhirl = 0;

    public Ghost(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.visibility = new GhostVisibility(manor2, eventPlayer);
        this.manor2 = manor2;
    }

    @Override
    public void die() {
        if (manor2.getDefinition().isArtifactPlay()) {
            manor2.getRoleMap().swapRole(this, () -> new Artifact(manor2, eventPlayer));
        } else {
            manor2.getRoleMap().swapRole(this, () -> new Spectator(manor2, eventPlayer));
        }
    }

    @Override
    public void initiatePlay() {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        player.showTitle(ManorTeam.GHOSTS.assignmentTitle());
        GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.DARK_GRAY);
        manor2.getPacketGlow().addViewer(player);
    }

    @Override
    public void retire(Manor2 manor2) {
        visibility.stop();
        super.retire(manor2);
    }

    @Override
    public void tick() {
        visibility.tick();
        whirl();
    }

    public void light(LightHit hit, UUID playerUuid) {
        visibility.light(hit, playerUuid);
        manor2.getGhostAttacks().onLit(this, hit, playerUuid);
    }

    private void whirl() {
        if (--ticksUntilWhirl > 0 || !manor2.getParticipants().contains(eventPlayer)) return;
        ticksUntilWhirl = WHIRL_INTERVAL_TICKS;

        float pitch = 0.8f + (ThreadLocalRandom.current().nextFloat() * 0.4f);
        operatePlayer(player -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WHIRL, SoundCategory.HOSTILE, WHIRL_VOLUME, pitch));
    }
}
