package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.TheNabbitsMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstants;
import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayer;
import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayerSet;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public final class TheNabbits extends Minigame {

    // TODO: Game needs to find valid, random, spawn locations for all players.
    // TODO: Game needs to spawn 'crops' that rabbits and fleeing players can pickup.
    // TODO: Game needs to properly reward players for specific actions.

    private static final String[] CAUGHT_MESSAGES = {
            "<yellow>%victim%</yellow> was caught by <dark_purple>%catcher%</dark_purple>!",
            "<dark_purple>%catcher%</dark_purple> nabbed <yellow>%victim%</yellow>!",
    };
    private static final PotionEffect GLOWING = new PotionEffect(
            PotionEffectType.GLOWING,
            600,
            1,
            false,
            false,
            false
    );
    private static final ItemStack REGULAR_CARROT = new ItemStack(Material.CARROT);

    static  {
        REGULAR_CARROT.editMeta(itemMeta -> {
            itemMeta.addEnchant(Enchantment.MENDING, 1, true);
        });
    }

    private final NabbitPlayerSet nabbitParticipants;
    private final WorldTiedBoundingBox playArea;
    private final Location spawnPoint;

    public TheNabbits(TheNabbitsMinigameDefinition def) {
        super("The Nabbits", MinigameConstants.THE_NABBITS_DESC, MinigameConstants.THE_NABBITS_DURATION, 100L, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.playArea = WorldTiedBoundingBox.of(def.getPlayArea().getLoc1(), def.getPlayArea().getLoc2());
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();

        this.nabbitParticipants = new NabbitPlayerSet();
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.participants) {
            nabbitParticipants.add(new NabbitPlayer(participant));
        }
        NabbitPlayer randomNabbitPlayer = Util.getRandom(this.nabbitParticipants);
        randomNabbitPlayer.changeRole(NabbitPlayer.Role.NABBIT_BOOTSTRAP);
    }

    @Override
    protected void onRunnable(long timeLeft) {

        // Let's make sure there are actually Nabbits to catch our runners.
        boolean allNabbitsOffline = this.nabbitParticipants.getNabbits().stream()
                .map(nabbit -> nabbit.getEventPlayer().getPlayer())
                .noneMatch(Objects::nonNull);
        if (allNabbitsOffline) {
            NabbitPlayer newNabbit = Util.getRandom(this.nabbitParticipants.getRoles(NabbitPlayer.Role.FLEEING, NabbitPlayer.Role.RABBIT));
            newNabbit.changeRole(NabbitPlayer.Role.NABBIT_BOOTSTRAP);
            this.sendAudienceMessage("A new Nabbit has been assigned!");
        }

        // Make Nabbits glow
        for (NabbitPlayer nabbit : this.nabbitParticipants.getNabbits()) {
            Player bukkitPlayer = nabbit.getEventPlayer().getPlayer();
            if (bukkitPlayer != null) {
                Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                    ColorManager.setTempPlayerColor(bukkitPlayer, ChatColor.DARK_PURPLE);
                    bukkitPlayer.addPotionEffect(GLOWING);
                });
            }
        }

        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            this.spawnCarrots(5);
        });
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
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null) { // Should never happen
            player.sendMessage("Something went wrong.");
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
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        this.ensureNotIllegal();

        NabbitPlayer nabbitPlayer = this.nabbitParticipants.getNabbitPlayer(event.getPlayer());
        if (nabbitPlayer == null || nabbitPlayer.isNabbit() || !isInBoundingBox(event.getPlayer())) {
            return;
        }

        Item item = event.getItem();

        if (item.getItemStack().getType() != Material.CARROT) {
            return;
        }

        event.setCancelled(true);
        item.remove();
        event.getPlayer().sendMessage("debug: player picked up a carrot");
    }


    private void spawnCarrots(int amount) {
        for (int i = 0; i < amount; i++) {
            Location location = this.playArea.getRandomLocation();
            int attempts = 0;
            while (attempts < 10) {
                if (location.getBlock().isEmpty()) {
                    break;
                }
                attempts++;
            }

            // Prevent stacking
            ItemStack itemStack = REGULAR_CARROT.clone();
            itemStack.editMeta((meta) -> meta.displayName(Component.text(Math.random())));

            // Spawn dropped item
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                location.getWorld().dropItem(location.toCenterLocation(), itemStack);
            });
        }
    }
}
