package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TheNabbitsMinigameDefinition;
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitPickupCarrot;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListeners;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstants;
import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayer;
import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayerSet;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

public final class TheNabbits extends Minigame {

    private static final int TICK_INTERVAL = 20; // 20t = 1s
    private static final int REVEAL_LOCATIONS_INTERVAL = 300; // 300t = 15s
    private static final String[] CAUGHT_MESSAGES = {
            "<yellow>%victim%</yellow> was caught by <dark_purple>%catcher%</dark_purple>!",
            "<dark_purple>%catcher%</dark_purple> nabbed <yellow>%victim%</yellow>!",
            "<yellow>%victim%</yellow> was a victim of <dark_purple>%catcher%</dark_purple>...",
            "<dark_purple>%catcher%</dark_purple> stuffed <yellow>%victim%</yellow> in their bag...",
    };
    private static final ItemStack REGULAR_CARROT = new ItemStack(Material.CARROT);

    static  {
        REGULAR_CARROT.editMeta(itemMeta -> {
            itemMeta.addEnchant(Enchantment.MENDING, 1, true);
        });
    }

    private final NabbitPlayerSet nabbitParticipants;
    private final List<WorldTiedBoundingBox> playAreas; // TODO: Needs to have multiple play areas because of how map is setup
    private final Location spawnPoint;
    private CountdownBossBar countdownBossBar;
    private int revealLocationsCounter = 0;
    private boolean earlyGameEnd = false;

    public TheNabbits(TheNabbitsMinigameDefinition def) {
        super("The Nabbits", MinigameConstants.THE_NABBITS_DESC, MinigameConstants.THE_NABBITS_DURATION, TICK_INTERVAL, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.playAreas = def.getPlayAreas().stream().map(playArea -> WorldTiedBoundingBox.of(playArea.getLoc1(), playArea.getLoc2())).toList();
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.nabbitParticipants = new NabbitPlayerSet();
    }

    @Override
    protected void handleStart() {
        if (this.participants.size() < 2) {
            this.sendAudienceMessage("Not enough players to start the game.");
            this.stop();
            return;
        }

        for (EventPlayer participant : this.participants) {
            nabbitParticipants.add(new NabbitPlayer(participant));
            participant.teleportAsync(this.findValidSpawnLocation(true));
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
            int secsUntilNextLocReveal = (REVEAL_LOCATIONS_INTERVAL - this.revealLocationsCounter) / TICK_INTERVAL;
            nabbitPlayer.sendActionBarTip(secsUntilNextLocReveal);
        }

        this.determineEarlyGameEnd();
    }

    @Override
    protected void handleStop() {
        for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
            EventPlayer eventPlayer = nabbitPlayer.getEventPlayer();
            Player bukkitPlayer = eventPlayer.getPlayer();

            if (bukkitPlayer != null) {
                // TODO: Just teleport to spawn
                bukkitPlayer.teleportAsync(this.spawnPoint).whenComplete((b, t) -> {
                    bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    if (this.earlyGameEnd) {
                        eventPlayer.sendTitle("<dark_purple>Nabbits Win", "All fleeing players were caught.");
                    } else {
                        eventPlayer.sendTitle("<green>Fleeing Players Win", "The Nabbits didn't catch everyone.");
                    }
                });
            }
            nabbitPlayer.handleGameEnd(() -> {
                // TODO: Test this
                int tokens = (int) (nabbitPlayer.getScore() / 3.5);
                TokenExchanging.give(bukkitPlayer, TokenExchanging.TokenType.CARROT, tokens);
            });
        }
        this.nabbitParticipants.clear();
        if (this.countdownBossBar != null) {
            this.countdownBossBar.stop(false);
        }
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            this.boundingBox.getEntities(Item.class).forEach(Entity::remove);
        });
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null) {
            return false;
        }

        boolean denyEntry = Util.hasCustomItem(bukkitPlayer.getInventory().getContents(), "easter-basket-token", "easter-carrot-token");


        if (denyEntry) {
            player.sendMessage(
                    "Hold up! Just so you know, we don't allow custom items in this minigame... " +
                            "Try removing any custom items from your inventory and join again!"
            );
            return false;
        }
        bukkitPlayer.teleportAsync(this.spawnPoint);
        return true;
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
        ExplorerListeners.fire(new NabbitPickupCarrot(1), bukkitPlayer);
        nabbitPlayer.addScore(1.0);
        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        bukkitPlayer.spawnParticle(Particle.HAPPY_VILLAGER, bukkitPlayer.getLocation(), 3, 0.5, 0.5, 0.5, 0.1);
        nabbitPlayer.getEventPlayer().sendActionBar("<gold>You picked up a carrot!");
    }


    private void spawnCarrots(int amount) {
        for (int i = 0; i < amount; i++) {
            Location location = this.findValidSpawnLocation(false);

            // Prevent stacking
            ItemStack itemStack = REGULAR_CARROT.clone();
            itemStack.editMeta((meta) -> meta.displayName(Component.text(Math.random())));

            // Spawn dropped item
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                location.getWorld().dropItem(location, itemStack);
            });
        }
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
        return rayTraceResult.getHitBlock() != null;
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
