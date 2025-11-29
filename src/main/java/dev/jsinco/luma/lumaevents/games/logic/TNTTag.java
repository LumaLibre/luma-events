package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.games.tokenformula.TNTTagTokenFormula;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.EditMeta;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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


public final class TNTTag extends InventoryUnifiedMinigame {

    private static final int ROUND_DURATION = 90;
    private static final int MAX_ROUNDS = 3;
    private static final int TICK_INTERVAL = 20; // 1 second in ticks

    private final Map<UUID, TNTTagPlayer> tntTagPlayers;
    private final Location spawnPoint;
    private final Scoreboard<EventPlayer> scoreboard;
    private final TNTTagTokenFormula tokenFormula;

    private CountdownBossBar roundCountdownBar;
    private int roundCount = 0;

    public TNTTag(MinigameDefinition def) {
        super("TNT Tag", "Don't explode!", (ROUND_DURATION * MAX_ROUNDS * 2000) /* internally double the duration for ticks */, TICK_INTERVAL, false, true, true);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.tntTagPlayers = new HashMap<>();
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new TNTTagTokenFormula();
    }

    @Override
    protected void handleStart() {
        this.scoreboard.addScorers(this.participants);
        for (EventPlayer participant : this.participants) {
            this.swapRole(participant, () -> new Runner(participant, this.scoreboard));
            Player player = participant.getPlayer();
            if (player != null && player.getGameMode() != GameMode.SURVIVAL) {
                Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> player.setGameMode(GameMode.SURVIVAL));
            }
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
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            for (TNTTagPlayer player : this.tntTagPlayers.values()) {
                player.removeEffects(true);
            }
        });

        this.scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.stream().filter(
                    p -> p.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(this.spawnPoint));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.RED)
                    .title("<red><b>Game Over")
                    .seconds(15)
                    .callback(() -> this.boundingBox.getPlayers().forEach(player -> {
                        Location loc = this.getGameDropOffLocation();
                        if (loc != null) {
                            player.teleportAsync(loc);
                        }
                        Util.sendMsg(player, "This minigame has concluded.");
                    }))
                    .build()
                    .start();
        });
    }

    @Override
    protected void tokenHandler(EventPlayer eventPlayer) {
        int finalScore = this.scoreboard.getScore(eventPlayer);
        tokenFormula.giveTokens(eventPlayer, finalScore);
        eventPlayer.addPermanentScore(MinigameConstant.TNTTAG, finalScore);
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        super.handleParticipantJoin(player);
        player.teleportAsync(this.spawnPoint);
        return true;
    }

    @Override
    public boolean removeParticipant(EventPlayer player) {
        TNTTagPlayer tntTagPlayer = this.tntTagPlayers.get(player.getUuid());
        if (tntTagPlayer != null) {
            tntTagPlayer.removeEffects(false);
            this.tntTagPlayers.remove(player.getUuid());
        }

        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer != null && this.roundCountdownBar != null) {
            this.roundCountdownBar.getBossBar().removeViewer(bukkitPlayer);
        }
        return super.removeParticipant(player);
    }

    public <T extends TNTTagPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends TNTTagPlayer> newRoleSupplier) {
        TNTTagPlayer currentRole = tntTagPlayers.get(eventPlayer.getUuid());
        if (currentRole != null) {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> currentRole.removeEffects(false));
        }
        TNTTagPlayer newRole = newRoleSupplier.get();
        Bukkit.getScheduler().runTask(EventMain.getInstance(), newRole::addEffects);
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
        // for every 5 players, a new tagger is added
        int taggerCount = Math.max(1, this.getRunners().size() / 5 );
        Logging.log("DEBUG: " + this.getRunners().size() + " runners, " + taggerCount + " taggers.");
        for (int i = this.getTaggers().size(); i < taggerCount; i++) {
            Runner tagger = Util.getRandom(this.getRunners());
            this.swapRole(tagger.getWho(), () -> new Tagger(tagger.getWho()));
            this.sendAudienceMessage("<red>" + tagger.getWho().getName() + " is it!");
        }


        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            this.getTaggers().forEach(Tagger::addEffects);
            this.getRunners().forEach(Runner::addEffects);
        });

        // tp all to spawnPoint
        this.tntTagPlayers.values().stream()
                .map(TNTTagPlayer::getPlayer)
                .filter(Objects::nonNull)
                .forEach(this::teleportNearSpawnPoint);

        this.sendAudienceMessage("A new round has started!");
        this.roundCountdownBar = CountdownBossBar.builder()
                .seconds(ROUND_DURATION)
                .color(BossBar.Color.YELLOW)
                .title("<yellow><b>Round ends in: %ss <gray>| <yellow>Round: " + (this.roundCount + 1) + "/" + MAX_ROUNDS)
                .global(false)
                .audience(this.audience)
                .callback(() -> {
                    this.endRound();

                    if (this.getRunners().size() < 2) {
                        this.sendAudienceMessage("Not enough runners left to continue, ending early!");
                        this.stop();
                        return;
                    }

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
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> this.getRunners().forEach(runner -> runner.removeEffects(false)));

        this.sendAudienceMessage("The round has ended!");
    }

    public void teleportNearSpawnPoint(Player player) {
        Location spawnLocation = this.spawnPoint.clone();
        spawnLocation.add(RANDOM.nextDouble(6), 0, RANDOM.nextDouble(6));
        player.teleportAsync(spawnLocation);
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
            //event.setCancelled(true);
            return;
        }
        if (!(victim instanceof Runner runner)) {
            //event.setCancelled(true);
            return;
        }

        event.setDamage(0.0);
        this.swapRole(tagger.getWho(), () -> new Runner(tagger.getWho(), this.scoreboard));
        this.swapRole(runner.getWho(), () -> new Tagger(runner.getWho()));
        this.sendAudienceMessage("<red>" + bukkitVictim.getName() + " is now it!");
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isInBoundingBox(player)) {
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
        public abstract void removeEffects(boolean ending);
        public abstract void tick();

        @Nullable
        public Player getPlayer() {
            return this.who.getPlayer();
        }
    }


    public static class Runner extends TNTTagPlayer {

        private static final PotionEffect RUNNER_SPEED = new PotionEffect(PotionEffectType.SPEED, 350, 1, false, false, true);
        private static final PotionEffect RUNNER_GLOW = new PotionEffect(PotionEffectType.GLOWING, 350, 0, false, false, true);

        private final Scoreboard<EventPlayer> scoreboard;
        private int tickCounter; // Ticks gone without becoming a tagger

        public Runner(EventPlayer who, Scoreboard<EventPlayer> scoreboard) {
            super(who);
            this.scoreboard = scoreboard;
            this.tickCounter = 0;
        }

        @Override
        public void addEffects() {
            Player player = getPlayer();
            if (player != null) {
                player.clearActivePotionEffects();
                player.addPotionEffect(RUNNER_SPEED);
                player.addPotionEffect(RUNNER_GLOW);
                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(20);
                }
            }
        }

        @Override
        public void removeEffects(boolean ending) {
            Player player = getPlayer();
            if (player == null) {
                return;
            }
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.GLOWING);
        }

        @Override
        public void tick() {
            this.who.sendActionBar("<yellow>Run! Don't get tagged!");
            this.addEffects();

            this.tickCounter += TICK_INTERVAL / 20; // Convert TICK_INTERVAL to seconds

            if (this.tickCounter >= 30) {
                // If the runner has been running for 30 seconds without being tagged, they earn an opal
                this.tickCounter = 0; // Reset the counter
                this.scoreboard.addScore(this.who, 1); // Increment the score
            }
        }
    }


    public static class Tagger extends TNTTagPlayer {

        private static final PotionEffect TAGGER_SPEED = new PotionEffect(PotionEffectType.SPEED, 350, 3, false, false, true);
        private static final PotionEffect TAGGER_DOLPHIN = new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 350, 0, false, false, true);

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

            player.clearActivePotionEffects();
            player.addPotionEffect(TAGGER_SPEED);
            player.addPotionEffect(TAGGER_DOLPHIN);

            EntityEquipment equipment = player.getEquipment();
            equipment.setHelmet(TAGGER_HAT);
            equipment.setChestplate(TAGGER_CHESTPLATE);
            equipment.setLeggings(TAGGER_LEGGINGS);
            equipment.setBoots(TAGGER_BOOTS);
        }

        @Override
        public void removeEffects(boolean ending) {
            Player player = getPlayer();
            if (player == null || ending) {
                return;
            }

            EntityEquipment equipment = player.getEquipment();
            equipment.setHelmet(AIR);
            equipment.setChestplate(AIR);
            equipment.setLeggings(AIR);
            equipment.setBoots(AIR);

            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        }

        @Override
        public void tick() {
            this.who.sendActionBar("<red>Uh oh, you're it! Tag others to pass the <white>TNT</white>!");
            Player player = getPlayer();
            if (player == null) {
                return;
            }

            player.addPotionEffect(TAGGER_SPEED);
            player.addPotionEffect(TAGGER_DOLPHIN);

            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(20);
            }
        }
    }

    public static class Spectator extends TNTTagPlayer {

        private static final PotionEffect SPECTATOR_INVIS = new PotionEffect(PotionEffectType.INVISIBILITY, 350, 0);
        private static final PotionEffect SPECTATOR_SPEED = new PotionEffect(PotionEffectType.SPEED, 350, 4);

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
        public void removeEffects(boolean ending) {
            Player whoPlayer = getPlayer();
            if (whoPlayer == null) {
                return;
            }
            for (EventPlayer participant : this.participants) {
                Player player = participant.getPlayer();
                if (player == null || player == this.who) {
                    continue;
                }
                player.showPlayer(EventMain.getInstance(), whoPlayer);
            }
            whoPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
            whoPlayer.removePotionEffect(PotionEffectType.SPEED);
        }

        @Override
        public void tick() {
            Player player = getPlayer();
            if (player == null) {
                return;
            }
            player.addPotionEffect(SPECTATOR_INVIS);
            player.addPotionEffect(SPECTATOR_SPEED);

            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(20);
            }

            this.who.sendActionBar("<gold>You are spectating. Leave with: <white>/event quit");
        }
    }
}
