package dev.lumas.events.games.logic;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.FreezeTagDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.tokenformula.Paintball2_1TokenFormula;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Couple;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class FreezeTag extends InventoryUnifiedMinigame {

    private static final AttributeModifier FREEZE_MOD = new AttributeModifier(
            new NamespacedKey(EventMain.getInstance(), "freeze_tag"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private final FreezeTagDefinition settings;
    private final Paintball2_1TokenFormula tokenFormula;
    private CountdownBossBar countdownBossBar;
    private List<FreezeTagTeam> teams;

    private volatile boolean gameEnded = false;
    private volatile boolean roundInProgress = false;
    private final Map<FreezeTagTeam, Integer> roundWins = new HashMap<>();
    private int currentRound = 0;
    private final ConcurrentHashMap<UUID, FrozenState> frozenPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingFreezeHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingUnfreezeHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> actionBarPauseUntil = new ConcurrentHashMap<>();

    public FreezeTag(FreezeTagDefinition settings) {
        super("Freeze Tag", "Freeze all enemy players to win!", Util.secsToMillis(settings.getTimeLimitSeconds()), 10, true, true, true, false);
        this.settings = settings;
        this.boundingBox = WorldTiedBoundingBox.of(settings.getRegion().getLoc1(), settings.getRegion().getLoc2());
        this.tokenFormula = new Paintball2_1TokenFormula();
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
                    GlowColorManager.getInstance().setTransientColor(player, team.getColor());
                    player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, team.getArmorColor()));
                    //player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, team.getArmorColor()));
                    //player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, team.getArmorColor()));
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                });
            }
        }

        this.roundWins.clear();
        for (FreezeTagTeam team : this.teams) {
            roundWins.put(team, 0);
        }
        this.currentRound = 1;
        this.roundInProgress = true;

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
        gameEnded = true;
        roundInProgress = false;
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

        CountdownBossBar.builder()
                .audience(this.audience)
                .color(BossBar.Color.BLUE)
                .title("<aqua><b>Game Over")
                .seconds(10)
                .callback(() -> {
                    this.sendAudienceMessage("This minigame has concluded.");
                    Executors.teleportGroupAsync(this.participants, settings.getLobbyLocation());
                })
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
                endRound();
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

                Component middlePart;
                if (frozenPlayers.containsKey(member.getUuid())) {
                    int unfreezeRequired = settings.getUnfreezeHitsRequired();
                    int unfreezeTaken = pendingUnfreezeHits.getOrDefault(member.getUuid(), 0);
                    int hitsLeft = Math.max(0, unfreezeRequired - unfreezeTaken);
                    middlePart = Component.text("❄".repeat(hitsLeft)).color(NamedTextColor.AQUA)
                            .append(Component.text("❄".repeat(unfreezeRequired - hitsLeft)).color(NamedTextColor.DARK_GRAY));
                } else {
                    int required = settings.getFreezeHitsRequired();
                    int takenHits = pendingFreezeHits.getOrDefault(member.getUuid(), 0);
                    int livesLeft = Math.max(0, required - takenHits);
                    middlePart = Component.text(heartsDisplay(livesLeft, required))
                            .color(NamedTextColor.RED);
                }

                Component myTeamPart = Component.text(team.getName() + " ❄ " + myFrozen + "/" + team.getMembers().size())
                        .color(team.getColor());

                Component enemyTeamPart = Component.text(enemy.getName() + " ❄ " + enemyFrozen + "/" + enemy.getMembers().size())
                        .color(enemy.getColor());

                member.sendActionBar(myTeamPart.append(sep).append(middlePart).append(sep).append(enemyTeamPart));
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
            GlowColorManager.getInstance().update(player);
            if (countdownBossBar != null) countdownBossBar.getBossBar().removeViewer(player);
        }

        return super.removeParticipant(participant, doTeleport);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (gameEnded && boundingBox instanceof WorldTiedBoundingBox wtbb) {
            if (wtbb.contains(attacker.getLocation()) || wtbb.contains(victim.getLocation())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!isParticipant(attacker) || !isParticipant(victim)) {
            return;
        }

        event.setCancelled(true);
        if (!roundInProgress) return;
        ensureNotIllegal();

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
                int required = settings.getUnfreezeHitsRequired();
                if (hits >= required) {
                    pendingUnfreezeHits.remove(victimUuid);
                    unfreezePlayer(victim, victimTeam);
                    EventPlayer attackerEp = EventPlayerManager.getByUUID(attacker.getUniqueId());
                    attackerTeam.addScore(attackerEp, settings.getUnfreezePoints());
                } else {
                    updateIceAge(victim);
                    pauseStatusActionBar(attacker, 1500);
                    victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                    attacker.sendActionBar(Util.color("<green>Unfreezing <white>" + victim.getName() + "<green>: " + hits + "<dark_gray>/<green>" + required));
                    Location crackCenter = victim.getLocation().clone().add(0, victim.getHeight() / 2.0, 0);
                    victim.getWorld().spawnParticle(Particle.BLOCK, crackCenter, 20, 0.35, 0.7, 0.35, 0.05,
                            Material.FROSTED_ICE.createBlockData());
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
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.3f);
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
                    attacker.sendActionBar(Util.color("<green>Healing <white>" + victim.getName() + "<green>: " + livesLeft + "<dark_gray>/<green>" + required));

                    pauseStatusActionBar(victim, 1500);
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.7f);
                    victim.sendActionBar(Util.color("<green>Healed by <white>" + attacker.getName() + "<green>: " + livesLeft + "<dark_gray>/<green>" + required));

                    Location healCenter = victim.getLocation().clone().add(0, victim.getHeight() + 0.2, 0);
                    victim.getWorld().spawnParticle(Particle.HEART, healCenter, 6, 0.3, 0.1, 0.3, 0);
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
    public void onFrozenPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        FrozenState state = frozenPlayers.get(player.getUniqueId());
        if (state == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;

        player.setVelocity(new Vector(0, 0, 0));
        Location back = state.frozenAt().clone();
        back.setYaw(to.getYaw());
        back.setPitch(to.getPitch());
        player.teleportAsync(back);
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

        Location frozenAt = player.getLocation().clone();
        Location displayLoc = frozenAt.clone();
        displayLoc.setYaw(0);
        displayLoc.setPitch(0);

        Executors.runSync(player, () -> {
            BlockDisplay iceDisplay = (BlockDisplay) player.getWorld().spawnEntity(displayLoc, EntityType.BLOCK_DISPLAY);
            iceDisplay.setPersistent(false);
            iceDisplay.setInterpolationDuration(0);
            iceDisplay.setInterpolationDelay(-1);

            Ageable frostedIce = (Ageable) Bukkit.createBlockData(Material.FROSTED_ICE);
            frostedIce.setAge(0);
            iceDisplay.setBlock(frostedIce);

            BoundingBox bb = player.getBoundingBox();
            float w = (float) bb.getWidthX() + 0.4f;
            float h = (float) bb.getHeight() + 0.2f;
            Transformation t = iceDisplay.getTransformation();
            t.getTranslation().set(-w / 2f, 0f, -w / 2f);
            t.getScale().set(w, h, w);
            iceDisplay.setTransformation(t);

            int durationTicks = Math.max(20, (int) (this.getDuration() / 50L) + 40);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false, false));
            applyFreezeAttributes(player);
            player.setVelocity(new Vector(0, 0, 0));

            frozenPlayers.put(uuid, new FrozenState(iceDisplay, frozenAt));

            Location center = frozenAt.clone().add(0, player.getHeight() / 2.0, 0);
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 60, 0.4, 0.9, 0.4, 0.08);

            player.showTitle(Util.title("<aqua><b>FROZEN!", "<gray>A teammate must unfreeze you."));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.7f);
        });

        EventPlayer frozenEp = EventPlayerManager.getByUUID(uuid);
        String frozenName = frozenEp.getName();
        for (EventPlayer member : team.getMembers()) {
            if (!member.getUuid().equals(uuid)) {
                member.sendMessage("<red>" + frozenName + " has been frozen! Unfreeze them!");
            }
        }
    }

    private int iceAgeForHits(int hits, int required) {
        if (required <= 1) return 0;
        return Math.min(3, hits * 4 / required);
    }

    private void updateIceAge(Player player) {
        int required = Math.max(1, settings.getUnfreezeHitsRequired());
        int hits = pendingUnfreezeHits.getOrDefault(player.getUniqueId(), 0);
        int age = iceAgeForHits(hits, required);

        Executors.runSync(player, () -> {
            FrozenState state = frozenPlayers.get(player.getUniqueId());
            if (state == null) return;
            Ageable frostedIce = (Ageable) Bukkit.createBlockData(Material.FROSTED_ICE);
            frostedIce.setAge(age);
            state.iceDisplay().setBlock(frostedIce);
        });
    }

    private void unfreezePlayer(Player player, FreezeTagTeam team) {
        UUID uuid = player.getUniqueId();
        pendingUnfreezeHits.remove(uuid);
        FrozenState state = frozenPlayers.remove(uuid);
        if (state == null) return;

        Executors.runSync(player, () -> {
            Location center = player.getLocation().clone().add(0, player.getHeight() / 2.0, 0);
            player.getWorld().spawnParticle(Particle.BLOCK, center, 80, 0.4, 0.9, 0.4, 0.15,
                    Material.FROSTED_ICE.createBlockData());
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 40, 0.5, 0.9, 0.5, 0.1);

            state.iceDisplay().remove();
            player.removePotionEffect(PotionEffectType.GLOWING);
            removeFreezeAttributes(player);
            GlowColorManager.getInstance().setTransientColor(player, team.getColor());
            player.showTitle(Util.title("<green><b>UNFROZEN!", "<gray>You're free to move again!"));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
        });
    }

    private void unfreezePlayerCleanup(Player player) {
        FrozenState state = frozenPlayers.remove(player.getUniqueId());
        if (state == null) return;

        Executors.runSync(player, () -> {
            state.iceDisplay().remove();
            player.removePotionEffect(PotionEffectType.GLOWING);
            removeFreezeAttributes(player);
            GlowColorManager.getInstance().update(player);
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
        if (speed != null) speed.addTransientModifier(FREEZE_MOD);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.addTransientModifier(FREEZE_MOD);
        AttributeInstance gravity = player.getAttribute(Attribute.GRAVITY);
        if (gravity != null) gravity.addTransientModifier(FREEZE_MOD);
    }

    private static void removeFreezeAttributes(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FREEZE_MOD);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(FREEZE_MOD);
        AttributeInstance gravity = player.getAttribute(Attribute.GRAVITY);
        if (gravity != null) gravity.removeModifier(FREEZE_MOD);
    }

    private void handleTokens() {
        if (teams == null) return;

        FreezeTagTeam winner = determineWinner();

        for (FreezeTagTeam team : teams) {
            for (EventPlayer member : team.getMembers()) {
                int score = team.getScore(member);
                boolean isWinner = team == winner;
                int finalScore = tokenFormula.giveTokens(member, Couple.of(score, isWinner));
                member.addPermanentScore(MinigameConstant.FREEZE_TAG, score);
            }
        }
    }

    private synchronized void endRound() {
        if (!roundInProgress || gameEnded) return;
        roundInProgress = false;

        FreezeTagTeam roundWinner = determineCurrentRoundWinner();
        if (roundWinner != null) {
            roundWins.merge(roundWinner, 1, Integer::sum);
        }

        int winsRequired = settings.getRoundWinsRequired();
        FreezeTagTeam matchWinner = roundWins.entrySet().stream()
                .filter(e -> e.getValue() >= winsRequired)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        String roundWinnerName = roundWinner != null ? roundWinner.getName() : "Nobody (Tie)";
        this.audience.showTitle(Util.title(
                "<yellow>Round " + currentRound + " Over!",
                "<gold>" + roundWinnerName + " wins!"
        ));

        if (matchWinner != null) {
            this.stop();
        } else {
            Executors.runDelayedAsync(TimeUnit.SECONDS, 5, t -> startNewRound());
        }
    }

    private void startNewRound() {
        if (gameEnded) return;
        currentRound++;

        new ArrayList<>(frozenPlayers.keySet()).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) unfreezePlayerCleanup(player);
        });
        frozenPlayers.clear();
        pendingFreezeHits.clear();
        pendingUnfreezeHits.clear();
        actionBarPauseUntil.clear();

        for (FreezeTagTeam team : teams) {
            for (EventPlayer member : team.getMembers()) {
                member.operatePlayer(player -> {
                    player.teleportAsync(team.getSpawnLocation().toCenterLocation());
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                });
            }
        }

        roundInProgress = true;

        this.audience.showTitle(Util.title(
                "<green><b>Round " + currentRound + "!",
                "<gray>" + buildRoundWinsDisplay()
        ));
    }

    private String buildRoundWinsDisplay() {
        if (teams == null || roundWins.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            FreezeTagTeam t = teams.get(i);
            if (i > 0) sb.append(" | ");
            sb.append(t.getName()).append(": ").append(roundWins.getOrDefault(t, 0))
              .append("/").append(settings.getRoundWinsRequired());
        }
        return sb.toString();
    }

    @Nullable
    private FreezeTagTeam determineWinner() {
        if (teams == null) return null;
        if (!roundWins.isEmpty()) {
            int maxWins = roundWins.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (maxWins > 0) {
                List<FreezeTagTeam> leaders = roundWins.entrySet().stream()
                        .filter(e -> e.getValue() == maxWins)
                        .map(Map.Entry::getKey)
                        .toList();
                if (leaders.size() == 1) return leaders.get(0);
            }
        }
        return determineCurrentRoundWinner();
    }

    @Nullable
    private FreezeTagTeam determineCurrentRoundWinner() {
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

    private record FrozenState(BlockDisplay iceDisplay, Location frozenAt) {}
}
