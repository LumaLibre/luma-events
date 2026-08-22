package dev.lumas.events.games.logic;

import dev.lumas.events.configurable.sectors.TheNabbitsMinigameDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.NabbitPlayer;
import dev.lumas.events.games.models.NabbitPlayerSet;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.TheNabbitsTokenFormula;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

public final class TheNabbits extends InventoryUnifiedMinigame {

    private static final int TICK_INTERVAL = 20; // 20t = 1s
    private static final int REVEAL_LOCATIONS_INTERVAL = 300; // 300t = 15s
    private static final String[] CAUGHT_MESSAGES = {
            "<yellow>%victim%</yellow> was caught by <dark_purple>%catcher%</dark_purple>!",
            "<dark_purple>%catcher%</dark_purple> nabbed <yellow>%victim%</yellow>!",
            "<yellow>%victim%</yellow> was a victim of <dark_purple>%catcher%</dark_purple>...",
            "<dark_purple>%catcher%</dark_purple> stuffed <yellow>%victim%</yellow> in their bag...",
    };
    private static final List<Material> BLACKLISTED_SPAWN_MATERIALS = List.of(
            Material.BARRIER
    );
    private static final List<Enchantment> BLACKLISTED_ENCHANTMENTS = List.of(
            Enchantment.FIRE_ASPECT,
            Enchantment.KNOCKBACK,
            Enchantment.THORNS
    );
    private static final ItemStack REGULAR_CARROT = new ItemStack(Material.CARROT);

    static  {
        REGULAR_CARROT.editMeta(itemMeta -> {
            itemMeta.addEnchant(Enchantment.MENDING, 1, true);
        });
        Util.setPersistentKey(REGULAR_CARROT, "nabbit_carrot", PersistentDataType.BOOLEAN, true);
    }

    private final NabbitPlayerSet nabbitParticipants;
    private final List<WorldTiedBoundingBox> playAreas; // TODO: Needs to have multiple play areas because of how map is setup
    private final Location spawnPoint;
    private CountdownBossBar countdownBossBar;
    private int revealLocationsCounter = 0;
    private boolean earlyGameEnd = false;

    private final Scoreboard<NabbitPlayer> scoreboard;
    private final TheNabbitsTokenFormula tokenFormula;

    public TheNabbits(TheNabbitsMinigameDefinition def) {
        super("The Nabbits","Score as many points as possible.", 360000L, TICK_INTERVAL, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.playAreas = def.getPlayAreas().stream().map(playArea -> WorldTiedBoundingBox.of(playArea.getLoc1(), playArea.getLoc2())).toList();
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.nabbitParticipants = new NabbitPlayerSet();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new TheNabbitsTokenFormula();
    }

    @Override
    protected void handleStart() {

        for (EventPlayer participant : this.participants) {
            nabbitParticipants.add(new NabbitPlayer(participant));
            participant.teleportAsync(this.findValidSpawnLocation(true));
            participant.operatePlayer(LivingEntity::clearActivePotionEffects);
        }
        NabbitPlayer randomNabbitPlayer = Util.getRandom(this.nabbitParticipants);
        randomNabbitPlayer.changeRole(NabbitPlayer.Role.NABBIT_BOOTSTRAP, false);
        this.nabbitParticipants.forEach(NabbitPlayer::sendRoleTitle);
        this.sendAudienceMessage("<dark_purple>" + randomNabbitPlayer.getName() + " is the Nabbit, run!");

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<dark_purple><b>Time Remaining</b><gray>:</gray> <b>%s</b></dark_purple>")
                .color(BossBar.Color.PURPLE)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        this.countdownBossBar.start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        // Let's make sure there are actually Nabbits to catch our runners.
        boolean allNabbitsOffline = this.nabbitParticipants.getNabbits().stream()
                .map(nabbit -> nabbit.getEventPlayer().getPlayer())
                .noneMatch(Objects::nonNull);
        if (allNabbitsOffline) {
            if (this.isAllParticipantsOffline()) {
                this.stop();
                return;
            }
            NabbitPlayer newNabbit = Util.getRandom(this.nabbitParticipants.getRoles(NabbitPlayer.Role.FLEEING, NabbitPlayer.Role.RABBIT));
            newNabbit.changeRole(NabbitPlayer.Role.NABBIT_BOOTSTRAP, true);
            this.sendAudienceMessage("A new Nabbit has been assigned!");
        }

        // Spawn carrots for fleeing players and rabbits
        this.spawnCarrots(2);

        // Reveal locations of fleeing players
        this.revealLocationsCounter += TICK_INTERVAL;
        if (this.revealLocationsCounter >= REVEAL_LOCATIONS_INTERVAL) {
            this.revealLocationsCounter = 0;
            for (NabbitPlayer fleeing : this.nabbitParticipants.getFleeing()) {
                fleeing.addStandardGlow();
            }
        }


        // Make nabbits glow and send actionbar tooltip
        // TODO: Dynamic time differences between pings?
        for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
            nabbitPlayer.addTicksSurvived(TICK_INTERVAL);
            if (nabbitPlayer.isNabbit()) {
                nabbitPlayer.addNabbitEffects();
            }

            if (nabbitPlayer.getTicksSurvived() % 1200 == 0) {
                this.scoreboard.addScore(nabbitPlayer, 2);
            }
            int secsUntilNextLocReveal = (REVEAL_LOCATIONS_INTERVAL - this.revealLocationsCounter) / TICK_INTERVAL;
            nabbitPlayer.sendActionBarTip(secsUntilNextLocReveal);
        }

        this.determineEarlyGameEnd();
    }

    @Override
    protected void handleStop() {
        this.scoreboard.handleGameEnd(this.audience, () -> {
            Location dropOffLocation = this.getGameDropOffLocation();
            if (dropOffLocation == null) {
                dropOffLocation = this.spawnPoint;
            }

            for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
                EventPlayer eventPlayer = nabbitPlayer.getEventPlayer();
                Player bukkitPlayer = eventPlayer.getPlayer();

                if (bukkitPlayer != null) {
                    Executors.teleportSafely(bukkitPlayer, dropOffLocation).whenComplete((b, t) -> {
                        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        if (this.earlyGameEnd) {
                            eventPlayer.sendTitle("<dark_purple>Nabbits Win", "All fleeing players were caught.");
                        } else {
                            eventPlayer.sendTitle("<green>Fleeing Players Win", "The Nabbits didn't catch everyone.");
                        }
                    });
                }
                nabbitPlayer.handleGameEnd(() -> {
                    // no op
                });
            }
        });

        this.nabbitParticipants.clear();
        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }

        unsafe(() -> {
            this.boundingBox.getEntities(Item.class).forEach(entity -> {
                Executors.runSync(entity, entity::remove);
            });
        });
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null) {
            return false;
        }

        this.teleportOnJoin(player, this.spawnPoint);
        return true;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        NabbitPlayer nabbitPlayer = this.nabbitParticipants.getNabbitPlayer(participant);
        if (nabbitPlayer == null) {
            return;
        }
        int finalScore = this.scoreboard.getScore(nabbitPlayer);
        tokenFormula.giveTokens(participant, finalScore);
        participant.addPermanentScore(MinigameConstant.THE_NABBITS, finalScore);
    }


    @EventHandler
    public void onPlayerDamageOther(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getDamager() instanceof Player bukkitDamager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player bukkitVictim)) {
            return;
        }

        NabbitPlayer damager = this.nabbitParticipants.getNabbitPlayer(bukkitDamager);
        NabbitPlayer victim = this.nabbitParticipants.getNabbitPlayer(bukkitVictim);

        // check the legality of the hit
        if (damager == null || victim == null || !isInBoundingBox(bukkitDamager, bukkitVictim)) {
            return;
        }

        if (damager.getRole() == NabbitPlayer.Role.RABBIT && victim.isNabbit()) {
            event.setCancelled(true);
            return;
        }

        if (damager.isNabbit()) {
            damager.tryNabbitCatch(victim, (role) -> {
                this.sendAudienceMessage(
                        Util.getRandom(CAUGHT_MESSAGES)
                                .replace("%victim%", victim.getName())
                                .replace("%catcher%", damager.getName())
                );
                for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
                    Player bukkitPlayer = nabbitPlayer.getEventPlayer().getPlayer();
                    if (bukkitPlayer == null) {
                        continue;
                    }
                    bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 1.0f);
                }
                if (role == NabbitPlayer.Role.NABBIT) {
                    this.sendAudienceMessage(victim.getName() + " is now a <dark_purple>Nabbit</dark_purple>!");
                }
                this.scoreboard.addScore(damager, 1);
                this.determineEarlyGameEnd();
            });
        }

        event.setDamage(0.0);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();

        NabbitPlayer victim = this.nabbitParticipants.getNabbitPlayer(event.getEntity());
        if (victim != null && isInBoundingBox(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player bukkitPlayer)) {
            return;
        }

        NabbitPlayer victim = this.nabbitParticipants.getNabbitPlayer(bukkitPlayer);
        if (victim != null && isInBoundingBox(bukkitPlayer)) {
            event.setDamage(0.0);
        }
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        NabbitPlayer nabbitPlayer = this.nabbitParticipants.getNabbitPlayer(bukkitPlayer);
        if (nabbitPlayer == null || !isInBoundingBox(bukkitPlayer)) {
            return;
        }

        event.setCancelled(true);
        if (nabbitPlayer.isNabbit()) {
            return;
        }

        Item item = event.getItem();

        if (item.getItemStack().getType() != Material.CARROT) {
            return;
        }

        item.remove();

        if (Util.RANDOM.nextInt(10) == 5) {
            this.scoreboard.addScore(nabbitPlayer, 1);
        }
        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        bukkitPlayer.spawnParticle(Particle.HAPPY_VILLAGER, bukkitPlayer.getLocation(), 3, 0.5, 0.5, 0.5, 0.1);
        nabbitPlayer.getEventPlayer().sendActionBar("<gold>You picked up a carrot!");
    }

    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        this.ensureNotIllegal();
        Player bukkitPlayer = event.getPlayer();
        NabbitPlayer nabbitPlayer = this.nabbitParticipants.getNabbitPlayer(bukkitPlayer);
        if (nabbitPlayer == null || !isInBoundingBox(bukkitPlayer)) {
            return;
        }

        if (event.getItem().getType() == Material.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }


    private void spawnCarrots(int amount) {
        for (int i = 0; i < amount; i++) {
            Location location = this.findValidSpawnLocation(false);

            // Prevent stacking
            ItemStack itemStack = REGULAR_CARROT.clone();
            itemStack.editMeta((meta) -> meta.displayName(Component.text(Math.random())));

            // Spawn dropped item
            Executors.runSync(location, () -> {
                location.getWorld().dropItem(location, itemStack);
            });
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        this.ensureNotIllegal();
        if (!this.boundingBox.contains(event.getEntity()) || !(event.getEntity().getShooter() instanceof Player bukkitPlayer)) {
            return;
        }
        NabbitPlayer nabbitPlayer = this.nabbitParticipants.getNabbitPlayer(bukkitPlayer);
        if (nabbitPlayer == null) {
            return;
        }
        event.setCancelled(true);
    }

    private Location findValidSpawnLocation(boolean player) {
        Location location = Util.getRandom(this.playAreas).getRandomLocation();
        int attempts = 0;
        while (!location.getBlock().isEmpty() || (player && !this.rayTraceForBlockBelow(location))) {
            location = Util.getRandom(this.playAreas).getRandomLocation();
            if (attempts++ > 15) {
                break;
            }
        }
        return location.toCenterLocation();
    }

    private boolean rayTraceForBlockBelow(Location location) {
        Vector direction = location.clone().add(0.0, -3.0, 0.0).toVector().subtract(location.toVector());
        RayTraceResult rayTraceResult = location.getWorld().rayTraceBlocks(location, direction, 3.0);
        if (rayTraceResult == null) {
            return false;
        }


        Block hitBlock = rayTraceResult.getHitBlock();
        if (hitBlock == null) {
            return false;
        }

        return !BLACKLISTED_SPAWN_MATERIALS.contains(hitBlock.getType());
    }

    private boolean determineEarlyGameEnd() {
        if (this.nabbitParticipants.getFleeing().isEmpty()) {
            this.earlyGameEnd = true;
            this.sendAudienceMessage("All fleeing players have been caught!");
            this.stop();
            return true;
        }
        return false;
    }

    private boolean isAllParticipantsOffline() {
        return this.participants.stream()
                .map(EventPlayer::getPlayer)
                .noneMatch(Objects::nonNull);
    }


}
