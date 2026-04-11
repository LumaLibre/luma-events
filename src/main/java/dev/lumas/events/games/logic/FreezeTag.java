package dev.lumas.events.games.logic;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.FreezeTagDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.tokenformula.FreezeTagTokenFormula;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.colormanagers.ColorManager;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Add scarlet/ivory team stuff
// TODO: Swap LumaGlowAPI references
public final class FreezeTag extends InventoryUnifiedMinigame {

    private static final List<String> EGG_TEXTURES = List.of(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDY0MmFmYTM5Njg1M2I4MWIxN2JlZjVjOGQ3YTQ0YzEyZGU2ODlhNTZhZjQ3NDg0NjY3OTgzOTlkYTNjZmVhZSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM5NjQyNWIyNjc3NDFkZGU0YTNhYzQ4OTBiN2Y5NWE4YjI5ODJkYTlmNGE5NWE2ZWE0ZDU1NjFkOTczNyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2YxYzI1YmY2MWEyYmJjN2E5OTU4ZTliOWRiYzlmZjdlMDg3N2M2MjJlNDdmYmNkNTUzNmU5YmUzZDAxMWIzMyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmMyMzNlOTI3ZWNmNjE4ZWE4YWY4NTI0ZjU4Mzk3NWFjOWM4NmQ1ZThkNzFlZTdmZTM4MzQzMTY0Y2I2In19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmRlMGE1MGIyYzA3NjVmMDY4NjhmNjBjYWNlYzNmMmNhYWNkY2RkODU1NGI0Y2FkYjlkNjQ2NDAyNzVkNzE5NiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdmMGRjOThkZTRiZDIzMWVlODM0OGQ3NzdkMjkxYjdhNGEyYWQ5ZGY0MWJiMzllZGNmODcwYTllM2ZhYzZlNCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTg0ZTc2ZGM4NzMzZjk2YTg0NjhkMzhmNzNhNWY3NzA4OTZkNWExMjljY2E4YzI5ZWZlOTkxOTdjYjY2NmFmMiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIzZmNiMTU2ZTQ1ZmViNGM3NzU5MjZiZWQ4MjMwZjhkNjUwYzdiZmJjMzQ2MGQwYmI2ZWExMDhiYjFjZWQ5ZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODBiOGVmNTJmYjdkNWZjMDE2ODJlNmYxYjAyNGUwY2MxYjc2YzJhZTQxYzk2YTdlNTRlNWZmNWE1MTU4NjlkYyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQxMmFmNjhlZGMzZWFkN2UzYWE5OWU5MjM4MTY2MzU5ZmVlNjE4OTA0ZWNlY2ZhYzNhYWJlNGIyNzc1M2VkMyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTBjZDc5NWViOTRlYmJlNDA1ZTZhNjAwYWVlOTVmZmZhZmMyYjhjNzQ0ODY1MGMwYmFhOTdmYzcwMjFjMzU0NCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNiZWQ4YTgzMzU4NTQ0MDMyYzMxNGIzODFlYmJiMWVjNGY0MGZiNTI3M2Y0NWUxNTZhZWM3YjJjMDdlZGZkZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWI5ZTZlMzRiNWI2MGZkNTNkNTdmZGE5OTgwZDdlOTk3YjUwNDY0Y2IxZjUxZGJjMzU3YTM3MmYzNjc2NTAzIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmQ1NWQ3Nzc0ZDFhN2Q2ZWQ0NTU2MDQxZTk2YmVhODIwYjI1ZGQ4ZjEzZmM1YTNlODI1NzYxZDBjMWZhNzZiYSJ9fX0="
    );

    private static final AttributeModifier FREEZE_SPEED_MOD = new AttributeModifier(
            new NamespacedKey(EventMain.getInstance(), "freeze_tag_speed"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    private static final AttributeModifier FREEZE_JUMP_MOD = new AttributeModifier(
            new NamespacedKey(EventMain.getInstance(), "freeze_tag_jump"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private final FreezeTagDefinition settings;
    private final FreezeTagTokenFormula tokenFormula;
    private CountdownBossBar countdownBossBar;
    private List<FreezeTagTeam> teams;

    private final ConcurrentHashMap<UUID, FrozenState> frozenPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingFreezeHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingUnfreezeHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> actionBarPauseUntil = new ConcurrentHashMap<>();

    public FreezeTag(FreezeTagDefinition settings) {
        super("Freeze Tag", "Freeze all enemy players to win!", Util.secsToMillis(settings.getTimeLimitSeconds()), 10, true, true, false);
        this.settings = settings;
        this.boundingBox = WorldTiedBoundingBox.of(settings.getRegion().getLoc1(), settings.getRegion().getLoc2());
        this.tokenFormula = new FreezeTagTokenFormula(settings.getMinimumTokens(), settings.getTokensPerPoint());
    }

    @Override
    protected void handleStart() {
        List<EventPlayer> shuffled = new ArrayList<>(this.participants);
        Collections.shuffle(shuffled);

        int middle = shuffled.size() / 2;
        this.teams = List.of(
                new FreezeTagTeam(shuffled.subList(0, middle), settings.getTeam1(), settings.getTeam1SpawnLocation()),
                new FreezeTagTeam(shuffled.subList(middle, shuffled.size()), settings.getTeam2(), settings.getTeam2SpawnLocation())
        );

        for (FreezeTagTeam team : teams) {
            for (EventPlayer member : team.getMembers()) {
                member.operatePlayer(player -> {
                    player.teleportAsync(team.getSpawnLocation().toCenterLocation());
                    ColorManager.setTempPlayerColor(player, Util.chatColorFromNamedTextColor(team.getColor()));
                    player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, team.getArmorColor()));
                    //player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, team.getArmorColor()));
                    //player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, team.getArmorColor()));
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                });
            }
        }

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<white><b>Time Remaining: %ss")
                .color(BossBar.Color.WHITE)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        this.countdownBossBar.start();
    }

    @Override
    protected void handleStop() {
        if (countdownBossBar != null) {
            countdownBossBar.stop(false);
        }

        FreezeTagTeam winner = determineWinner();
        String winnerName = winner != null ? winner.getName() : "Nobody (Tie)";

        new ArrayList<>(frozenPlayers.keySet()).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) unfreezePlayerCleanup(player);
        });

        this.audience.showTitle(Util.title("<yellow>Game Over!", "<gold>" + winnerName + " wins!"));

        Location dropOff = this.getGameDropOffLocation();
        CountdownBossBar.builder()
                .audience(this.audience)
                .color(BossBar.Color.BLUE)
                .title("<aqua><b>Game Over")
                .seconds(10)
                .callback(() -> this.participants.forEach(ep -> {
                    if (dropOff != null) ep.teleportAsync(dropOff);
                    ep.sendMessage("This minigame has concluded.");
                }))
                .build()
                .start();
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // Handled in onPostStop
    }

    @Override
    public void onPostStop() {
        super.onPostStop();
        handleTokens();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (teams == null) return;

        for (FreezeTagTeam team : teams) {
            List<EventPlayer> members = team.getMembers();
            if (members.isEmpty()) continue;
            boolean allFrozen = members.stream().allMatch(ep -> frozenPlayers.containsKey(ep.getUuid()));
            if (allFrozen) {
                this.stop();
                return;
            }
        }

        FreezeTagTeam team1 = teams.get(0);
        FreezeTagTeam team2 = teams.get(1);

        int team1Frozen = (int) team1.getMembers().stream().filter(ep -> frozenPlayers.containsKey(ep.getUuid())).count();
        int team2Frozen = (int) team2.getMembers().stream().filter(ep -> frozenPlayers.containsKey(ep.getUuid())).count();

        Component sep = Component.text(" | ").color(NamedTextColor.WHITE);

        for (FreezeTagTeam team : teams) {
            FreezeTagTeam enemy = teams.stream().filter(t -> t != team).findFirst().orElse(null);
            if (enemy == null) continue;

            int myFrozen = team == team1 ? team1Frozen : team2Frozen;
            int enemyFrozen = enemy == team1 ? team1Frozen : team2Frozen;

            for (EventPlayer member : team.getMembers()) {
                if (isStatusActionBarPaused(member)) {
                    continue;
                }

                int required = settings.getFreezeHitsRequired();
                int takenHits = pendingFreezeHits.getOrDefault(member.getUuid(), 0);
                int livesLeft = Math.max(0, required - takenHits);

                Component myTeamPart = Component.text(team.getName() + " ❄ " + myFrozen + "/" + team.getMembers().size())
                        .color(team.getColor());

                Component livesPart = Component.text(heartsDisplay(livesLeft, required))
                        .color(NamedTextColor.RED);

                Component enemyTeamPart = Component.text(enemy.getName() + " ❄ " + enemyFrozen + "/" + enemy.getMembers().size())
                        .color(enemy.getColor());

                Component bar = myTeamPart
                        .append(sep)
                        .append(livesPart)
                        .append(sep)
                        .append(enemyTeamPart);

                member.sendActionBar(bar);
            }
        }
    }

    private String heartsDisplay(int livesLeft, int required) {
        return "❤".repeat(livesLeft) + "♡".repeat(Math.max(0, required - livesLeft));
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        player.teleportAsync(settings.getLobbyLocation());
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        if (teams != null) {
            teams.stream()
                    .filter(t -> t.isMember(participant))
                    .findFirst()
                    .ifPresent(t -> t.removeMember(participant));
        }

        Player player = participant.getPlayer();
        if (player != null) {
            UUID uuid = player.getUniqueId();
            if (frozenPlayers.containsKey(uuid)) {
                unfreezePlayerCleanup(player);
            }
            actionBarPauseUntil.remove(uuid);
            pendingFreezeHits.remove(uuid);
            pendingUnfreezeHits.remove(uuid);
            ColorManager.updatePlayersColor(player);
            if (countdownBossBar != null) countdownBossBar.getBossBar().removeViewer(player);
        }

        return super.removeParticipant(participant, doTeleport);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        ensureNotIllegal();

        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!isParticipant(attacker) || !isParticipant(victim)) {
            return;
        }

        event.setCancelled(true);

        FreezeTagTeam attackerTeam = getTeam(attacker);
        FreezeTagTeam victimTeam = getTeam(victim);
        if (attackerTeam == null || victimTeam == null) return;

        UUID victimUuid = victim.getUniqueId();
        UUID attackerUuid = attacker.getUniqueId();

        boolean attackerFrozen = frozenPlayers.containsKey(attackerUuid);
        boolean victimFrozen = frozenPlayers.containsKey(victimUuid);

        if (attackerFrozen) {
            if (victimFrozen && attackerTeam == victimTeam && !settings.isAllowUnfreezingWhileFrozen()) {
                pauseStatusActionBar(attacker, 1200);
                attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                attacker.sendActionBar(Util.color("<red>You can't unfreeze teammates while frozen."));
                return;
            }
            if (!victimFrozen && attackerTeam != victimTeam && !settings.isAllowFreezingWhileFrozen()) {
                pauseStatusActionBar(attacker, 1200);
                attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                attacker.sendActionBar(Util.color("<red>You can't freeze enemies while frozen."));
                return;
            }
            if (!victimFrozen && attackerTeam == victimTeam && !settings.isAllowHealingWhileFrozen()) {
                pauseStatusActionBar(attacker, 1200);
                attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                attacker.sendActionBar(Util.color("<red>You can't heal teammates while frozen."));
                return;
            }
        }

        if (victimFrozen) {
            if (attackerTeam == victimTeam) {
                int hits = pendingUnfreezeHits.merge(victimUuid, 1, Integer::sum);
                updateFrozenEggPosition(victim);
                int required = settings.getUnfreezeHitsRequired();
                if (hits >= required) {
                    pendingUnfreezeHits.remove(victimUuid);
                    unfreezePlayer(victim, victimTeam);
                    EventPlayer attackerEp = EventPlayerManager.getByUUID(attacker.getUniqueId());
                    attackerTeam.addScore(attackerEp, settings.getUnfreezePoints());
                } else {
                    pauseStatusActionBar(attacker, 1500);
                    attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
                    attacker.sendActionBar(Util.color("<green>Unfreezing <white>" + victim.getName() + "<green>: " + hits + "<dark_gray>/<green>" + required));
                }
            }
        } else {
            if (attackerTeam != victimTeam) {
                int hits = pendingFreezeHits.merge(victimUuid, 1, Integer::sum);
                int required = settings.getFreezeHitsRequired();
                if (hits >= required) {
                    pendingFreezeHits.remove(victimUuid);
                    freezePlayer(victim, victimTeam);
                    EventPlayer attackerEp = EventPlayerManager.getByUUID(attacker.getUniqueId());
                    attackerTeam.addScore(attackerEp, settings.getFreezePoints());
                } else {
                    applyHitKnockback(attacker, victim);
                    pauseStatusActionBar(attacker, 1500);
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
                    attacker.sendActionBar(Util.color("<aqua>Freezing <white>" + victim.getName() + "<aqua>: " + hits + "<dark_gray>/<aqua>" + required));
                }
            } else {
                int currentHits = pendingFreezeHits.getOrDefault(victimUuid, 0);
                if (currentHits > 0) {
                    int healedHits = currentHits - 1;

                    if (healedHits <= 0) {
                        pendingFreezeHits.remove(victimUuid);
                    } else {
                        pendingFreezeHits.put(victimUuid, healedHits);
                    }

                    int required = settings.getFreezeHitsRequired();
                    int livesLeft = required - healedHits;

                    pauseStatusActionBar(attacker, 1500);
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
                    attacker.sendActionBar(Util.color("<green>Healing <white>" + victim.getName() + "<green>: " + livesLeft + "<dark_gray>/<green>" + required));

                    pauseStatusActionBar(victim, 1500);
                    victim.playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.8f);
                    victim.sendActionBar(Util.color("<green>Healed by <white>" + attacker.getName() + "<green>: " + livesLeft + "<dark_gray>/<green>" + required));
                } else {
                    pauseStatusActionBar(attacker, 1000);
                    attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.8f);
                    attacker.sendActionBar(Util.color("<gray>" + victim.getName() + " is already at full health."));
                }
            }
        }
    }

    private void applyHitKnockback(Player attacker, Player victim) {
        Location attackerLoc = attacker.getLocation();
        Location victimLoc = victim.getLocation();

        org.bukkit.util.Vector knockback = victimLoc.toVector()
                .subtract(attackerLoc.toVector())
                .setY(0);

        if (knockback.lengthSquared() == 0) {
            knockback = attackerLoc.getDirection().setY(0);
        }

        knockback.normalize().multiply(0.45).setY(0.35);
        victim.setVelocity(knockback);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();

        FrozenState state = frozenPlayers.get(player.getUniqueId());
        if (state == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // ignore rotations
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        updateFrozenEggPosition(player);
    }

    private void pauseStatusActionBar(Player player, long millis) {
        actionBarPauseUntil.put(player.getUniqueId(), System.currentTimeMillis() + millis);
    }

    private boolean isStatusActionBarPaused(EventPlayer player) {
        Long until = actionBarPauseUntil.get(player.getUuid());
        if (until == null) return false;

        if (System.currentTimeMillis() >= until) {
            actionBarPauseUntil.remove(player.getUuid());
            return false;
        }

        return true;
    }

    private void freezePlayer(Player player, FreezeTagTeam team) {
        UUID uuid = player.getUniqueId();
        pendingUnfreezeHits.remove(uuid);

        Location loc = player.getEyeLocation().add(0, 0.5, 0);
        loc.setYaw(0);
        loc.setPitch(0);

        Executors.runSync(player, () -> {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            Util.setPlayerHead(head, EGG_TEXTURES.get(RANDOM.nextInt(EGG_TEXTURES.size())));

            ItemDisplay egg = (ItemDisplay) player.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
            egg.setPersistent(false);
            egg.setItemStack(head);
            egg.setInterpolationDuration(0);
            egg.setInterpolationDelay(-1);

            BoundingBox bb = player.getBoundingBox();
            Transformation transformation = egg.getTransformation();
            transformation.getScale().set(
                    (float) (bb.getWidthX() * 3),
                    (float) (bb.getHeight() * 3),
                    (float) (bb.getWidthZ() * 3)
            );
            egg.setTransformation(transformation);

            int durationTicks = Math.max(20, (int) (this.getDuration() / 50L) + 40);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false, false));
            applyFreezeAttributes(player);

            frozenPlayers.put(uuid, new FrozenState(egg));
            updateFrozenEggPosition(player);

            player.showTitle(Util.title("<aqua><b>FROZEN!", "<gray>A teammate must unfreeze you."));
            player.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 1f, 0.8f);
        });

        EventPlayer frozenEp = EventPlayerManager.getByUUID(uuid);
        String frozenName = frozenEp.getName();
        for (EventPlayer member : team.getMembers()) {
            if (!member.getUuid().equals(uuid)) {
                member.sendMessage("<red>" + frozenName + " has been frozen! Unfreeze them!");
            }
        }
    }

    private void updateFrozenEggPosition(Player player) {
        FrozenState state = frozenPlayers.get(player.getUniqueId());
        if (state == null) return;

        int required = Math.max(1, settings.getUnfreezeHitsRequired());
        int hits = pendingUnfreezeHits.getOrDefault(player.getUniqueId(), 0);

        double progress = Math.min(1.0, hits / (double) required);
        double baseY = 0.5;
        double maxDrop = 0.85;
        double yOffset = baseY - (maxDrop * 1.5 * progress);

        Location loc = player.getEyeLocation().clone().add(0, yOffset, 0);
        loc.setYaw(0);
        loc.setPitch(0);

        ItemDisplay egg = state.egg();
        egg.teleportAsync(loc);
    }

    private void unfreezePlayer(Player player, FreezeTagTeam team) {
        UUID uuid = player.getUniqueId();
        pendingUnfreezeHits.remove(uuid);
        FrozenState state = frozenPlayers.remove(uuid);
        if (state == null) return;

        Executors.runSync(player, () -> {
            state.egg().remove();
            player.removePotionEffect(PotionEffectType.GLOWING);
            removeFreezeAttributes(player);
            ColorManager.setTempPlayerColor(player, team.getColor());
            player.showTitle(Util.title("<green><b>UNFROZEN!", "<gray>You're free to move again!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        });
    }

    private void unfreezePlayerCleanup(Player player) {
        FrozenState state = frozenPlayers.remove(player.getUniqueId());
        if (state == null) return;

        Executors.runSync(player, () -> {
            state.egg().remove();
            player.removePotionEffect(PotionEffectType.GLOWING);
            removeFreezeAttributes(player);
            ColorManager.updatePlayersColor(player);
        });
    }

    private static ItemStack coloredLeather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyFreezeAttributes(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.addTransientModifier(FREEZE_SPEED_MOD);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.addTransientModifier(FREEZE_JUMP_MOD);
    }

    private static void removeFreezeAttributes(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FREEZE_SPEED_MOD);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(FREEZE_JUMP_MOD);
    }

    private void handleTokens() {
        if (teams == null) return;
        for (FreezeTagTeam team : teams) {
            for (EventPlayer member : team.getMembers()) {
                int score = team.getScore(member);
                tokenFormula.giveTokens(member, score);
                member.addPermanentScore(MinigameConstant.FREEZE_TAG, score);
            }
        }
    }

    @Nullable
    private FreezeTagTeam determineWinner() {
        if (teams == null) return null;
        FreezeTagTeam team1 = teams.get(0);
        FreezeTagTeam team2 = teams.get(1);
        long active1 = team1.getMembers().stream().filter(ep -> !frozenPlayers.containsKey(ep.getUuid())).count();
        long active2 = team2.getMembers().stream().filter(ep -> !frozenPlayers.containsKey(ep.getUuid())).count();
        if (active1 > active2) return team1;
        if (active2 > active1) return team2;
        return null;
    }

    @Nullable
    private FreezeTagTeam getTeam(Player player) {
        if (teams == null) return null;
        return teams.stream().filter(t -> t.isMember(player)).findFirst().orElse(null);
    }

    @Getter
    public static class FreezeTagTeam implements Scorer {

        private final NamedTextColor color;
        private final Color armorColor;
        private final Location spawnLocation;
        private final String teamName;
        private final Map<EventPlayer, Integer> scoreMap;

        public FreezeTagTeam(List<EventPlayer> members, FreezeTagDefinition.TeamConfig config, Location spawnLocation) {
            this.color = config.getNamedTextColor();
            this.armorColor = config.getArmorColor();
            this.spawnLocation = spawnLocation;
            this.teamName = config.getName();
            this.scoreMap = new HashMap<>();
            for (EventPlayer member : members) {
                scoreMap.put(member, 0);
            }
        }

        public List<EventPlayer> getMembers() {
            return List.copyOf(scoreMap.keySet());
        }

        public boolean isMember(Player player) {
            return scoreMap.keySet().stream().anyMatch(m -> m.getUuid().equals(player.getUniqueId()));
        }

        public boolean isMember(EventPlayer player) {
            return scoreMap.keySet().stream().anyMatch(m -> m.getUuid().equals(player.getUuid()));
        }

        public void removeMember(EventPlayer player) {
            scoreMap.remove(player);
        }

        public void addScore(EventPlayer player, int points) {
            scoreMap.merge(player, points, Integer::sum);
        }

        public int getScore(EventPlayer player) {
            return scoreMap.getOrDefault(player, 0);
        }

        @Override
        public String getName() {
            return teamName;
        }
    }

    private record FrozenState(ItemDisplay egg) {}
}
