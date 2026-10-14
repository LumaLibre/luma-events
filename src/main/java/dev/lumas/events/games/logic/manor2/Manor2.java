package dev.lumas.events.games.logic.manor2;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.game.Manor2Definition;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.models.InactivitySettings;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.packet.PacketGlow;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactEntity;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactManager;
import dev.lumas.events.games.logic.manor2.artifact.ArtifactPickupResult;
import dev.lumas.events.games.logic.manor2.attack.GhostAttacks;
import dev.lumas.events.games.logic.manor2.attack.OutsideBoundsEffect;
import dev.lumas.events.games.logic.manor2.light.Flashlight;
import dev.lumas.events.games.logic.manor2.roles.Artifact;
import dev.lumas.events.games.logic.manor2.roles.Ghost;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.games.logic.manor2.roles.RoleDialogues;
import dev.lumas.events.games.logic.manor2.roles.RoleHitbox;
import dev.lumas.events.games.logic.manor2.roles.Spectator;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.tokenformula.Manor2TokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import dev.lumas.events.utility.latency.LatencyHandler;
import dev.lumas.events.utility.latency.LatencyTracker;
import dev.lumas.events.utility.latency.hitbox.TrailRecorder;
import dev.lumas.events.utility.latency.tracing.Tracing;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import org.jspecify.annotations.Nullable;

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
    private static final Component USES_LIGHT_MESSAGE = Util.color("<gradient:white:yellow>This mini-game uses light mechanics");
    private static final long TICK_INTERVAL = 1L;
    private static final int TELEPORTS_PER_TICK = 5;
    private static final long END_DELAY_SECONDS = 10;
    private static final int MINIMUM_GHOSTS = 1;
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
    private Component gameOverSubtitle = Util.color("Game ended by force", Util.TEXT_COLOR);
    private final Manor2TokenFormula tokenFormula = new Manor2TokenFormula();
    private final OutsideBoundsEffect outsideBounds = new OutsideBoundsEffect(this);
    private final Dialog dialog = RoleDialogues.dialog();

    public Manor2(Manor2Definition definition) {
        super("Manor 2", Util.getRandom(SPLASH), definition.getDuration(), TICK_INTERVAL, true, true, false, false);
        this.definition = definition;
        this.artifactManager = new ArtifactManager(this);
        Manor2Definition.LagCompensationSettings lagCompensation = definition.getLagCompensation();
        this.latencyHandler = new LatencyHandler(latency, lagCompensation.isEnabled(), lagCompensation.getMaxRewindNano(), lagCompensation.getInterpolationMillis());
        this.tracing = new Tracing(this::involvedEntities, recorder, latencyHandler);
    }

    @Override
    protected InactivitySettings inactivitySettings() {
        return InactivitySettings.defaults(participant -> roleMap.get(participant.getUuid()), Ghost.class, GhostHunter.class);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        tokenFormula.giveTokens(participant, scoreboard.getScore(participant));
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
                    player.sendActionBar(USES_LIGHT_MESSAGE);
                    player.clearActivePotionEffects();
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
            gameOverSubtitle = Util.color("<gold>It's a tie!", Util.TEXT_COLOR);
            this.stop();
        }
        recorder.tick();
        ghostAttacks.tick();
        artifactManager.tick();
        outsideBounds.tick();
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
        if (gameOver) {
            gameOverSubtitle = Util.color("<gold><winner> has won!", Util.TEXT_COLOR, Placeholder.component("winner", winner.plural()));
            this.stop();
        }
    }

    @Override
    protected void handleStop() {
        if (timer != null) {
            unsafe(() -> timer.stop(false));
        }

        packetGlow.stop();
        ghostAttacks.stop();
        recorder.stopRecording();
        artifactManager.stop();

        Executors.sync(definition.getHunterStartLocations().getFirst(), blockCache::shutdown);

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(roleMap.retire(role));
        }
        audience.showTitle(Title.title(Util.color("<yellow>Game Over", Util.TEXT_COLOR), gameOverSubtitle));
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
            .thenRun(() -> {
                Executors.teleportGroupAsync(participants, definition.getLobby());
                scoreboard.showScores(this.audience);
                this.sendAudienceMessage("This minigame has concluded.");
            })
            .exceptionally(exception -> {
                exception.printStackTrace();
                return null;
            });

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        this.latency.start(); // No-op if already started
        player.teleportAsync(definition.getLobby());
        player.operatePlayer(bukkitPlayer -> Util.sendMsg(
            bukkitPlayer,
            "<manor2_info><gold>Click here for more info</gold></manor2_info> about Manor 2",
            Placeholder.styling("manor2_info", ClickEvent.callback(audience1 -> audience1.showDialog(dialog)))
        ));
        return super.handleParticipantJoin(player);
    }

    @Override
    public boolean removeParticipant(EventPlayer player, boolean doTeleport) {
        if (player.getPlayer() != null && this.timer != null) {
            this.timer.getBossBar().removeViewer(player.getPlayer());
        }
        Manor2Role removed = roleMap.remove(player.getUuid());
        if (removed != null) {
            Executors.runSync(player, () -> removed.retire(this));
        }
        boolean result = super.removeParticipant(player, doTeleport);
        if (removed instanceof Ghost) {
            replenishGhosts();
        }
        return result;
    }

    private void replenishGhosts() {
        if (!this.active || this.stopping) {
            return;
        }
        int missing = MINIMUM_GHOSTS - roleMap.getMatching(Ghost.class).size();
        for (int i = 0; i < missing; i++) {
            Manor2Role candidate = ghostCandidate();
            if (candidate == null) {
                return;
            }
            promoteToGhost(candidate);
        }
    }

    @Nullable
    private Manor2Role ghostCandidate() {
        List<Manor2Role> benched = new ArrayList<>();
        benched.addAll(roleMap.getMatching(Spectator.class));
        benched.addAll(roleMap.getMatching(Artifact.class));
        if (!benched.isEmpty()) {
            return Util.getRandom(benched);
        }
        List<GhostHunter> hunters = roleMap.getMatching(GhostHunter.class);
        if (hunters.size() <= 1) {
            return null;
        }
        return Util.getRandom(hunters);
    }

    private void promoteToGhost(Manor2Role role) {
        EventPlayer eventPlayer = role.getEventPlayer();
        scoreboard.registerTeam(role.getUuid(), ManorTeam.GHOSTS);
        roleMap.swapRoleSafely(role, () -> new Ghost(this, eventPlayer))
            .thenRun(() -> eventPlayer.teleportAsync(Util.getRandom(definition.getGhostStartLocations())))
            .exceptionally(exception -> {
                exception.printStackTrace();
                return null;
            });
        sendAudienceMessage("<yellow>" + eventPlayer.getName() + "</yellow> has risen as a ghost!");
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (event.getDamageSource().getDamageType() == DamageType.FREEZE) {
            event.setCancelled(true);
        }
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
        tryPickupArtifact(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler
    public void onPlayerAttackEntity(PrePlayerAttackEntityEvent event) {
        this.ensureNotIllegal();
        tryPickupArtifact(event.getPlayer(), event.getAttacked());
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        this.ensureNotIllegal();
        event.setCancelled(true);
        if (roleMap.get(event.getPlayer().getUniqueId()) instanceof Spectator spectator) {
            spectator.onUseItem(event.getItem());
        }
    }

    private void tryPickupArtifact(Player player, Entity entity) {
        if (roleMap.as(player.getUniqueId(), GhostHunter.class) instanceof GhostHunter ghostHunter) {
            ArtifactEntity.fromEntity(entity)
                .ifPresent(item -> {
                    ArtifactPickupResult result = artifactManager.pickup(entity.getLocation());
                    if (result != ArtifactPickupResult.NOT_FOUND) {
                        player.getInventory().addItem(item);
                        if (result == ArtifactPickupResult.FOUND_PLAYER_ARTIFACT) {
                            scoreboard.playerArtifactFound(ghostHunter);
                        } else {
                            scoreboard.artifactFound(ghostHunter);
                        }
                    } else {
                        entity.getLocation()
                            .getNearbyEntities(1, 1, 1)
                            .stream()
                            .filter(ArtifactEntity::isArtifact)
                            .forEach(Entity::remove);
                    }
                });
        }
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
