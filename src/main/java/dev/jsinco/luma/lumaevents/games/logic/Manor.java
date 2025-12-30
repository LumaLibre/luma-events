package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.configurable.sectors.ManorMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.games.tokenformula.ManorTokenFormula;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.lumas.lumaitems.LumaItems;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;


// TODO: Tokens, invisibility for spectators, gameplay loop improvements
public final class Manor extends InventoryUnifiedMinigame {

    private static final int TICK_INTERVAL = 2;
    public static final NamespacedKey key = new NamespacedKey(LumaItems.getInstance(), "manor_runner_boots");

    private final Location spawnLocation;
    private final Location startLocation;

    private final ManorPlayerMap manorPlayers;
    private final Scoreboard<EventPlayer> scoreboard;
    private final ManorTokenFormula tokenFormula;

    private BossBar bossBar;
    private CountdownBossBar countdownBossBar;


    public Manor(ManorMinigameDefinition def) {
        super("Manor", "Don't get caught!", 135000, TICK_INTERVAL, false, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();

        this.manorPlayers = new ManorPlayerMap();
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new ManorTokenFormula();
    }


    @Override
    protected void tokenHandler(EventPlayer eventPlayer) {
        int finalScore = this.scoreboard.getScore(eventPlayer);

        this.tokenFormula.giveTokens(eventPlayer, finalScore);
        eventPlayer.addPermanentScore(MinigameConstant.MANOR, finalScore);
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        ManorPlayer manorPlayer = this.manorPlayers.remove(participant.getUuid());
        if (manorPlayer != null) {
            manorPlayer.onRemove();
        }
        return super.removeParticipant(participant);
    }

    @Override
    protected void handleStart() {
        this.bossBar = BossBar.bossBar(
                Util.color("<b>Remaining Players: " + (this.getParticipants().size() - 1)),
                1.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );

        for (EventPlayer participant : this.getParticipants()) {
            this.manorPlayers.add(new Runner(participant, this));
            participant.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    Executors.runSync(() -> player.setGameMode(GameMode.SURVIVAL));
                }
            });
        }
        // Assign a random hunter
        EventPlayer initialHunter = Util.getRandom(this.manorPlayers.getRunners()).getEventPlayer();
        Hunter hunter = this.manorPlayers.swapRole(initialHunter, () -> new Hunter(initialHunter, this));
        hunter.sendMessage("You haven't spawned in yet, get ready to catch everyone!");

        this.manorPlayers.getRunners().forEach(runner -> {
            runner.getEventPlayer().teleportAsync(this.startLocation);
            runner.sendMessage("The hunter hasn't spawned in yet, hide!");
        });

        this.countdownBossBar = CountdownBossBar.builder()
                .title("<red><b>Hunter spawns in: %s")
                .seconds(15)
                .audience(this.audience)
                .color(BossBar.Color.RED)
                .callback(() -> {
                    this.ensureHunterAssigned();
                    Hunter newHunter = this.manorPlayers.getHunter();
                    if (newHunter == null) {
                        this.sendAudienceMessage("Error assigning hunter. Ending game...");
                        this.stop();
                        return;
                    }
                    newHunter.getEventPlayer().teleportAsync(this.startLocation);
                    newHunter.sendMessage("<red>Catch <b>everyone</b> to win.");

                    this.manorPlayers.getRunners().forEach(runner -> {
                        runner.sendMessage("<light_purple>The hunter has spawned. Do not get <b>caught</b>.");
                    });

                    this.manorPlayers.forEach(manorPlayer -> {
                        manorPlayer.getEventPlayer().addBossBar(this.bossBar);
                    });
                })
                .build()
                .start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.ensureHunterAssigned()) return;

        this.manorPlayers.forEach(manorPlayer -> {
            manorPlayer.onTick(timeLeft);
            manorPlayer.getEventPlayer().operatePlayer(player -> {
                player.setSaturation(10);
                player.setFoodLevel(20);
            });
        });
    }

    @Override
    protected void handleStop() {
        this.countdownBossBar.stop(false);

        this.manorPlayers.forEach(ManorPlayer::onRemove);
        this.sendAudienceMessage("This minigame has concluded.");

        this.scoreboard.handleGameEnd(this.audience, () -> {
            this.participants.forEach(eventPlayer -> eventPlayer.teleportAsync(this.spawnLocation));

            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<blue><b>Game Over")
                    .seconds(15)
                    .callback(() -> {
                        this.participants.forEach(eventPlayer -> {
                            eventPlayer.teleportAsync(this.getGameDropOffLocation());
                            eventPlayer.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();

        if (!(event.getDamageSource().getCausingEntity() instanceof Player attackerPlayer)) return;
        Player victimPlayer = event.getPlayer();

        ManorPlayer attacker = this.manorPlayers.get(attackerPlayer.getUniqueId());
        ManorPlayer victim = this.manorPlayers.get(victimPlayer.getUniqueId());

        if (victim == null || attacker == null) return;
        victim.onDeath(event, attacker);
    }

    @EventHandler
    public void onPlayerDamagedByPlayer(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();

        if (!(event.getEntity() instanceof Player victimPlayer)) return;
        if (!(event.getDamager() instanceof Player attackerPlayer)) return;

        ManorPlayer victim = this.manorPlayers.get(victimPlayer.getUniqueId());
        ManorPlayer attacker = this.manorPlayers.get(attackerPlayer.getUniqueId());
        if (victim == null || attacker == null) return;

        if (attacker instanceof Hunter hunter) {
            victim.onAttacked(event, hunter);
        } else {
            event.setCancelled(true);
            attacker.sendMessage("You cannot attack this player.");
        }
    }


    // TODO: Do something about hunter possibly being afk
    private boolean ensureHunterAssigned() {
        Hunter hunter = this.manorPlayers.getHunter();

        if (hunter != null && hunter.getEventPlayer().getPlayer() != null) {
            return true;
        }

        // hunter left game, we need to assign a new one
        if (this.manorPlayers.size() < 2) {
            this.sendAudienceMessage("Not enough players to continue the game. Ending...");
            this.stop();
            return false; // Not enough players to continue
        }

        if (hunter != null) {
            this.manorPlayers.removeByValue(hunter);
        }

        // TODO: Maybe use a spectator?
        Runner newHunter = Util.getRandom(this.manorPlayers.getRunners());
        this.manorPlayers.swapRole(Runner.class, newHunter, () -> new Hunter(newHunter.getEventPlayer(), this));
        this.sendAudienceMessage("The current hunter has disconnected. A new hunter has been assigned.");
        return true;
    }



    @Getter
    private abstract static class ManorPlayer {

        protected static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, false, true, true);
        protected static final PotionEffect DARKNESS = new PotionEffect(PotionEffectType.DARKNESS, 600, 0, false, false, true);
        protected static final PotionEffect GLOWING = new PotionEffect(PotionEffectType.GLOWING, 600, 0, false, false, true);
        protected static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 600, 0, false, false, true);
        protected static final PotionEffect NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, false, false, true);

        protected final EventPlayer eventPlayer;
        protected final Manor context;

        public ManorPlayer(EventPlayer eventPlayer, Manor context) {
            this.eventPlayer = eventPlayer;
            this.context = context;
        }

        public UUID getUUID() {
            return this.eventPlayer.getUuid();
        }

        public void sendMessage(String message) {
            this.eventPlayer.sendMessage(message);
        }

        protected Double distanceTo(ManorPlayer manorPlayer) {
            Player me = this.eventPlayer.getPlayer();
            Player you = manorPlayer.eventPlayer.getPlayer();
            if (me != null && you != null) {
                return me.getLocation().distance(you.getLocation());
            }
            return null;
        }

        protected void hideFromOthers(ManorPlayer... players) {
            hideFromOthers(List.of(players));
        }

        protected void showToOthers(ManorPlayer... players) {
            showToOthers(List.of(players));
        }


        protected <T extends ManorPlayer> void hideFromOthers(Iterable<T> players) {
            if (!Bukkit.isPrimaryThread()) {
                Executors.sync(() -> hideIntl(players));
            } else {
                hideIntl(players);
            }
        }

        protected <T extends ManorPlayer> void showToOthers(Iterable<T> players) {
            if (!Bukkit.isPrimaryThread()) {
                Executors.sync(() -> showIntl(players));
            } else {
                showIntl(players);
            }
        }

        private <T extends ManorPlayer> void hideIntl(Iterable<T> players) {
            Player me = this.eventPlayer.getPlayer();
            if (me == null) return;

            for (ManorPlayer other : players) {
                if (other == this) continue;
                Player otherPlayer = other.eventPlayer.getPlayer();
                if (otherPlayer != null) {
                    otherPlayer.hidePlayer(LumaItems.getInstance(), me);
                }
            }
        }

        private <T extends ManorPlayer> void showIntl(Iterable<T> players) {
            Player me = this.eventPlayer.getPlayer();
            if (me == null) return;

            for (ManorPlayer other : players) {
                if (other == this) continue;
                Player otherPlayer = other.eventPlayer.getPlayer();
                if (otherPlayer != null) {
                    otherPlayer.showPlayer(LumaItems.getInstance(), me);
                }
            }
        }

        public abstract void onTick(long timeLeft);
        public abstract void onRemove();
        public abstract void onAttacked(EntityDamageByEntityEvent event, ManorPlayer attacker);
        public abstract void onDeath(PlayerDeathEvent event, ManorPlayer attacker);
    }

    private static class Hunter extends ManorPlayer {

        private static final int POTION_EFFECTS_TICK_SPEED = 100;
        private int potionEffectTicksElapsed = POTION_EFFECTS_TICK_SPEED;

        public Hunter(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            this.hideFromOthers(context.manorPlayers.getRunners());
        }

        @Override
        public void onTick(long timeLeft) {
            this.broadcastHeartbeatSound();

            this.showToOthers(this.context.manorPlayers.getSpectators());

            Player hunterPlayer = this.eventPlayer.getPlayer();
            if (hunterPlayer == null) return;

            for (Runner runner : this.context.manorPlayers.getRunners()) {
                Player runnerPlayer = runner.eventPlayer.getPlayer();
                if (runnerPlayer == null) continue;
                Double distance = this.distanceTo(runner);
                if (distance == null) continue;

                if (distance <= 7.0) {
                    this.showToOthers(runner);
                } else {
                    this.hideFromOthers(runner);
                }

                if (distance >= 15.0 || distance <= 6.0) {
                    runner.showToOthers(this);
                } else {
                    runner.hideFromOthers(this);
                }

            }

            if (++this.potionEffectTicksElapsed >= POTION_EFFECTS_TICK_SPEED) {
                this.potionEffectTicksElapsed = 0;
                this.eventPlayer.operatePlayer(player -> {
                    player.addPotionEffect(GLOWING);
                    player.addPotionEffect(NIGHT_VISION);
                    player.addPotionEffect(SPEED);
                });
            }

            this.eventPlayer.sendActionBar("<red>Catch <b>everyone</b> to win. <gray>Time Left: %ds".formatted(Util.millisToSecs(timeLeft)));
        }

        @Override
        public void onRemove() {
            this.showToOthers(this.context.manorPlayers);
            Executors.runSync(() -> {
                this.eventPlayer.removeBossBar(this.context.bossBar);
                this.eventPlayer.operatePlayer(player -> {
                    player.removePotionEffect(PotionEffectType.GLOWING);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.removePotionEffect(PotionEffectType.SPEED);
                });
            });
        }

        @Override
        public void onAttacked(EntityDamageByEntityEvent event, ManorPlayer attacker) {
            if (!(attacker instanceof Runner)) {
                event.setCancelled(true);
                attacker.sendMessage("You cannot attack this player.");
            }
            // TODO
        }

        @Override
        public void onDeath(PlayerDeathEvent event, ManorPlayer attacker) {
            throw new UnsupportedOperationException("Not yet implemented"); // TODO
        }

        private void broadcastHeartbeatSound() {
            // Broadcast heartbeat sound to all runners
            // increase volume and frequency of heartbeat the closer they are to the hunter
            Player hunterPlayer = this.eventPlayer.getPlayer();
            if (hunterPlayer == null) return;

            for (Runner runner : this.context.manorPlayers.getRunners()) {
                Player runnerPlayer = runner.eventPlayer.getPlayer();
                Double distance = this.distanceTo(runner);


                if (runnerPlayer == null || distance == null || distance > 30.0) continue;


                double proximity = Math.max(0, 1 - (distance / 30.0));
                int delay = (int) (20 - (proximity * 14));
                float volume = (float) (0.4 + proximity * 1.6); // 0.4 to 2.0
                float pitch = (float) (0.8 + proximity * 0.4);  // slight pitch rise near hunter


                runner.updateHeartbeatTickSpeed(delay);
                runner.playHeartbeatIfAble(volume, pitch);
            }
        }


    }

    private static class Runner extends ManorPlayer {

        private static final ItemStack BOOTS = Util.editMeta(ItemStack.of(Material.LEATHER_BOOTS), meta -> {
            meta.addAttributeModifier(Attribute.SCALE, new AttributeModifier(key, -0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
        });
        private static final int SCORE_REQUIREMENT = 800; // 40s per token
        private static final int POTION_EFFECTS_TICK_SPEED = 100;


        private int tokenTicksElapsed;

        private int heartbeatTicksElapsed;
        private int heartbeatTickSpeed;

        private int potionEffectTicksElapsed = POTION_EFFECTS_TICK_SPEED;

        public Runner(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);
            this.addBoots();
        }

        @Override
        public void onTick(long timeLeft) {
            this.eventPlayer.sendActionBar("<light_purple>Do not get <b>caught</b>. <gray>Time Left: %ds".formatted(Util.millisToSecs(timeLeft)));
            if (++this.potionEffectTicksElapsed >= POTION_EFFECTS_TICK_SPEED) {
                this.potionEffectTicksElapsed = 0;
                this.eventPlayer.operatePlayer(player -> {
                    player.addPotionEffect(DARKNESS);
                    player.addPotionEffect(GLOWING);
                    player.addPotionEffect(NIGHT_VISION);
                });
            }

            if (tokenTicksElapsed >= SCORE_REQUIREMENT) {
                this.context.scoreboard.addScore(this.eventPlayer, 1);
                tokenTicksElapsed = 0;
            } else {
                tokenTicksElapsed += TICK_INTERVAL;
            }
        }

        @Override
        public void onDeath(PlayerDeathEvent event, ManorPlayer attacker) {
            event.setCancelled(true);

            this.context.manorPlayers.swapRole(this.eventPlayer, () -> new Spectator(this.eventPlayer, this.context));
            this.eventPlayer.teleportAsync(this.context.startLocation);


            this.context.sendAudienceMessage(this.eventPlayer.getName() + " was caught by " + attacker.getEventPlayer().getName() + "!");
            this.context.scoreboard.addScore(attacker.getEventPlayer(), 1); // Hunter gets 1 points per catch

            if (this.context.manorPlayers.getRunners().isEmpty()) {
                this.context.sendAudienceMessage("The hunter has caught all runners. The hunter wins...");
                this.context.stop();
                return;
            }

            // Add 15 secs for each caught runner
            this.context.setDuration(this.context.getDuration() + Util.secsToMillis(15));
            this.context.sendAudienceMessage("15 seconds have been added to the game time.");

            int current = this.context.manorPlayers.getRunners().size();
            int total = this.context.getParticipants().size() - 1; // exclude hunter
            float t = ((float) current / total);
            float progress = t == 0f ? 0f : 1.0f - t;

            this.context.bossBar.progress(progress);
            this.context.bossBar.name(Util.color("<b>Remaining Players: " + current));

        }

        @Override
        public void onRemove() {
            this.removeBoots();
            this.showToOthers(this.context.manorPlayers);
            this.eventPlayer.removeBossBar(this.context.bossBar);
            Executors.runSync(() -> {
                this.eventPlayer.operatePlayer(player -> {
                    player.removePotionEffect(PotionEffectType.DARKNESS);
                    player.removePotionEffect(PotionEffectType.GLOWING);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                });
            });
        }

        @Override
        public void onAttacked(EntityDamageByEntityEvent event, ManorPlayer attacker) {
            if (!(attacker instanceof Hunter)) {
                event.setCancelled(true);
                attacker.sendMessage("You cannot attack this player.");
                return;
            }

            event.setDamage(90.0); // Ensure instant kill when attacked by hunter
        }



        public boolean updateHeartbeatTickSpeed(int tickSpeed) {
            if (this.heartbeatTickSpeed != tickSpeed) {
                this.heartbeatTickSpeed = tickSpeed;
                return true;
            }
            return false;
        }

        public boolean canPlayHeartbeat() {
            return this.heartbeatTicksElapsed >= this.heartbeatTickSpeed;
        }

        public void playHeartbeatIfAble(float volume, float pitch) {
            if (this.canPlayHeartbeat()) {
                Player player = this.eventPlayer.getPlayer();
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                }
                this.heartbeatTicksElapsed = 0;
            } else {
                this.heartbeatTicksElapsed += TICK_INTERVAL;
            }
        }


        private void addBoots() {
            Player player = this.eventPlayer.getPlayer();
            if (player != null) {
                player.getInventory().setBoots(BOOTS);
            }
        }

        private void removeBoots() {
            Player player = this.eventPlayer.getPlayer();
            if (player != null) {
                player.getInventory().setBoots(null);
            }
        }
    }


    private static class Spectator extends ManorPlayer {

        private static final int POTION_EFFECTS_TICK_SPEED = 100;

        private int potionEffectTicksElapsed = POTION_EFFECTS_TICK_SPEED;

        public Spectator(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            List<ManorPlayer> others = new ArrayList<>(context.manorPlayers.getRunners());
            others.add(context.manorPlayers.getHunter());
            this.hideFromOthers(others);
        }

        @Override
        public void onTick(long timeLeft) {
            this.eventPlayer.sendActionBar("<yellow>You're spectating, quit with <b>/event quit</b>. <gray>(%ds left)".formatted(Util.millisToSecs(timeLeft)));
            if (++this.potionEffectTicksElapsed >= POTION_EFFECTS_TICK_SPEED) {
                this.potionEffectTicksElapsed = 0;
                this.eventPlayer.operatePlayer(player -> {
                    player.addPotionEffect(INVISIBILITY);
                });
            }
        }

        @Override
        public void onRemove() {
            this.showToOthers(this.context.manorPlayers);
            Executors.runSync(() -> {
                this.eventPlayer.operatePlayer(player -> {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                });
                this.eventPlayer.removeBossBar(this.context.bossBar);
            });
        }

        @Override
        public void onAttacked(EntityDamageByEntityEvent event, ManorPlayer attacker) {
            event.setCancelled(true);
        }

        @Override
        public void onDeath(PlayerDeathEvent event, ManorPlayer attacker) {
            event.setCancelled(true);
            this.eventPlayer.teleportAsync(this.context.spawnLocation);
        }
    }



    private static class ManorPlayerMap extends HashMap<UUID, ManorPlayer> implements Iterable<ManorPlayer> {


        @Nullable
        public ManorPlayer removeByValue(ManorPlayer manorPlayer) {
            return this.remove(manorPlayer.getUUID());
        }

        public ManorPlayer add(ManorPlayer manorPlayer) {
            return this.put(manorPlayer.getUUID(), manorPlayer);
        }

        @Nullable
        public Hunter getHunter() {
            Hunter hunter = null;
            for (ManorPlayer player : this.values()) {
                if (player instanceof Hunter h) {
                    if (hunter == null) {
                        hunter = h;
                    } else {
                        throw new IllegalStateException("Multiple hunters found in ManorPlayerList");
                    }
                }
            }
            return hunter;
        }

        public List<Runner> getRunners() {
            List<Runner> runners = new ArrayList<>();
            for (ManorPlayer player : this.values()) {
                if (player instanceof Runner r) {
                    runners.add(r);
                }
            }
            return runners;
        }

        public List<Spectator> getSpectators() {
            List<Spectator> spectators = new ArrayList<>();
            for (ManorPlayer player : this.values()) {
                if (player instanceof Spectator s) {
                    spectators.add(s);
                }
            }
            return spectators;
        }

        public <T extends ManorPlayer> T swapRole(Class<? extends ManorPlayer> ifRole, ManorPlayer manorPlayer, Supplier<? extends ManorPlayer> newRoleSupplier) {
            if (ifRole.isInstance(manorPlayer)) {
                return this.swapRole(manorPlayer.getEventPlayer(), newRoleSupplier);
            }
            return (T) manorPlayer;
        }

        public  <T extends ManorPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends ManorPlayer> newRoleSupplier) {
            ManorPlayer currentRole = this.get(eventPlayer.getUuid());
            if (currentRole != null) {
                Executors.runSync(() -> {
                    currentRole.onRemove();
                });
            }
            ManorPlayer newRole = newRoleSupplier.get();
            this.put(newRole.getUUID(), newRole);
            return (T) newRole;
        }

        @NotNull
        @Override
        public Iterator<ManorPlayer> iterator() {
            return this.values().iterator();
        }
    }

}
