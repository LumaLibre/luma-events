package dev.lumas.events.games.logic.manor2.roles;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactManager;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactRegion;
import dev.lumas.events.games.logic.manor2.attack.NiceRestraint;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicLong;

public class Artifact extends Manor2Role {

    private static final String FREEZE_MESSAGE = "<gray>You are <bold><gradient:aqua:white>FROZEN</gradient></bold> and can't move";
    private static final String DEATH_MESSAGE = "<gray>You died, and can only spectate";
    private static final NamespacedKey FROZEN_ATTRIBUTE = Preconditions.checkNotNull(NamespacedKey.fromString("frozen", EventMain.getInstance()));

    private ArtifactRegion region = null;
    private Manor2 manor2;
    private AtomicLong revealReadyTime = new AtomicLong(Long.MAX_VALUE);

    protected Artifact(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.manor2 = manor2;
    }

    @Override
    public void tick() {
        if ((region == null)
            && eventPlayer.getPlayer() instanceof Player player
        ) {
            ArtifactManager manager = manor2.getArtifactManager();
            manager.allocateRegion()
                .ifPresent(region -> {
                    this.region = region;
                    WorldBorder worldBorder = Bukkit.createWorldBorder();
                    worldBorder.setCenter(region.boundingBox().getCenterLocation());
                    worldBorder.setSize(region.boundingBox().getWidthX());
                    Bukkit.broadcastMessage("center = %s, width = %s".formatted(
                        worldBorder.getCenter(),
                        worldBorder.getSize()
                    ));
                    Executors.teleportSafely(player, region.randomPos().toLocation())
                        .thenAcceptAsync(_ -> {
                            manor2.getRecorder().clearTrail(player);
                            revealReadyTime.set(System.nanoTime());
                        });
                    operatePlayer(_ -> player.setWorldBorder(worldBorder));
                });
        }
    }

    @Override
    public void die() {
        eventPlayer.operatePlayer(player -> {
            player.sendActionBar(Util.color(DEATH_MESSAGE));
        });
        manor2.getRoleMap().swapRole(this, () -> new Spectator(manor2, eventPlayer));
    }

    @Override
    public void initiatePlay() {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        player.setInvisible(true);
    }

    @Override
    public void retire(Manor2 manor2) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        NiceRestraint.release(player);
        player.setInvisible(false);
        player.setWorldBorder(null);
        super.retire(manor2);
    }

    public void freeze() {
        eventPlayer.operatePlayer(NiceRestraint::apply);
    }

    public long revealReady() {
        return revealReadyTime.get();
    }

    public void unfreeze() {
        eventPlayer.operatePlayer(NiceRestraint::release);
    }

}
