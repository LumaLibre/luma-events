package dev.lumas.events.games.interfaces;


import dev.lumas.events.EventMain;
import dev.lumas.events.games.events.MinigameExitPreventionListener;
import dev.lumas.events.games.events.MinigamePreventDamageListener;
import dev.lumas.events.games.events.MinigamePreventInventoryTampering;
import dev.lumas.events.games.events.SleepyEventListener;
import dev.lumas.events.games.exceptions.GameComponentIllegallyActive;
import dev.lumas.events.games.interfaces.models.InactivitySettings;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.MinigameBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Externals;
import dev.lumas.events.utility.JoinTrace;
import dev.lumas.events.utility.SoftSleepyAPI;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.effects.SoundDefinition;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.Setter;
import me.kteq.hiddenarmor.HiddenArmorAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public abstract class Minigame extends AsynchronousRunnable implements Listener {

    public static final String MINIGAME_PARTICIPANT_PERM = "lumaevents.default";
    private static final boolean HIDDEN_ARMOR_AVAILABLE = Externals.pluginExists("HiddenArmor");

    protected static final Random RANDOM = Util.RANDOM;


    // Iterated from async and region threads while joins/leaves mutate it:
    protected final List<EventPlayer> participants = new CopyOnWriteArrayList<>();
    private final Map<UUID, CompletableFuture<Boolean>> joinTeleports = new ConcurrentHashMap<>();
    protected final List<Listener> extraListeners = new ArrayList<>();
    private final InactivitySettings cachedInactivitySettings = this.inactivitySettings();
    private final Map<UUID, AfkWarning> afkWarnings = new ConcurrentHashMap<>();
    private final MinigamePreventInventoryTampering inventoryTampering;
    private final MinigamePreventDamageListener preventDamage;

    private final String name;
    private final String description;
    private long duration;
    private final long tickInterval;
    private final boolean async;


    protected long startTime = -1;
    private volatile TokenPayout tokenPayout = TokenPayout.NORMAL;
    protected volatile boolean open = false;
    protected volatile boolean active = false;
    protected volatile boolean stopping = false;
    protected Audience audience;
    protected MinigameBoundingBox boundingBox;
    protected CountdownBossBar queueBossbar;

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        this.extraListeners.add(new MinigameExitPreventionListener(this));
        if (SoftSleepyAPI.available()) {
            this.extraListeners.add(new SleepyEventListener(this));
        }
        this.inventoryTampering = new MinigamePreventInventoryTampering(this);
        this.preventDamage = new MinigamePreventDamageListener(this);
        registerEvents(this.inventoryTampering);
        registerEvents(this.preventDamage);
    }

    protected Minigame(String name, String description, long duration, long tickInterval, boolean async, boolean preventExit, boolean preventInventoryTampering, boolean preventDamage) {
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.tickInterval = tickInterval;
        this.async = async;
        if (SoftSleepyAPI.available()) {
            this.extraListeners.add(new SleepyEventListener(this));
        }
        if (preventExit) this.extraListeners.add(new MinigameExitPreventionListener(this));
        this.inventoryTampering = preventInventoryTampering ? new MinigamePreventInventoryTampering(this) : null;
        this.preventDamage = preventDamage ? new MinigamePreventDamageListener(this) : null;


        if (this.inventoryTampering != null) registerEvents(this.inventoryTampering);
        if (this.preventDamage != null) registerEvents(this.preventDamage);
    }


    public boolean timedStart() {
        return this.timedStart(90);
    }

    public boolean timedStart(int seconds) {
        if (this.active) {
            return false;
        }
        this.active = true;
        this.open = true;
        this.openQueue(seconds);
        return true;
    }

    public boolean start() {
        this.open = false;

        if (this.participants.size() < this.minimumParticipants()) {
            // Nothing has happened at this point other than these values
            // being changed to true, so we can just set them to false and return
            this.active = false;
            if (this.inventoryTampering != null) {
                unregisterEvents(this.inventoryTampering);
            }
            if (this.preventDamage != null) {
                unregisterEvents(this.preventDamage);
            }
            // TODO: remove this:
            extraListeners.stream()
                .filter(Objects::nonNull)
                .forEach(this::unregisterEvents);
            Util.broadcast("Not enough players joined to start " + this.name, MINIGAME_PARTICIPANT_PERM);
            return false;
        }
        this.onPreStart();

        for (EventPlayer participant : this.participants) {
            participant.minigameDirty(true);
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }

                if (HIDDEN_ARMOR_AVAILABLE) {
                    HiddenArmorAPI.forceShow(player);
                }
            });
        }

        registerEvents(this);
        this.audience = Audience.audience(participants.stream()
            .map(EventPlayer::getPlayer).filter(Objects::nonNull).toList());
        this.startTime = System.currentTimeMillis();
        extraListeners.stream()
            .filter(Objects::nonNull)
            .forEach(this::registerEvents);

        if (EventMain.getConfiguration().getMinigameStart() instanceof SoundDefinition definition) {
            definition.playFor(audience);
        }
        unsafe(this::handleStart);
        if (async) {
            this.repeatingAsync(0, this.tickInterval);
        } else {
            throw new UnsupportedOperationException("Minigame must be async");
        }
        return true;
    }

    public synchronized boolean stop() {
        if (!this.active || this.stopping) {
            return false;
        }
        this.cancel(); // halt tasks first
        unregisterEvents(this); // choosing to unregister listeners before doing shutdown logic
        extraListeners.stream()
            .filter(Objects::nonNull)
            .forEach(this::unregisterEvents);

        if (this.inventoryTampering != null) {
            unregisterEvents(this.inventoryTampering);
        }
        if (this.preventDamage != null) {
            unregisterEvents(this.preventDamage);
        }

        this.stopping = true;

        try {
            if (EventMain.getConfiguration().getMinigameStop() instanceof SoundDefinition definition) {
                definition.playFor(audience);
            }
            this.handleStop();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        try {
            this.onPostStop();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        for (EventPlayer participant : this.participants) {
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                if (HIDDEN_ARMOR_AVAILABLE) {
                    HiddenArmorAPI.clearForceShow(player);
                }
            });
            participant.minigameDirty(false);
        }

        this.active = false;
        this.open = false; // Should be false by now anyway :P
        this.afkWarnings.keySet().forEach(this::clearAfkWarning);
        this.participants.clear();
        return true;
    }

    public final boolean addParticipant(EventPlayer player) {
        if (!this.active || !this.open) {
            return false;
        }


        try {
            if (!this.handleParticipantJoin(player)) {
                player.sendMessage("This minigame has denied your entry.");
                return false;
            }
            Player bukkitPlayer = player.getPlayer();
            if (bukkitPlayer != null) {
                SoftSleepyAPI.wakeup(bukkitPlayer);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            player.sendMessage("An error occurred while trying to join the minigame.");
            return false;
        }


        if (!this.participants.contains(player)) {
            this.participants.add(player);
        }

        // The rollback must not be able to run before the player is a participant
        CompletableFuture<Boolean> teleport = this.joinTeleports.remove(player.getUuid());
        if (teleport != null) {
            teleport.whenComplete((success, throwable) -> {
                if (Boolean.TRUE.equals(success) && throwable == null) {
                    JoinTrace.joinComplete(player.getUuid());
                    return;
                }
                EventMain.getInstance().getLogger().warning("Join teleport failed for " + player.getUuid()
                    + " (success=" + success + ", ex=" + throwable + "), removing them from " + this.name);
                this.removeParticipant(player, false);
                player.sendMessage("<red>Could not move you into the minigame, so you were removed from it.");
            });
        }

        player.sendTitle( // TODO: coloring
            "<yellow>" + this.name,
            "<red>" + this.description
        );
        this.sendHowToPlay(player);
        return true;
    }

    private void sendHowToPlay(EventPlayer player) {
        HowToPlayDialogue howToPlay = this.howToPlay();
        if (howToPlay == null) {
            return;
        }
        final Dialog dialog = howToPlay.dialog();
        player.operatePlayer(bukkitPlayer -> Util.sendMsg(
            bukkitPlayer,
            "<how_to_play><yellow>Click here for more info</yellow></how_to_play> about " + this.name,
            Placeholder.styling("how_to_play", ClickEvent.callback(audience -> audience.showDialog(dialog)))
        ));
    }

    public final boolean removeParticipant(EventPlayer player, boolean doTeleport) {
        return this.removeParticipant(player, doTeleport, false);
    }

    public boolean removeParticipant(EventPlayer player, boolean doTeleport, boolean wasAfk) {
        this.participants.remove(player);
        this.clearAfkWarning(player.getUuid());
        player.operatePlayer(bukkitPlayer -> {
            if (bukkitPlayer.getGameMode() != GameMode.SURVIVAL) {
                bukkitPlayer.setGameMode(GameMode.SURVIVAL);
            }
            if (HIDDEN_ARMOR_AVAILABLE) {
                player.operatePlayer(HiddenArmorAPI::clearForceShow);
            }
        });
        player.minigameDirty(false);
        Location loc = this.getGameDropOffLocation();
        if (loc != null && doTeleport) {
            player.operatePlayer(bukkitPlayer -> {
                Executors.teleportSafely(bukkitPlayer, loc);
                Util.sendMsg(bukkitPlayer, "You have been removed from the active minigame!");
            });
        }
        this.audience = Audience.audience(participants.stream()
            .map(EventPlayer::getPlayer).filter(Objects::nonNull).toList());
        return true;
    }

    private void openQueue(int seconds) {
        Util.broadcast("<hover:show_text:'Click me!'><click:run_command:/event join>A minigame is starting! Use <gold>/event join</gold> to participate!", MINIGAME_PARTICIPANT_PERM);
        if (!tokenPayout.isNormal()) Util.broadcast(describePayout(tokenPayout), MINIGAME_PARTICIPANT_PERM);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> player.hasPermission(MINIGAME_PARTICIPANT_PERM))
            .toList();

        this.queueBossbar = CountdownBossBar.builder()
            .title("<gradient:#3F255E:#9f2e51:#611e4e:#331D4D><b>" + name + " Starting in</b><gray>:</gray> <b>%ss</b>")
            .seconds(seconds)
            .color(BossBar.Color.RED)
            .callback(this::start)
            .audience(Audience.audience(players))
            .build()
            .start();
        // newMinigame runs on the async scheduler, so read each player's location on the region that owns them
        players.forEach(player -> Executors.runSync(player,
            () -> player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1f, 0.75f)));
    }

    @Override
    public void accept(ScheduledTask task) {
        if (this.stopping || !this.active) return;
        long timeLeft = this.duration - (System.currentTimeMillis() - this.startTime);
        if (timeLeft <= 0) {
            this.stop();
            return;
        }
        try {
            this.onRunnable(timeLeft);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        unsafe(this::checkInactivity);
    }

    private void checkInactivity() {
        InactivitySettings settings = this.cachedInactivitySettings;
        long now = System.currentTimeMillis();
        for (EventPlayer participant : this.participants) {
            UUID uuid = participant.getUuid();
            if (settings.ignoreFloodgate() && participant.isFloodgate()) {
                this.clearAfkWarning(uuid);
                continue;
            }

            InactivitySettings.Timeout timeout = settings.timeoutFor(participant);
            long idleMillis = this.idleMillis(uuid, now);
            long kickMillis = TimeUnit.SECONDS.toMillis(timeout.secondsBeforeKick());
            if (idleMillis >= kickMillis) {
                this.removeInactiveParticipant(participant);
            } else if (idleMillis >= TimeUnit.SECONDS.toMillis(timeout.secondsBeforeWarning())) {
                this.warnInactiveParticipant(participant, timeout, kickMillis - idleMillis);
            } else {
                this.clearAfkWarning(uuid);
            }
        }
    }

    private long idleMillis(UUID uuid, long now) {
        long sleepyIdleMillis = TimeUnit.SECONDS.toMillis(SoftSleepyAPI.afkSeconds(uuid));
        return Math.min(now - this.startTime, sleepyIdleMillis);
    }

    private void warnInactiveParticipant(EventPlayer participant, InactivitySettings.Timeout timeout, long remainingMillis) {
        Player player = participant.getPlayer();
        if (player == null) {
            return;
        }
        long remainingSeconds = Math.max(1, (remainingMillis + 999) / 1000);
        float progress = Math.clamp(remainingMillis / (float) TimeUnit.SECONDS.toMillis(timeout.warningWindowSeconds()), 0f, 1f);

        AfkWarning warning = this.afkWarnings.get(participant.getUuid());
        if (warning == null) {
            BossBar bossBar = BossBar.bossBar(afkBossBarName(remainingSeconds), progress, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            warning = new AfkWarning(bossBar, player, remainingSeconds);
            this.afkWarnings.put(participant.getUuid(), warning);
            Executors.runSync(player, () -> player.showBossBar(bossBar));
            participant.sendTitle("<red>Are you still there?", "<yellow>Move within " + remainingSeconds + "s or you will be removed");
            participant.sendMessage("<red>You are AFK and will be removed from " + this.name + " in " + remainingSeconds + " seconds. Move to stay in the game!");
            participant.operatePlayer(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f));
            return;
        }

        warning.bossBar().progress(progress);
        if (warning.shownSeconds() == remainingSeconds) {
            return;
        }
        warning.shownSeconds(remainingSeconds);
        warning.bossBar().name(afkBossBarName(remainingSeconds));
        if (remainingSeconds <= 10) {
            float pitch = 0.5f + (10 - remainingSeconds) * 0.15f;
            participant.operatePlayer(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, pitch));
        }
        if (remainingSeconds == 10) {
            participant.sendTitle("<red>Are you still there?", "<yellow>Move within 10s or you will be removed");
            participant.operatePlayer(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f));
        }
    }

    private static Component afkBossBarName(long remainingSeconds) {
        return Util.color("<red><b>You are AFK!</b></red> <gray>Move within <white><b>" + remainingSeconds + "s</b></white> or you will be removed");
    }

    private void clearAfkWarning(UUID uuid) {
        AfkWarning warning = this.afkWarnings.remove(uuid);
        if (warning != null) {
            Player player = warning.player();
            Executors.runSync(player, () -> player.hideBossBar(warning.bossBar()));
        }
    }

    public void removeInactiveParticipant(EventPlayer participant) {
        if (!this.removeParticipant(participant, true, true)) {
            return;
        }
        participant.sendMessage("<red>You were removed from " + this.name + " for being AFK.");
        this.sendAudienceMessage("<yellow>" + participant.getName() + "</yellow> was removed for inactivity.");
    }

    public void ensureNotIllegal() {
        if (!this.isActive()) {
            throw new GameComponentIllegallyActive("Minigame is not active");
        }
    }

    public void ensureTickThread(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            throw new IllegalStateException("Bad thread for " + entity);
        }
    }

    protected boolean addExtraListener(Listener listener) {
        if (listener == null) {
            return false;
        }
        this.extraListeners.add(listener);
        if (!this.open) {
            registerEvents(listener);
        }
        return true;
    }

    protected void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, EventMain.getInstance());
    }

    protected void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    @Nullable
    public Location getGameDropOffLocation() {
        return EventMain.getConfiguration().getGameDropOffLocation();
    }

    public void sendAudienceMessage(String m) {
        if (this.audience == null) {
            return;
        }
        this.audience.sendMessage(Util.color(Util.PREFIX + m, Util.TEXT_COLOR));
    }

    public void sendAudienceMessage(Component m) {
        if (this.audience == null) {
            return;
        }
        this.audience.sendMessage(Util.color(Util.PREFIX).append(m).colorIfAbsent(Util.TEXT_COLOR));
    }

    public void sendAudienceTitle(String title, String subtitle, TagResolver... tagResolvers) {
        if (this.audience == null) {
            return;
        }
        this.audience.showTitle(Util.title(title, subtitle, Util.TEXT_COLOR, tagResolvers));
    }

    public void sendAudienceTitle(String title, String subtitle, Title.Times times) {
        if (this.audience == null) {
            return;
        }
        this.audience.showTitle(Util.title(title, subtitle, times, Util.TEXT_COLOR));
    }

    public void sendAudienceActionBar(String message, TagResolver... tagResolvers) {
        if (this.audience == null) {
            return;
        }
        this.audience.sendActionBar(Util.color(message, Util.TEXT_COLOR, tagResolvers));
    }

    public void playAudienceSound(Sound sound, float volume, float pitch) {
        if (this.audience == null) {
            return;
        }

        net.kyori.adventure.sound.Sound kyoriSound = net.kyori.adventure.sound.Sound.sound(Registry.SOUNDS.getKey(sound), net.kyori.adventure.sound.Sound.Source.NEUTRAL, volume, pitch);
        this.audience.playSound(kyoriSound);
    }

    protected boolean isParticipant(EventPlayer... players) {
        for (EventPlayer p : players) {
            if (!this.participants.contains(p)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isParticipant(Player player) {
        return this.participants.stream()
            .map(EventPlayer::getUuid)
            .filter(Objects::nonNull)
            .anyMatch(uuid -> uuid.equals(player.getUniqueId()));
    }

    protected boolean isInBoundingBox(EventPlayer... players) {
        for (EventPlayer p : players) {
            Player bukkitPlayer = p.getPlayer();
            if (bukkitPlayer == null) return false;
            if (!this.boundingBox.contains(bukkitPlayer.getLocation())) {
                return false;
            }
        }
        return true;
    }

    protected boolean isInBoundingBox(Player... players) {
        for (Player p : players) {
            if (!this.boundingBox.contains(p.getLocation())) {
                return false;
            }
        }
        return true;
    }

    protected void unsafe(Runnable block) {
        try {
            block.run();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    protected void onPreStart() {
        // This method can be overridden to perform actions before the minigame starts
        // For example, setting up the environment, clearing inventories, etc.
    }

    protected void onPostStop() {
        // This method can be overridden to perform actions after the minigame stops
        // For example, restoring player inventories, cleaning up resources, etc.
    }

    protected int minimumParticipants() {
        // This method can be overridden to specify the minimum number of participants required to start the minigame
        return 2; // Default is 2 participants
    }

    // Minigame starts, returns true if successful
    protected abstract void handleStart();

    protected abstract void onRunnable(long timeLeft);

    // Minigame stops, returns true if successful
    protected abstract void handleStop();

    protected InactivitySettings inactivitySettings() {
        return InactivitySettings.defaults();
    }

    protected CompletableFuture<Boolean> teleportOnJoin(EventPlayer player, @Nullable Location destination) {
        CompletableFuture<Boolean> future = player.teleportAsync(destination);
        this.joinTeleports.put(player.getUuid(), future);
        return future;
    }

    protected abstract boolean handleParticipantJoin(EventPlayer player);

    protected @Nullable HowToPlayDialogue howToPlay() {
        return null;
    }

    // TODO: cleanup/move
    private static String describePayout(TokenPayout tokenPayout) {
        if (tokenPayout.paysNothing()) return "This one is <yellow>just for fun</yellow> - it pays out no tokens.";
        if (tokenPayout.flat()) {
            String tokens = "<gold>" + tokenPayout.flatTokens() + "</gold> token" + (tokenPayout.flatTokens() == 1 ? "" : "s");
            return "This one is <yellow>casual</yellow> - everyone gets " + tokens + ", regardless of score.";
        }
        return "This one pays out <gold>" + tokenPayout.percentDifference() + "</gold> tokens than usual!";
    }

    private static final class AfkWarning {
        private final BossBar bossBar;
        private final Player player;
        private long shownSeconds;

        private AfkWarning(BossBar bossBar, Player player, long shownSeconds) {
            this.bossBar = bossBar;
            this.player = player;
            this.shownSeconds = shownSeconds;
        }

        BossBar bossBar() {
            return bossBar;
        }

        Player player() {
            return player;
        }

        long shownSeconds() {
            return shownSeconds;
        }

        void shownSeconds(long shownSeconds) {
            this.shownSeconds = shownSeconds;
        }
    }
}
