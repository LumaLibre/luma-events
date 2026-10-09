package dev.lumas.events.games.logic.manor2;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.game.Manor2Definition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.packet.PacketGlow;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactEntity;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactManager;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactPickupResult;
import dev.lumas.events.games.logic.manor2.attack.GhostAttacks;
import dev.lumas.events.games.logic.manor2.light.Flashlight;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.games.logic.manor2.roles.RoleHitbox;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import dev.lumas.events.utility.latency.LatencyHandler;
import dev.lumas.events.utility.latency.LatencyTracker;
import dev.lumas.events.utility.latency.hitbox.TrailRecorder;
import dev.lumas.events.utility.latency.tracing.Tracing;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class Manor2 extends InventoryUnifiedMinigame {
    private static final String[] SPLASH = {
        "Are you afraid of ghosts?",
        "Boooooooooooo!!!!",
        "Ghost Busters!",
        "Who you gonna call?",
        "House of Dracula!",
        "Trick or treat?",
        "Here's Johny!!!",
        "Put these foolish ambitions to rest",
        "Wazzzuup!",
        "Did you pray enough?",
        "It's alive! It's alive!",
        "Be afraid. Be very afraid",
        "What is the color of night?",
        "This is not a dream!",
        "Someone must extinguish thy flame.",
        "Don't cross the streams!!!!"
    };
    private static final long TICK_INTERVAL = 1L;
    private static final int TELEPORTS_PER_TICK = 5;
    private static final long END_DELAY_SECONDS = 10;
    public static final int TRAIL_HISTORY_TICKS = 20;
    public static final NamespacedKey ITEM_TAG = Preconditions.checkNotNull(
        NamespacedKey.fromString("manor2/item", EventMain.getInstance())
    );
    @Getter
    private final Manor2Definition definition;

    @Getter
    private final MinigameRoleMap<Manor2Role> roleMap = new MinigameRoleMap<>(role -> role.retire(this), Manor2Role::initiatePlay);
    @Getter
    private final LocalizedFakeBlockCache blockCache = new LocalizedFakeBlockCache(32);
    @Getter
    private final LatencyTracker latency = new LatencyTracker(this::onlineParticipants);
    @Getter
    private final LatencyHandler latencyHandler;
    @Getter
    private final TrailRecorder recorder = new TrailRecorder(TRAIL_HISTORY_TICKS, RoleHitbox.hitboxProvider(roleMap), this::involvedEntities);
    @Getter
    private final PacketGlow packetGlow = PacketGlow.newInstance();
    @Getter
    private final Tracing tracing;
    @Getter
    private final GhostAttacks ghostAttacks = new GhostAttacks(this);
    @Getter
    private final ManorScoreboard scoreboard = new ManorScoreboard(this);
    @Getter
    private final ArtifactManager artifactManager;

    private CountdownBossBar timer;

    public Manor2(Manor2Definition definition) {
        super("Manor 2", Util.getRandom(SPLASH), definition.getDuration(), TICK_INTERVAL, true, true, false, false);
        this.definition = definition;
        this.artifactManager = new ArtifactManager(this);
        Manor2Definition.LagCompensationSettings lagCompensation = definition.getLagCompensation();
        this.latencyHandler = new LatencyHandler(latency, lagCompensation.isEnabled(), lagCompensation.getMaxRewindNano(), lagCompensation.getInterpolationMillis());
        this.tracing = new Tracing(this::involvedEntities, recorder, latencyHandler);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: Event tokens
    }

    @Override
    protected void handleStart() {
        this.latency.start(); // No-op if already started
        Collections.shuffle(participants);
        int ghostsToSpecify = Math.ceilDiv(participants.size(), definition.getHunterRatio());
        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i).getPlayer();
            if (player == null) {
                continue;
            }
            Manor2Role role;
            List<Location> teleportTargets;
            if (ghostsToSpecify-- > 0) {
                role = new Ghost(this, participants.get(i));
                scoreboard.registerTeam(role.getUuid(), ManorTeam.GHOSTS);
                teleportTargets = definition.getGhostStartLocations();
            } else {
                role = new GhostHunter(this, participants.get(i));
                scoreboard.registerTeam(role.getUuid(), ManorTeam.HUNTERS);
                teleportTargets = definition.getHunterStartLocations();
            }
            roleMap.put(role);
            Location target = teleportTargets.get(i % teleportTargets.size());
            Executors.delayedSync(player, Math.floorDiv(i, TELEPORTS_PER_TICK) + 1, () -> {
                    Executors.teleportSafely(player, target);
                    role.initiatePlay();
                }
            );
        }
        showParticipantsToEachOther();
        packetGlow.start();
        Executors.repeatingSync(definition.getHunterStartLocations().getFirst(), 1L, task -> {
            if (stopping) {
                task.cancel();
                return;
            }
            blockCache.showSync();
        });

        timer = CountdownBossBar.builder()
            .title("<white><b>Time Remaining: %ss")
            .color(BossBar.Color.WHITE)
            .miliseconds(getDuration())
            .audience(audience)
            .build();
        timer.start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (timeLeft <= TimeUnit.SECONDS.toMillis(END_DELAY_SECONDS)) {
            this.stop();
        }
        recorder.tick();
        ghostAttacks.tick();
        artifactManager.tick();
        for (Manor2Role role : roleMap) {
            role.tick();
            role.operatePlayer(player -> {
                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(20);
                }
            });
        }

        boolean gameOver = roleMap.getMatching(Ghost.class).isEmpty() || roleMap.getMatching(GhostHunter.class).isEmpty();
        ManorTeam winner = roleMap.getMatching(Ghost.class).isEmpty() ? ManorTeam.HUNTERS : ManorTeam.GHOSTS;
        if (false && gameOver) {
            sendGameOver(winner);
            this.stop();
        }
    }

    @Override
    protected void handleStop() {
        if (timer != null) {
            timer.stop(false);
        }

        packetGlow.stop();
        ghostAttacks.stop();
        scoreboard.showScores(audience);
        recorder.stopRecording();

        Executors.sync(definition.getHunterStartLocations().getFirst(), blockCache::shutdown);

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(CompletableFuture.runAsync(() -> role.retire(this), Executors.executor(role)));
        }
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
            .exceptionally(exception -> {
                exception.printStackTrace();
                return null;
            });

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.latency.start(); // No-op if already started
        player.teleportAsync(definition.getLobby());
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer player, boolean doTeleport) {
        if (player.getPlayer() != null && this.timer != null) {
            this.timer.getBossBar().removeViewer(player.getPlayer());
        }
        if (roleMap.remove(player.getUuid()) instanceof Manor2Role role) {
            Executors.runSync(player, () -> role.retire(this));
        }
        return super.removeParticipant(player, doTeleport);
    }

    @EventHandler
    public void onTridentThrow(PlayerLaunchProjectileEvent event) {
        this.ensureNotIllegal();
        if (!(event.getProjectile() instanceof Trident)) return;
        if (!Flashlight.isFlashlight(event.getItemStack())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onGhostAttack(PrePlayerAttackEntityEvent event) {
        this.ensureNotIllegal();
        if (ghostAttacks.handleAttack(event.getPlayer(), event.getAttacked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        this.ensureNotIllegal();
        if (ghostAttacks.handleSwapHands(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        this.ensureNotIllegal();
        if (roleMap.as(event.getPlayer().getUniqueId(), GhostHunter.class) instanceof GhostHunter ghostHunter) {
            ArtifactEntity.fromEntity(event.getRightClicked())
                .ifPresent(item -> {
                    ArtifactPickupResult result = artifactManager.pickup(event.getRightClicked().getLocation());
                    if (result != ArtifactPickupResult.NOT_FOUND) {
                        event.getPlayer().getInventory().addItem(item);
                        if (result == ArtifactPickupResult.FOUND_PLAYER_ARTIFACT) {
                            scoreboard.playerArtifactFound(ghostHunter);
                        } else {
                            scoreboard.artifactFound(ghostHunter);
                        }
                    } else {
                        event.getRightClicked().getLocation()
                            .getNearbyEntities(1, 1, 1)
                            .stream()
                            .filter(ArtifactEntity::isArtifact)
                            .forEach(Entity::remove);
                    }
                });
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PrePlayerAttackEntityEvent event) {
        this.ensureNotIllegal();
        if (roleMap.as(event.getPlayer().getUniqueId(), GhostHunter.class) instanceof GhostHunter ghostHunter) {
            ArtifactEntity.fromEntity(event.getAttacked())
                .ifPresent(item -> {
                    ArtifactPickupResult result = artifactManager.pickup(event.getAttacked().getLocation());
                    if (result != ArtifactPickupResult.NOT_FOUND) {
                        event.getPlayer().getInventory().addItem(item);
                        if (result == ArtifactPickupResult.FOUND_PLAYER_ARTIFACT) {
                            scoreboard.playerArtifactFound(ghostHunter);
                        } else {
                            scoreboard.artifactFound(ghostHunter);
                        }
                    } else {
                        event.getAttacked().getLocation()
                            .getNearbyEntities(1, 1, 1)
                            .stream()
                            .filter(ArtifactEntity::isArtifact)
                            .forEach(Entity::remove);
                    }
                });
        }
        event.setCancelled(true);
    }

    private void sendGameOver(ManorTeam winner) {
        sendAudienceTitle("<yellow>Game Over", "<gold>" + winner.displayName() + " has won!");
    }

    private void showParticipantsToEachOther() {
        List<Player> players = onlineParticipants();
        for (Player viewer : players) {
            Executors.runSync(viewer, () -> {
                for (Player other : players) {
                    if (other != viewer) viewer.showPlayer(EventMain.getInstance(), other);
                }
            });
        }
    }

    private List<Player> onlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player != null) players.add(player);
        }
        return players;
    }

    public Collection<LivingEntity> involvedEntities() {
        return this.getParticipants().stream()
            .map(EventPlayer::getPlayer)
            .filter(Objects::nonNull)
            .map(LivingEntity.class::cast)
            .toList();
    }
}
