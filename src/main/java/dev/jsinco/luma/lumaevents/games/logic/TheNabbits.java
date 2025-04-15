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
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.Objects;

public final class TheNabbits extends Minigame {

    // TODO: Game needs to properly reward players for specific actions.

    private static final int TICK_INTERVAL = 20; // 20t = 1s
    private static final int REVEAL_LOCATIONS_INTERVAL = 300; // 300t = 15s
    private static final String[] CAUGHT_MESSAGES = {
            "<yellow>%victim%</yellow> was caught by <dark_purple>%catcher%</dark_purple>!",
            "<dark_purple>%catcher%</dark_purple> nabbed <yellow>%victim%</yellow>!",
    };
    private static final ItemStack REGULAR_CARROT = new ItemStack(Material.CARROT);

    static  {
        REGULAR_CARROT.editMeta(itemMeta -> {
            itemMeta.addEnchant(Enchantment.MENDING, 1, true);
        });
    }

    private final NabbitPlayerSet nabbitParticipants;
    private final WorldTiedBoundingBox playArea;
    private final Location spawnPoint;
    private CountdownBossBar countdownBossBar;
    private int revealLocationsCounter = 0;

    public TheNabbits(TheNabbitsMinigameDefinition def) {
        super("The Nabbits", MinigameConstants.THE_NABBITS_DESC, MinigameConstants.THE_NABBITS_DURATION, TICK_INTERVAL, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.playArea = WorldTiedBoundingBox.of(def.getPlayArea().getLoc1(), def.getPlayArea().getLoc2());
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
            participant.teleportAsync(this.findValidSpawnLocation());
        }
        NabbitPlayer randomNabbitPlayer = Util.getRandom(this.nabbitParticipants);
        randomNabbitPlayer.changeRole(NabbitPlayer.Role.NABBIT_BOOTSTRAP, false);
        this.nabbitParticipants.forEach(NabbitPlayer::sendRoleTitle);

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
        for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
            nabbitPlayer.addTicksSurvived(TICK_INTERVAL);
            if (nabbitPlayer.isNabbit()) {
                nabbitPlayer.addNabbitGlow();
            }
            int secsUntilNextLocReveal = (REVEAL_LOCATIONS_INTERVAL - this.revealLocationsCounter) / TICK_INTERVAL;
            nabbitPlayer.sendActionBarTip(secsUntilNextLocReveal);
        }

        this.determineEarlyGameEnd();
    }

    @Override
    protected void handleStop() {
        for (NabbitPlayer nabbitPlayer : this.nabbitParticipants) {
            nabbitPlayer.handleGameEnd(() -> {
                this.audience.sendMessage(Component.text("Minigame has ended."));
                // TODO
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

        boolean denyEntry = Util.hasCustomItem(bukkitPlayer.getInventory().getContents());


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

        if (damager.isNabbit()) {
            damager.tryNabbitCatch(victim, () -> {
                this.sendAudienceMessage(
                        Util.getRandom(CAUGHT_MESSAGES)
                                .replace("%victim%", victim.getName())
                                .replace("%catcher%", damager.getName())
                );
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
            event.setCancelled(true);
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
        event.getPlayer().sendMessage("debug: player picked up a carrot");
    }


    private void spawnCarrots(int amount) {
        for (int i = 0; i < amount; i++) {
            Location location = this.findValidSpawnLocation();

            // Prevent stacking
            ItemStack itemStack = REGULAR_CARROT.clone();
            itemStack.editMeta((meta) -> meta.displayName(Component.text(Math.random())));

            // Spawn dropped item
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                location.getWorld().dropItem(location, itemStack);
            });
        }
    }

    private Location findValidSpawnLocation() {
        Location location = this.playArea.getRandomLocation();
        int attempts = 0;
        while (attempts < 10) {
            if (location.getBlock().isEmpty()) {
                break;
            }
            attempts++;
        }
        return location.toCenterLocation();
    }

    private void determineEarlyGameEnd() {
        if (this.nabbitParticipants.getFleeing().isEmpty()) {
            this.sendAudienceMessage("All fleeing players have been caught!");
            this.stop();
        }
    }


}
