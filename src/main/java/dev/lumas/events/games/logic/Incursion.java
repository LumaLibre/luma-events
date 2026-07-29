package dev.lumas.events.games.logic;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.IncursionDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.IncursionTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Incursion extends InventoryUnifiedMinigame {

    private static final NamespacedKey FREEZE_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_freeze");
    private static final NamespacedKey KIT_KEY = new NamespacedKey(EventMain.getInstance(), "incursion_kit");

    private static final AttributeModifier FREEZE_MODIFIER =
            new AttributeModifier(FREEZE_KEY, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private static final List<Attribute> FREEZE_ATTRIBUTES = List.of(Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH);

    private static final Set<Integer> WARNING_SECONDS = Set.of(60, 30, 10, 3, 2, 1);

    private final IncursionDefinition definition;
    private final IncursionTokenFormula tokenFormula;
    private final MapSide side1;
    private final MapSide side2;

    private final Scoreboard<IncursionTeam> scoreboard = new Scoreboard<>();
    private final ConcurrentHashMap<UUID, Integer> points = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> frozenAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> invincibleUntil = new ConcurrentHashMap<>();
    private final Set<UUID> respawning = ConcurrentHashMap.newKeySet();
    private final Set<Integer> announcedSwapWarnings = ConcurrentHashMap.newKeySet();
    private final Set<Integer> announcedEndWarnings = ConcurrentHashMap.newKeySet();

    private volatile IncursionTeam team1;
    private volatile IncursionTeam team2;
    private volatile boolean secondHalf = false;
    private volatile boolean kickoffInProgress = false;
    private CountdownBossBar countdownBossBar;

    public Incursion(IncursionDefinition definition) {
        super(
                "Incursion",
                "Dive into the enemy team's hole to score!",
                Util.secsToMillis(definition.getGameLengthSeconds()),
                10,
                true,
                true,
                true,
                true
        );
        this.definition = definition;
        this.boundingBox = definition.getBounds().toBlockBoundingBox();
        this.side1 = new MapSide(definition.getTeam1().getSpawnArea(), definition.getTeam1().getHole().toBlockBoundingBox());
        this.side2 = new MapSide(definition.getTeam2().getSpawnArea(), definition.getTeam2().getHole().toBlockBoundingBox());
        this.tokenFormula = new IncursionTokenFormula(definition.getMinimumTokens(), definition.getPointsPerToken());
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        player.teleportAsync(definition.getLobbyLocation());
        return true;
    }

    @Override
    protected void handleStart() {
        this.team1 = new IncursionTeam(definition.getTeam1(), side1);
        this.team2 = new IncursionTeam(definition.getTeam2(), side2);

        List<EventPlayer> shuffled = new ArrayList<>(this.participants);
        Collections.shuffle(shuffled, RANDOM);
        for (int i = 0; i < shuffled.size(); i++) {
            (i % 2 == 0 ? team1 : team2).getMembers().add(shuffled.get(i).getUuid());
        }

        for (EventPlayer participant : shuffled) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue; // Shouldn't happen
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                player.setFoodLevel(20);
                player.setSaturation(20f);
                equipKit(player, team);
            });
        }

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        this.countdownBossBar.start();

        this.kickoff("<gold><b>First Half");
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (team1 == null || team2 == null) return;

        if (!secondHalf) {
            long untilSwap = (this.getDuration() / 2) - (this.getDuration() - timeLeft);
            if (untilSwap <= 0) halfTime();
            else warn(untilSwap, announcedSwapWarnings, "Sides swap in <yellow>%s<reset>!");
        } else {
            warn(timeLeft, announcedEndWarnings, "The game ends in <yellow>%s<reset>!");
        }

        broadcastScoreActionBar();
    }

    @Override
    protected void handleStop() {
        this.kickoffInProgress = false;
        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            clearPlayerState(participant);
        }

        if (team1 != null && team2 != null) {
            scoreboard.addScore(team1, team1.getScore().get());
            scoreboard.addScore(team2, team2.getScore().get());
        }

        this.scoreboard.handleGameEnd(this.audience, () -> {
            Executors.teleportGroupAsync(this.participants, definition.getLobbyLocation());
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
                    .seconds(10)
                    .callback(() -> this.sendAudienceMessage("This minigame has concluded."))
                    .build()
                    .start();
        });
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = points.getOrDefault(participant.getUuid(), 0);
        tokenFormula.giveTokens(participant, score);
        participant.addPermanentScore(MinigameConstant.INCURSION, score);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        UUID uuid = participant.getUuid();
        if (team1 != null) team1.getMembers().remove(uuid);
        if (team2 != null) team2.getMembers().remove(uuid);
        clearPlayerState(participant);

        Player player = participant.getPlayer();
        if (player != null && this.countdownBossBar != null) {
            this.countdownBossBar.getBossBar().removeViewer(player);
        }

        return super.removeParticipant(participant, doTeleport);
    }

    private synchronized void halfTime() {
        if (secondHalf || !this.active || this.stopping) return;
        this.secondHalf = true;

        MapSide previous = team1.getSide();
        team1.setSide(team2.getSide());
        team2.setSide(previous);

        this.sendAudienceMessage("<gold><b>Half time!</b> <reset>Both teams have swapped sides.");
        this.playAudienceSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.4f);
        this.kickoff("<gold><b>Second Half");
    }

    // Sends everyone to their spawn area, locks them in place and starts a start-countdown
    private void kickoff(String title) {
        this.kickoffInProgress = true;
        int cooldownTicks = Math.max(0, definition.getStartCooldownTicks());

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue; // Shouldn't happen
            sendToSpawn(participant, team, cooldownTicks);
        }

        if (cooldownTicks == 0) {
            releaseKickoff();
            return;
        }

        long endsAt = System.currentTimeMillis() + (cooldownTicks * 50L);
        AtomicInteger lastShown = new AtomicInteger(-1);
        Executors.runRepeatingAsync(TimeUnit.MILLISECONDS, 0, 200, task -> {
            if (!this.active || this.stopping) {
                task.cancel();
                return;
            }

            long remaining = endsAt - System.currentTimeMillis();
            if (remaining <= 0) {
                task.cancel();
                releaseKickoff();
                return;
            }

            int seconds = (int) Math.ceil(remaining / 1000.0);
            if (lastShown.getAndSet(seconds) == seconds) return;
            this.audience.showTitle(Title.title(
                    Util.color(title),
                    Util.color("<yellow>Get ready<gray>...</gray> <b>" + seconds + "</b>"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
            ));
            this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
        });
    }

    private void releaseKickoff() {
        this.kickoffInProgress = false;

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            invincibleUntil.remove(participant.getUuid());
            participant.operatePlayer(this::unfreeze);
        }

        this.audience.showTitle(Title.title(
                Util.color("<green><b>GO!"),
                Util.color("<gray>Dive into the enemy hole to score!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(300))
        ));
        this.playAudienceSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
    }

    private void warn(long millisRemaining, Set<Integer> announced, String format) {
        int seconds = (int) Math.round(millisRemaining / 1000.0);
        if (!WARNING_SECONDS.contains(seconds) || !announced.add(seconds)) return;
        this.sendAudienceMessage(String.format(format, formatCountdown(seconds)));
        this.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, seconds <= 3 ? 1.8f : 1.0f);
    }

    private static String formatCountdown(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return seconds + (seconds == 1 ? " second" : " seconds");
    }

    private void broadcastScoreActionBar() {
        Component separator = Component.text(" | ", NamedTextColor.DARK_GRAY);

        for (EventPlayer participant : new ArrayList<>(this.participants)) {
            IncursionTeam team = teamOf(participant.getUuid());
            if (team == null) continue;
            IncursionTeam enemy = enemyOf(team);

            Component actionBar = Component.text(team.getName() + " " + team.getScore().get(), team.getColor())
                    .append(separator)
                    .append(Component.text(enemy.getName() + " " + enemy.getScore().get(), enemy.getColor()))
                    .append(separator)
                    .append(Component.text("You: " + points.getOrDefault(participant.getUuid(), 0), NamedTextColor.GRAY))
                    .decorate(TextDecoration.BOLD);

            participant.sendActionBar(actionBar);
        }
    }

    // Teleports a participant into their team's spawn area AND FREEZES THEM THERE
    private void sendToSpawn(EventPlayer participant, IncursionTeam team, int invincibilityTicks) {
        Player player = participant.getPlayer();
        if (player == null) return;

        UUID uuid = participant.getUuid();
        respawning.add(uuid);
        if (invincibilityTicks > 0) {
            invincibleUntil.put(uuid, System.currentTimeMillis() + (invincibilityTicks * 50L));
        }

        Location spawn = team.getSide().spawnArea().randomSpawn();
        player.teleportAsync(spawn).whenComplete((_, _) -> Executors.runSync(player, () -> {
            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0f);
            player.setFireTicks(0);
            player.setFoodLevel(20);
            player.setSaturation(20f);

            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);

            equipKit(player, team);
            freeze(player, spawn);
            respawning.remove(uuid);
        }));
    }

    private void respawnFlow(EventPlayer participant, IncursionTeam team) {
        if (respawning.contains(participant.getUuid())) return;

        int freezeTicks = Math.max(0, definition.getRespawnFreezeTicks());
        int invincibilityTicks = Math.max(freezeTicks, definition.getRespawnInvincibilityTicks());
        sendToSpawn(participant, team, invincibilityTicks);

        Player player = participant.getPlayer();
        if (player == null) return;
        if (freezeTicks == 0) {
            Executors.runSync(player, () -> unfreeze(player));
            return;
        }
        Executors.delayedSync(player, freezeTicks, () -> {
            if (!kickoffInProgress) unfreeze(player);
        });
    }

    private void scoreHole(EventPlayer participant, IncursionTeam team) {
        int awarded = definition.getHolePoints();
        addPoints(participant, team, awarded);

        String name = participant.getName() != null ? participant.getName() : "Someone";
        this.sendAudienceMessage(Component.text(name, team.getColor())
                .append(Component.text(" dove into the enemy hole! ", NamedTextColor.WHITE))
                .append(Component.text("(+" + awarded + ")", NamedTextColor.GRAY)));
        this.playAudienceSound(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        respawnFlow(participant, team);
    }

    private void addPoints(EventPlayer participant, IncursionTeam team, int amount) {
        points.merge(participant.getUuid(), amount, Integer::sum);
        team.getScore().addAndGet(amount);
    }

    private void freeze(Player player, Location at) {
        if (frozenAt.put(player.getUniqueId(), at.clone()) != null) return; // already frozen
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            if (instance.getModifiers().stream().noneMatch(modifier -> FREEZE_KEY.equals(modifier.getKey()))) {
                instance.addTransientModifier(FREEZE_MODIFIER);
            }
        }
    }

    private void unfreeze(Player player) {
        frozenAt.remove(player.getUniqueId());
        for (Attribute attribute : FREEZE_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            instance.getModifiers().stream()
                    .filter(modifier -> FREEZE_KEY.equals(modifier.getKey()))
                    .toList()
                    .forEach(instance::removeModifier);
        }
    }

    private void clearPlayerState(EventPlayer participant) {
        UUID uuid = participant.getUuid();
        invincibleUntil.remove(uuid);
        respawning.remove(uuid);
        frozenAt.remove(uuid);
        participant.operatePlayer(this::unfreeze);
    }

    private void equipKit(Player player, IncursionTeam team) {
        player.getInventory().setChestplate(teamChestplate(team));
        // TODO: Weapon(s)
    }

    private ItemStack teamChestplate(IncursionTeam team) {
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        chestplate.editMeta(LeatherArmorMeta.class, meta -> {
            meta.setColor(team.getArmorColor());
            meta.displayName(Component.text(team.getName(), team.getColor()).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            meta.getPersistentDataContainer().set(KIT_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
        });
        return chestplate;
    }

    private static boolean isTeamArmor(@Nullable ItemStack item) {
        return item != null && Util.hasPersistentKey(item, KIT_KEY);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        IncursionTeam team = teamOf(uuid);
        if (team == null) return;

        Location to = event.getTo();
        Location frozen = frozenAt.get(uuid);
        if (frozen != null) {
            if (event instanceof PlayerTeleportEvent) return;
            if (to.getX() != event.getFrom().getX() || to.getZ() != event.getFrom().getZ()) {
                Location back = frozen.clone();
                back.setY(to.getY()); // don't fight gravity
                back.setYaw(to.getYaw());
                back.setPitch(to.getPitch());
                event.setTo(back);
            }
            return;
        }

        if (respawning.contains(uuid) || kickoffInProgress) return;
        if (event.getFrom().getBlockX() == to.getBlockX()
                && event.getFrom().getBlockY() == to.getBlockY()
                && event.getFrom().getBlockZ() == to.getBlockZ()) {
            return; // Optimization
        }

        EventPlayer participant = participantOf(uuid);
        if (participant == null) return;

        if (enemyOf(team).getSide().hole().contains(to)) {
            scoreHole(participant, team);
        } else if (team.getSide().hole().contains(to)) {
            participant.sendMessage("<red>That's your own hole! No points for you.");
            respawnFlow(participant, team);
        } else if (!this.boundingBox.contains(to)) {
            participant.sendMessage("<red>Don't leave the arena!");
            respawnFlow(participant, team);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!this.active || this.stopping) return;

        Player victim = event.getEntity();
        IncursionTeam victimTeam = teamOf(victim.getUniqueId());
        if (victimTeam == null) return;

        event.setCancelled(true);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.deathMessage(null);
        unsafe(() -> event.setDeathSound(Sound.ITEM_TOTEM_USE));

        AttributeInstance maxHealth = victim.getAttribute(Attribute.MAX_HEALTH);
        event.setReviveHealth(maxHealth != null ? maxHealth.getValue() : 20.0);

        Player killer = resolveKiller(event.getDamageSource().getCausingEntity(), victim);
        if (killer != null) {
            IncursionTeam killerTeam = teamOf(killer.getUniqueId());
            EventPlayer killerParticipant = participantOf(killer.getUniqueId());
            if (killerTeam != null && killerParticipant != null && killerTeam != victimTeam) {
                addPoints(killerParticipant, killerTeam, definition.getKillPoints());
                killerParticipant.sendActionBar("<green>+" + definition.getKillPoints() + " <gray>for eliminating " + victim.getName());
            }
        }

        EventPlayer participant = participantOf(victim.getUniqueId());
        if (participant != null) respawnFlow(participant, victimTeam);
    }

    // Might not be needed, but better safe than sorry
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        IncursionTeam team = teamOf(player.getUniqueId());
        EventPlayer participant = participantOf(player.getUniqueId());
        if (team == null || participant == null) return;

        event.setRespawnLocation(team.getSide().spawnArea().randomSpawn());
        Executors.delayedSync(player, 1, () -> respawnFlow(participant, team));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!this.active || this.stopping) return;

        if (!(event.getEntity() instanceof Player victim)) return;
        IncursionTeam victimTeam = teamOf(victim.getUniqueId());
        if (victimTeam == null) return;

        if (isOutOfPlay(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Player attacker = resolveKiller(damagerOf(event), victim);
        if (attacker == null) return;

        IncursionTeam attackerTeam = teamOf(attacker.getUniqueId());
        if (attackerTeam == null) return;

        if (attackerTeam == victimTeam || isOutOfPlay(attacker.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (!this.active || this.stopping) return;

        Player player = event.getPlayer();
        IncursionTeam team = teamOf(player.getUniqueId());
        if (team == null || isTeamArmor(event.getNewItem())) return;

        Executors.delayedSync(player, 1, () -> {
            player.getInventory().setChestplate(teamChestplate(team));
            Util.sendMsg(player, "You can't take off your armor!");
        });
    }

    private boolean isOutOfPlay(UUID uuid) {
        if (kickoffInProgress || frozenAt.containsKey(uuid) || respawning.contains(uuid)) return true;
        Long until = invincibleUntil.get(uuid);
        if (until == null) return false;

        if (System.currentTimeMillis() >= until) {
            invincibleUntil.remove(uuid);
            return false;
        }
        return true;
    }

    @Nullable
    private static Entity damagerOf(EntityDamageEvent event) {
        Entity causing = event.getDamageSource().getCausingEntity();
        if (causing != null) return causing;
        return event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
    }

    @Nullable
    private static Player resolveKiller(@Nullable Entity causingEntity, Player victim) {
        if (!(causingEntity instanceof Player killer) || killer.getUniqueId().equals(victim.getUniqueId())) return null;
        return killer;
    }

    @Nullable
    private IncursionTeam teamOf(UUID uuid) {
        if (team1 != null && team1.getMembers().contains(uuid)) return team1;
        if (team2 != null && team2.getMembers().contains(uuid)) return team2;
        return null;
    }

    private IncursionTeam enemyOf(IncursionTeam team) {
        return team == team1 ? team2 : team1;
    }

    @Nullable
    private EventPlayer participantOf(UUID uuid) {
        for (EventPlayer participant : this.participants) {
            if (participant.getUuid().equals(uuid)) return participant;
        }
        return null;
    }

    public record MapSide(IncursionDefinition.SpawnArea spawnArea, WorldTiedBoundingBox hole) {}

    @Getter
    public static final class IncursionTeam implements Scorer {

        private final String teamName;
        private final NamedTextColor color;
        private final Color armorColor;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final AtomicInteger score = new AtomicInteger();

        @Setter
        private volatile MapSide side;

        private IncursionTeam(IncursionDefinition.TeamDefinition definition, MapSide side) {
            this.teamName = definition.getName();
            this.color = definition.getNamedTextColor();
            this.armorColor = definition.getArmorColor();
            this.side = side;
        }

        @Override
        public String getName() {
            return teamName;
        }
    }
}
