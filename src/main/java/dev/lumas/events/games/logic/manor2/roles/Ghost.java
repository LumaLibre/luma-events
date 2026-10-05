package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.Team;
import dev.lumas.events.games.logic.manor2.light.GhostVisibility;
import dev.lumas.events.games.logic.manor2.light.LightHit;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class Ghost extends Manor2Role {

    private static final int WHIRL_INTERVAL_TICKS = 80;
    private static final float WHIRL_VOLUME = 0.6f;

    @Getter
    private final GhostVisibility visibility;
    private int revealedTime = 0;
    private Manor2 manor2;
    private int ticksUntilWhirl = 0;

    public Ghost(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.visibility = new GhostVisibility(manor2, eventPlayer);
        this.manor2 = manor2;
        eventPlayer.operatePlayer(player -> {
            GlowColorManager.getInstance().setTransientColor(player, NamedTextColor.DARK_GRAY);
            manor2.getPacketGlow().addViewer(player);
        });
    }

    @Override
    public void die() {
        retire(manor2); // TODO: extract parts of this
        manor2.getRoleMap().swapRole(this, () -> new Spectator(manor2, eventPlayer));
    }

    private void showRevealedResult() {
        eventPlayer.operatePlayer(player -> {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double newHealth = maxHealth.getValue() * (1 - (double) revealedTime / manor2.getDefinition().getGhostRevealTime());
                player.setHealth(newHealth);
            } else {
                EventMain.getInstance().getLogger().warning("Player has no max health attribute - can not take damage");
            }
        });
    }

    @Override
    public CompletableFuture<Void> initiatePlay() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        eventPlayer.operatePlayer(player -> {
            player.showTitle(Title.title(
                    MiniMessage.miniMessage().deserialize(
                            manor2.getDefinition().getMessages().getTeamAssignmentShowMessage(),
                            Placeholder.component("team", Team.GHOSTS.displayName(manor2.getDefinition().getMessages()))
                    ),
                    Util.color(manor2.getDefinition().getMessages().getGhostDescription())
            ));
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> retire(Manor2 manor2) {
        visibility.stop();
        return super.retire(manor2);
    }

    @Override
    public void tick() {
        visibility.tick();
        whirl();

        if (visibility.getState().isRevealed()) {
            revealedTime++;
        } else {
            revealedTime = 0; // <- TODO: intended?
        }
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
