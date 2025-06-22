package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.EditMeta;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

// TODO: Test
public final class TNTTag extends InventoryUnifiedMinigame {

    private static final int ROUND_DURATION = 60;
    private static final int MAX_ROUNDS = 3;

    private final Map<UUID, TNTTagPlayer> tntTagPlayers = new HashMap<>();
    private final Location spawnPoint;

    private CountdownBossBar roundCountdownBar;
    private int roundCount = 0;

    public TNTTag(MinigameDefinition def) {
        super("TNT Tag", "Don't explode!", (ROUND_DURATION * MAX_ROUNDS * 2000) /* internally double the duration for ticks */, 20, false, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
    }

    @Override
    protected int minimumParticipants() {
        return 2;
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.participants) {
            this.swapRole(participant, () -> new Runner(participant));
        }
        this.startRound();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        this.tntTagPlayers.values().forEach(TNTTagPlayer::tick);


        if (this.isAllTaggersOffline()) {
            if (this.isAllParticipantsOffline()) {
                this.stop();
                return;
            }
            Runner runner = Util.getRandom(this.getRunners());
            this.swapRole(runner.getWho(), () -> new Tagger(runner.getWho()));
            this.sendAudienceMessage("A new Tagger has been assigned because all were offline.");
        }
    }

    @Override
    protected void handleStop() {
        if (this.roundCountdownBar != null) {
            this.roundCountdownBar.stop(false);
        }
        for (TNTTagPlayer player : this.tntTagPlayers.values()) {
            player.removeEffects(true);
        }
        this.tntTagPlayers.clear();
        this.sendAudienceMessage("TNT Tag has ended! (debug)");
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnPoint);
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer player) {
        TNTTagPlayer tntTagPlayer = this.tntTagPlayers.get(player.getUuid());
        if (tntTagPlayer != null) {
            tntTagPlayer.removeEffects(true);
            this.tntTagPlayers.remove(player.getUuid());
        }
        return super.removeParticipant(player);
    }

    public <T extends TNTTagPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends TNTTagPlayer> newRoleSupplier) {
        TNTTagPlayer currentRole = tntTagPlayers.get(eventPlayer.getUuid());
        if (currentRole != null) {
            currentRole.removeEffects(false);
        }
        TNTTagPlayer newRole = newRoleSupplier.get();
        newRole.addEffects();
        tntTagPlayers.put(eventPlayer.getUuid(), newRole);
        return (T) newRole;
    }

    private List<Tagger> getTaggers() {
        return this.tntTagPlayers.values().stream()
                .filter(it -> it instanceof Tagger)
                .map(Tagger.class::cast)
                .toList();
    }

    private List<Runner> getRunners() {
        return this.tntTagPlayers.values().stream()
                .filter(it -> it instanceof Runner)
                .map(Runner.class::cast)
                .toList();
    }

    private boolean isAllParticipantsOffline() {
        return this.participants.stream()
                .map(EventPlayer::getPlayer)
                .noneMatch(Objects::nonNull);
    }

    private boolean isAllTaggersOffline() {
        return this.getTaggers().stream()
                .map(TNTTagPlayer::getPlayer)
                .noneMatch(Objects::nonNull);
    }

    @Nullable
    private TNTTagPlayer getTntTagPlayer(Player player) {
        if (player == null) {
            return null;
        }
        return this.tntTagPlayers.get(player.getUniqueId());
    }


    public void startRound() {
        Runner initialTagger = Util.getRandom(this.getRunners());
        this.swapRole(initialTagger.getWho(), () -> new Tagger(initialTagger.getWho()));

        this.getTaggers().forEach(Tagger::addEffects);
        this.getRunners().forEach(Runner::addEffects);

        this.sendAudienceMessage("A new round has started!");
        this.roundCountdownBar = CountdownBossBar.builder()
                .seconds(ROUND_DURATION)
                .color(BossBar.Color.YELLOW)
                .title("<yellow><b>Round ends in: %s <gray>| <yellow>Round: " + (this.roundCount + 1) + "/" + MAX_ROUNDS)
                .global(false)
                .audience(this.audience)
                .callback(() -> {
                    this.endRound();
                    this.roundCount++;
                    if (this.roundCount < MAX_ROUNDS) {
                        this.startRound();
                    } else {
                        this.stop();
                    }
                })
                .build();
        this.roundCountdownBar.start();
    }

    public void endRound() {
        this.getTaggers().forEach(tagger -> {
            this.swapRole(tagger.getWho(), () -> new Spectator(tagger.getWho(), this.participants));
        });
        this.getRunners().forEach(runner -> runner.removeEffects(false));

        this.sendAudienceMessage("The round has ended!");
    }

    @EventHandler
    public void onPlayerHitPlayer(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getDamager() instanceof Player bukkitDamager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player bukkitVictim)) {
            return;
        }

        TNTTagPlayer damager = this.getTntTagPlayer(bukkitDamager);
        TNTTagPlayer victim = this.getTntTagPlayer(bukkitVictim);

        // check the legality of the hit
        if (damager == null || victim == null || !isInBoundingBox(bukkitDamager, bukkitVictim)) {
            return;
        }

        if (!(damager instanceof Tagger tagger)) {
            event.setCancelled(true);
            return;
        }
        if (!(victim instanceof Runner runner)) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(0.0);
        this.swapRole(tagger.getWho(), () -> new Runner(tagger.getWho()));
        this.swapRole(runner.getWho(), () -> new Tagger(runner.getWho()));
        this.sendAudienceMessage(tagger.getWho() + " has tagged " + runner.getWho() + "!");
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player bukkitPlayer)) {
            return;
        }

        TNTTagPlayer victim = this.getTntTagPlayer(bukkitPlayer);
        if (victim != null && isInBoundingBox(bukkitPlayer)) {
            event.setDamage(0.0);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();

        TNTTagPlayer victim = this.getTntTagPlayer(event.getPlayer());
        if (victim != null && isInBoundingBox(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @Getter
    @Setter
    public abstract static class TNTTagPlayer {

        protected final EventPlayer who;

        public TNTTagPlayer(EventPlayer who) {
            this.who = who;
        }

        public abstract void addEffects();
        public abstract void removeEffects(boolean gameOver);
        public abstract void tick();

        @Nullable
        public Player getPlayer() {
            if (who == null) {
                throw new IllegalStateException("Tagger is not set.");
            }
            return who.getPlayer();
        }

    }


    public static class Runner extends TNTTagPlayer {

        public Runner(EventPlayer who) {
            super(who);
        }

        @Override
        public void addEffects() {
            Player player = getPlayer();
            if (player == null) {
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 350, 1));
        }

        @Override
        public void removeEffects(boolean gameOver) {
            Player player = getPlayer();
            if (player == null) {
                return;
            }
            player.removePotionEffect(PotionEffectType.SPEED);
        }

        @Override
        public void tick() {
            this.who.sendActionBar("<yellow>Run! Don't get tagged!");
        }
    }


    public static class Tagger extends TNTTagPlayer {
        private static final EditMeta editMeta = meta -> {
            LeatherArmorMeta armorMeta = (LeatherArmorMeta) meta;
            armorMeta.setColor(Color.RED);
            armorMeta.addEnchant(Enchantment.UNBREAKING, 10, true);
        };

        private static final ItemStack TAGGER_HAT = Util.createBasicItem(Material.TNT, true);
        private static final ItemStack TAGGER_CHESTPLATE = Util.createItem(Material.LEATHER_CHESTPLATE, editMeta);
        private static final ItemStack TAGGER_LEGGINGS = Util.createItem(Material.LEATHER_LEGGINGS, editMeta);
        private static final ItemStack TAGGER_BOOTS = Util.createItem(Material.LEATHER_BOOTS, editMeta);
        private static final ItemStack AIR = new ItemStack(Material.AIR);


        public Tagger(EventPlayer initialTagger) {
            super(initialTagger);
        }

        @Override
        public void addEffects() {
            Player player = getPlayer();
            if (player == null) {
                return;
            }

            EntityEquipment equipment = player.getEquipment();
            equipment.setHelmet(TAGGER_HAT);
            equipment.setChestplate(TAGGER_CHESTPLATE);
            equipment.setLeggings(TAGGER_LEGGINGS);
            equipment.setBoots(TAGGER_BOOTS);
        }

        @Override
        public void removeEffects(boolean gameOver) {
            if (gameOver) {
                return;
            }
            Player player = getPlayer();
            if (player == null) {
                return;
            }

            EntityEquipment equipment = player.getEquipment();
            equipment.setHelmet(AIR);
            equipment.setChestplate(AIR);
            equipment.setLeggings(AIR);
            equipment.setBoots(AIR);
        }

        @Override
        public void tick() {
            this.who.sendMessage("<gold>Uh oh! You are it! Tag others to pass the TNT!");
        }
    }

    public static class Spectator extends TNTTagPlayer {

        private final List<EventPlayer> participants;

        public Spectator(EventPlayer who, List<EventPlayer> participants) {
            super(who);
            this.participants = participants;
        }

        @Override
        public void addEffects() {
            Player whoPlayer = getPlayer();
            if (whoPlayer == null) {
                return;
            }
            for (EventPlayer participant : participants) {
                Player player = participant.getPlayer();
                if (player == null || player == this.who) {
                    continue;
                }
                player.hidePlayer(EventMain.getInstance(), whoPlayer);
            }
            whoPlayer.getWorld().playSound(whoPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 6.7f);
        }

        @Override
        public void removeEffects(boolean gameOver) {
            Player whoPlayer = getPlayer();
            if (whoPlayer == null) {
                return;
            }
            for (EventPlayer participant : participants) {
                Player player = participant.getPlayer();
                if (player == null || player == this.who) {
                    continue;
                }
                player.showPlayer(EventMain.getInstance(), whoPlayer);
            }
            whoPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        @Override
        public void tick() {
            Player player = getPlayer();
            if (player == null) {
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 350, 0));
            this.who.sendActionBar("<red>You are spectating. Leave with: <white>/event quit");
        }
    }
}
