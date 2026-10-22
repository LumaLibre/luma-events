package dev.lumas.events.games.logic.manor2;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.game.Manor2Definition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.HowToPlayDialogue;
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
import dev.lumas.events.games.logic.manor2.roles.RoleHitbox;
import dev.lumas.events.games.logic.manor2.roles.Spectator;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.tokenformula.Manor2TokenFormula;
import dev.lumas.events.games.tokenformula.ScoreRangeTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.SoftSleepyAPI;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.clientside.LocalizedFakeBlockCache;
import dev.lumas.events.utility.latency.LatencyHandler;
import dev.lumas.events.utility.latency.LatencyTracker;
import dev.lumas.events.utility.latency.hitbox.TrailRecorder;
import dev.lumas.events.utility.latency.tracing.Tracing;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
import org.bukkit.inventory.ItemType;
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
    private static final String GHOST_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTAzYzY0NzlmZjU2OWQ1OWNiYTliZDJjOWU1MzlkOWU4MDRjMjkzYWQwMTFhMDdjZDFkYzY4ZWZiMDdjOWU2YSJ9fX0=";
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
    private final PacketGlow packetGlow = PacketGlow.newInstance(this::canSeeOutline);
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
    private volatile @Nullable ManorTeam winner;
    private final ScoreRangeTokenFormula tokenFormula = Manor2TokenFormula.formula(this);
    private final OutsideBoundsEffect outsideBounds = new OutsideBoundsEffect(this);

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
        return InactivitySettings.withRoleTimeout(new InactivitySettings.Timeout(30, 45),
            participant -> roleMap.get(participant.getUuid()), Ghost.class, GhostHunter.class);
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        boolean won = winner != null && winner == scoreboard.teamOf(participant.getUuid());
        int score = scoreboard.getScore(participant);
        tokenFormula.giveTokens(participant, Couple.of(score, won));
        participant.addPermanentScore(MinigameConstant.MANOR_2, score);
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
            SoftSleepyAPI.wakeup(player);
            Executors.delayedSync(player, Math.floorDiv(i, TELEPORTS_PER_TICK) + 1, () -> {
                    if (roleMap.get(role.getUuid()) != role || !isParticipant(player)) {
                        return;
                    }
                    Executors.teleportSafely(player, target)
                        .thenRunAsync(() -> {
                            if (roleMap.get(role.getUuid()) != role || !isParticipant(player)) {
                                return;
                            }
                            role.initiatePlay();
                            player.sendActionBar(USES_LIGHT_MESSAGE);
                            player.clearActivePotionEffects();
                        }, Executors.executor(player));
                }
            );
        }
        showParticipantsToEachOther();
        packetGlow.start();
        Executors.repeatingSync(definition.getHunterStartLocations().getFirst(), 1L, task -> {
            if (stopping) {
                task.cancel();
                blockCache.shutdown();
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
        packetGlow.tick();
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
        if (gameOver && !this.stopping) {
            this.winner = winner;
            gameOverSubtitle = Util.color("<gold><winner> has won!", Util.TEXT_COLOR, Placeholder.component("winner", winner.plural()));
            this.stop();
        }
    }

    private boolean canSeeOutline(Player viewer, Entity target) {
        if (!(roleMap.get(viewer.getUniqueId()) instanceof Ghost)) {
            return true;
        }
        Location viewerLocation = viewer.getLocation();
        Location targetLocation = target.getLocation();
        double distance = definition.getGhostOutlineDistance();
        return viewerLocation.getWorld() == targetLocation.getWorld() && viewerLocation.distanceSquared(targetLocation) <= distance * distance;
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

        List<EventPlayer> endedParticipants = List.copyOf(participants);
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (Manor2Role role : roleMap) {
            completableFutures.add(roleMap.retire(role));
        }
        audience.showTitle(Title.title(Util.color("<yellow>Game Over", Util.TEXT_COLOR), gameOverSubtitle));
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
            .thenRun(() -> {
                Executors.teleportGroupAsync(endedParticipants, definition.getLobby());
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
        return super.handleParticipantJoin(player);
    }

    @Override
    protected HowToPlayDialogue howToPlay() {
        return HowToPlayDialogue.builder()
            .title("<b><gradient:#331D4D:#611e4e:#9f2e51>Manor 2")
            .sections(List.of(
                new HowToPlayDialogue.ItemSection(ItemType.TORCH.createItemStack(), "<gray><b>Light Mechanics</b></gray>", """
                    This game uses light mechanics. It's recommended
                    that you turn <b><u>off</u></b> any type of <b>full bright</b>
                    or <b>gamma increasement mods</b>.
                    Ghosts are always <b>vanished</b> when they are not revealed with a flashlight,
                    you are <u>worsening</u> your own experience of this game by leaving these mods
                    enabled because you can't see what area your own flashlight is covering!
                    """),
                new HowToPlayDialogue.HeadSection(GHOST_HEAD_BASE64, "<gray><b>Ghost</b></gray>", """
                    The ghosts kill hunters, this is done by going into range and <b>attacking</b> (<b>left-clicking a hunter</b>).
                    Ghosts are shy, but can be revealed when <b>they get light on them</b>. When revealed, they are <b>incapable
                    of killing hunters</b>. For every 10 players, there is 1 ghost.
                    """),
                new HowToPlayDialogue.ItemSection(ItemType.BOW.createItemStack(), "<gray><b>Hunters</b></gray>", """
                    The hunters collect artifact and kills ghosts. The main issue is that both of these are invisible.
                    But luckily they brought a flashlight! With this flashlight they can (<b>right-click to use</b>):<br>
                    <gray>-</gray> Reveal artifacts for points<br>
                    <gray>-</gray> Reveal and even kill ghosts<br>
                    <gray>-</gray> Prevent ongoing and new ghost attacks<br><br>
                    To know if a <b>ghost is near</b>, a hunter gets <b>cold</b> and <b>lights starts to flicker</b>.
                    """)
            ))
            .build();
    }

    @Override
    public boolean removeParticipant(EventPlayer player, boolean doTeleport, boolean wasAfk) {
        if (player.getPlayer() != null && this.timer != null) {
            this.timer.getBossBar().removeViewer(player.getPlayer());
        }
        Manor2Role removed = roleMap.remove(player.getUuid());
        if (removed != null) {
            roleMap.retire(removed);
        }
        boolean result = super.removeParticipant(player, doTeleport, wasAfk);
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
        eventPlayer.operatePlayer(SoftSleepyAPI::wakeup);
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
    public void onHunterAttackHunter(PrePlayerAttackEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getAttacked() instanceof Player attacked)) return;
        if (roleMap.is(event.getPlayer().getUniqueId(), GhostHunter.class) && roleMap.is(attacked.getUniqueId(), GhostHunter.class)) {
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
        if (roleMap.get(event.getPlayer().getUniqueId()) instanceof Spectator spectator) {
            spectator.onConsumeItem(event.getItem());
            event.setCancelled(true);
        } else if (roleMap.get(event.getPlayer().getUniqueId()) instanceof Ghost ghost) {
            event.setCancelled(!ghost.onConsumeItem(event.getItem()));
        } else {
            event.setCancelled(true);
        }
    }

    private void tryPickupArtifact(Player player, Entity entity) {
        if (roleMap.as(player.getUniqueId(), GhostHunter.class) instanceof GhostHunter ghostHunter) {
            ArtifactEntity.fromEntity(entity)
                .ifPresent(item -> {
                    ArtifactPickupResult result = artifactManager.pickup(entity.getLocation());
                    switch (result) {
                        case ArtifactPickupResult.FoundNormalArtifact ignored -> scoreboard.artifactFound(ghostHunter);
                        case ArtifactPickupResult.FoundPlayerArtifact(Artifact artifact) ->
                            scoreboard.playerArtifactFound(ghostHunter, artifact);
                        default -> entity.getLocation()
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
