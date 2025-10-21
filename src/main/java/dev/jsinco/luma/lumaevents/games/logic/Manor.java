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
import dev.jsinco.luma.lumaitems.LumaItems;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class Manor extends InventoryUnifiedMinigame {

    private static final int TICK_INTERVAL = 2;

    private final Location spawnLocation;
    private final Location startLocation;

    private final ManorPlayerMap manorPlayers;
    private final BossBar bossBar;
    private final Scoreboard<EventPlayer> scoreboard;
    private final ManorTokenFormula tokenFormula;

    private CountdownBossBar countdownBossBar;


    public Manor(ManorMinigameDefinition def) {
        super("Manor", "Don't get caught!", 130000, TICK_INTERVAL, false, true, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnLocation = def.getSpawnLocation();
        this.startLocation = def.getStartLocation();

        this.manorPlayers = new ManorPlayerMap();
        this.bossBar = BossBar.bossBar(
                Util.color("<b>Remaining Players: " + (this.getParticipants().size() - 1)),
                1.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new ManorTokenFormula();
    }

    @Override
    protected int minimumParticipants() {
        return 2;
    }

    @Override
    protected void handleTokens() {
        for (ManorPlayer manorPlayer : this.manorPlayers) {
            EventPlayer eventPlayer = manorPlayer.getEventPlayer();
            int finalScore = this.scoreboard.getScore(eventPlayer);

            this.tokenFormula.giveTokens(eventPlayer, finalScore);
            eventPlayer.addPermanentScore(MinigameConstant.MANOR, finalScore);
        }
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        ManorPlayer manorPlayer = this.manorPlayers.remove(participant.getUuid());
        manorPlayer.onRemove();
        return super.removeParticipant(participant);
    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.getParticipants()) {
            this.manorPlayers.add(new Runner(participant, this));
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
                .seconds(10)
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
        victim.onAttacked(event, attacker);
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player victim)) return;
        if (this.manorPlayers.containsKey(victim.getUniqueId()) && event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
            event.setCancelled(true);
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

        protected static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, true, true);
        protected static final PotionEffect BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false, true);
        protected static final PotionEffect GLOWING = new PotionEffect(PotionEffectType.GLOWING, 45, 0, false, false, true);

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

        protected boolean isWithinDistance(ManorPlayer manorPlayer, double distance) {
            Player me = this.eventPlayer.getPlayer();
            Player you = manorPlayer.eventPlayer.getPlayer();
            if (me != null && you != null) {
                return me.getLocation().distanceSquared(you.getLocation()) <= distance * distance;
            }
            return false;
        }

        protected Double distanceTo(ManorPlayer manorPlayer) {
            Player me = this.eventPlayer.getPlayer();
            Player you = manorPlayer.eventPlayer.getPlayer();
            if (me != null && you != null) {
                return me.getLocation().distance(you.getLocation());
            }
            return null;
        }

        protected <T extends ManorPlayer> void hideFromOthers(Iterable<T> players) {
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

        protected <T extends ManorPlayer> void showToOthers(Iterable<T> players) {
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

        private final List<BukkitTask> revealTasks = new ArrayList<>();

        public Hunter(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            this.hideFromOthers(context.manorPlayers.getRunners());
        }

        @Override
        public void onTick(long timeLeft) {
            this.revealTasks.removeIf(BukkitTask::isCancelled);
            this.broadcastHeartbeatSound();

            this.showToOthers(this.context.manorPlayers.getSpectators());

            for (Runner runner : this.context.manorPlayers.getRunners()) {
                if (this.isWithinDistance(runner, 3.0)) {
                    runner.revealLocation();
                    this.revealLocation(runner, 20);
                }
            }

            this.eventPlayer.sendActionBar("<red>Catch <b>everyone</b> to win. <gray>Time Left: %ds".formatted(Util.millisToSecs(timeLeft)));
            this.eventPlayer.operatePlayer(player -> {
                player.addPotionEffect(INVISIBILITY);
            });
        }

        @Override
        public void onRemove() {
            for (BukkitTask task : this.revealTasks) {
                task.cancel();
            }
            this.revealTasks.clear();
            this.showToOthers(this.context.manorPlayers);

            this.eventPlayer.operatePlayer(player -> {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            });
            this.eventPlayer.removeBossBar(this.context.bossBar);
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

        private void revealLocation(Runner runner, long ticks) {
            this.showToOthers(List.of(runner));
            BukkitTask task = Executors.delayedSync(ticks, () -> {
                this.hideFromOthers(List.of(runner));
            });
            this.revealTasks.add(task);
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
                final int maxFreezeTicks = runnerPlayer.getMaxFreezeTicks();


                double proximity = Math.max(0, 1 - (distance / 30.0));
                long delay = (long) (40L - (proximity * 30L)); // 40 ticks (2s) → 10 ticks (0.5s)
                float volume = (float) (0.4 + proximity * 1.6); // 0.4 to 2.0
                float pitch = (float) (0.8 + proximity * 0.4);  // slight pitch rise near hunter
                int freezeTicks = (int) (proximity * maxFreezeTicks); // TODO

                runner.scheduleHeartbeat(delay, volume, pitch, freezeTicks);
            }
        }


    }

    private static class Runner extends ManorPlayer {

        private static final ItemStack BOOTS = Util.editMeta(ItemStack.of(Material.LEATHER_BOOTS), meta -> {
            NamespacedKey key = new NamespacedKey(LumaItems.getInstance(), "manor_runner_boots");
            meta.addAttributeModifier(Attribute.SCALE, new AttributeModifier(key, -0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
        });
        private static final int SCORE_REQUIREMENT = 40;

        private BukkitTask heartbeatTask;
        private int timeElapsed;

        public Runner(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);
            this.addBoots();
        }

        @Override
        public void onTick(long timeLeft) {
            this.eventPlayer.sendActionBar("<light_purple>Do not get <b>caught</b>. <gray>Time Left: %ds".formatted(Util.millisToSecs(timeLeft)));
            this.eventPlayer.operatePlayer(player -> {
                player.addPotionEffect(BLINDNESS);
            });
            this.eventPlayer.removeBossBar(this.context.bossBar);

            if (timeElapsed >= SCORE_REQUIREMENT) {
                this.context.scoreboard.addScore(this.eventPlayer, 1);
                timeElapsed = 0;
            } else {
                timeElapsed += TICK_INTERVAL;
            }
        }

        @Override
        public void onDeath(PlayerDeathEvent event, ManorPlayer attacker) {
            event.setCancelled(true);

            this.context.manorPlayers.swapRole(this.eventPlayer, () -> new Spectator(this.eventPlayer, this.context));
            this.eventPlayer.teleportAsync(this.context.startLocation);


            this.context.sendAudienceMessage(this.eventPlayer.getName() + " was caught by " + attacker.getEventPlayer().getName() + "!");
            // Add 15 secs for each caught runner
            this.context.setDuration(this.context.getDuration() + Util.secsToMillis(15));
            this.context.sendAudienceMessage("15 seconds have been added to the game time.");

            int current = this.context.manorPlayers.getRunners().size();
            int total = this.context.getParticipants().size() - 1; // exclude hunter
            float progress = 1.0f - ((float) current / total);

            this.context.bossBar.progress(progress);
            this.context.bossBar.name(Util.color("<b>Remaining Players: " + current));
        }

        @Override
        public void onRemove() {
            this.removeBoots();
            this.stopHeartbeatTask();
        }

        @Override
        public void onAttacked(EntityDamageByEntityEvent event, ManorPlayer attacker) {
            if (!(attacker instanceof Hunter)) {
                event.setCancelled(true);
                attacker.sendMessage("You cannot attack this player.");
            }

            event.setDamage(30.0); // Ensure instant kill when attacked by hunter
        }

        public void revealLocation() {
            this.eventPlayer.sendActionBar("<b><red>YOUR LOCATION HAS BEEN REVEALED");
            Player player = this.eventPlayer.getPlayer();
            if (player != null) {
                player.addPotionEffect(GLOWING);
            }
        }

        public void scheduleHeartbeat(long delay, float volume, float pitch, int freezeTicks) {
            this.stopHeartbeatTask();

            this.heartbeatTask = Executors.repeatingSync(delay, () -> {
                Player player = this.eventPlayer.getPlayer();
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                    player.setFreezeTicks(freezeTicks);
                }
            });
        }

        public void stopHeartbeatTask() {
            if (this.heartbeatTask != null) {
                this.heartbeatTask.cancel();
                this.heartbeatTask = null;
            }
        }

        private void addBoots() {
            this.eventPlayer.operatePlayer(player -> player.getInventory().setBoots(BOOTS));
        }

        private void removeBoots() {
            this.eventPlayer.operatePlayer(player -> player.getInventory().setBoots(null));
        }
    }


    private static class Spectator extends ManorPlayer {

        public Spectator(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            List<ManorPlayer> others = new ArrayList<>(context.manorPlayers.getRunners());
            others.add(context.manorPlayers.getHunter());
            this.hideFromOthers(others);
        }

        @Override
        public void onTick(long timeLeft) {
            this.eventPlayer.sendActionBar("<yellow>You're spectating, quit with <b>/event quit</b>. <gray>(%ds left)".formatted(Util.millisToSecs(timeLeft)));
            this.eventPlayer.operatePlayer(player -> {
                player.addPotionEffect(INVISIBILITY);
            });
        }

        @Override
        public void onRemove() {
            this.showToOthers(this.context.manorPlayers);
            this.eventPlayer.operatePlayer(player -> {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            });
            this.eventPlayer.removeBossBar(this.context.bossBar);
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
                currentRole.onRemove();
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
