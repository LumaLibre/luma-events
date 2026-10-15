package dev.lumas.events.games.logic.manor2.roles;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactManager;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactRegion;
import dev.lumas.events.games.logic.manor2.attack.NiceRestraint;
import dev.lumas.events.games.logic.manor2.light.Visibility;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsRolePredicate;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class Artifact extends Manor2Role {

    private static final String FREEZE_MESSAGE = "<gray>You are <bold><gradient:aqua:white>FROZEN</gradient></bold> and can't move";
    private static final String DEATH_MESSAGE = "<gray>You died, and can only spectate";
    private static final String WAITING_FOR_REGION_MESSAGE = "<gray>Waiting for a safe region...";
    private static final String ARTIFACT_MESSAGE_1 = "<red>Hide from hunters!";
    private static final String ARTIFACT_MESSAGE_2 = "<red>Your soul is bound to an artifact";
    private static final NamespacedKey SCALE_CHANGE = Preconditions.checkNotNull(NamespacedKey.fromString(
        "manor2/artifact_scale", EventMain.getInstance()
    ));

    private static final int FREEZE_TICKS_INCREMENT = 5;
    private static final int FREEZE_TICKS_DECREEMENT = 10;

    private ArtifactRegion region = null;
    private Manor2 manor2;
    private AtomicLong revealReadyTime = new AtomicLong(Long.MAX_VALUE);
    private final Visibility visibility;
    private volatile boolean frozen = false;
    private volatile boolean isDead = false;

    protected Artifact(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.manor2 = manor2;
        this.visibility = new Visibility(manor2, eventPlayer);
    }

    @Override
    public void tick() {
        if (!(eventPlayer.getPlayer() instanceof Player player)) {
            return;
        }
        if (region == null) {
            ArtifactManager manager = manor2.getArtifactManager();
            manager.allocateRegion()
                .ifPresentOrElse(regionInfo -> {
                    this.region = regionInfo.region();
                    WorldBorder worldBorder = Bukkit.createWorldBorder();
                    worldBorder.setCenter(region.boundingBox().getCenterLocation());
                    worldBorder.setSize(region.boundingBox().getWidthX());
                    Executors.teleportSafely(player, regionInfo.position().toLocation().add(0.5, 0, 0.5))
                        .thenAcceptAsync(_ -> {
                            manor2.getRecorder().clearTrail(player);
                            revealReadyTime.set(System.nanoTime());
                        }, Executors.executor(player));
                    operatePlayer(_ -> {
                        player.setWorldBorder(worldBorder);
                        player.showTitle(Title.title(Util.color(ARTIFACT_MESSAGE_1), Util.color(ARTIFACT_MESSAGE_2)));
                    });
                }, () -> operatePlayer(player1 ->
                    player1.showTitle(Title.title(Component.empty(), Util.color(WAITING_FOR_REGION_MESSAGE), Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(1), Duration.ofSeconds(0))))
                ));
        }
        operatePlayer(player1 -> {
            int previous = player1.getFreezeTicks();
            visibility.setVanished(new IsRolePredicate(GhostHunter.class));
            if (frozen) {
                player1.setFreezeTicks(Math.min(previous + FREEZE_TICKS_INCREMENT, player.getMaxFreezeTicks()));
            } else {
                player1.setFreezeTicks(Math.max(previous - FREEZE_TICKS_DECREEMENT, 0));
            }
        });
    }

    @Override
    public void die() {
        if (isDead) {
            return;
        }
        isDead = true;
        eventPlayer.operatePlayer(player -> {
            player.sendActionBar(Util.color(DEATH_MESSAGE));
        });
        manor2.getRoleMap().swapRole(this, () -> new Spectator(manor2, eventPlayer));
    }

    @Override
    public void initiatePlay() {
        if (manor2.isStopping()) {
            return;
        }
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        player.setInvisible(true);
        if (scale != null) {
            scale.addModifier(new AttributeModifier(
                SCALE_CHANGE, 1d / player.getHeight() - 1, AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }

    @Override
    public void retire(Manor2 manor2) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        player.setInvisible(false);
        visibility.stop();
        NiceRestraint.release(player);
        player.setWorldBorder(null);
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.removeModifier(SCALE_CHANGE);
        }
        long delta = System.nanoTime() - started();
        if (delta > 0) {
            double minutes = 1e-9 * delta / 60;
            manor2.getScoreboard().addScore(eventPlayer, (int) (minutes * manor2.getDefinition().getPlayerArtifactAliveMinuteScore()));
        }
        super.retire(manor2);
    }

    public void freeze() {
        this.frozen = true;
        eventPlayer.operatePlayer(player -> {
            NiceRestraint.apply(player);
            player.sendActionBar(Util.color(FREEZE_MESSAGE));
        });
    }

    public long started() {
        return revealReadyTime.get();
    }

    public void unfreeze() {
        this.frozen = false;
        eventPlayer.operatePlayer(NiceRestraint::release);
    }
}
