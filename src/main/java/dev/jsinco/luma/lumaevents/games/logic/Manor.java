package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.LumaItems;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class Manor extends InventoryUnifiedMinigame {

    @Getter
    ManorPlayerMap manorPlayers = new ManorPlayerMap();


    public Manor(String name, String description, long duration, long tickInterval, boolean async) {
        super(name, description, duration, tickInterval, async);
    }

    @Override
    protected void handleTokens() {

    }

    @Override
    protected void handleStart() {
        if (this.getParticipants().size() < 2) {
            this.sendAudienceMessage("Not enough players to start the game.");
            this.stop();
            return;
        }

        for (EventPlayer participant : this.getParticipants()) {
            this.manorPlayers.add(new Runner(participant, this));
        }
        // Assign a random hunter
        Runner initialHunter = Util.getRandom(this.manorPlayers.getRunners());
        this.manorPlayers.swapRole(Runner.class, initialHunter, () -> new Hunter(initialHunter.getEventPlayer(), this));
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (!this.ensureHunterAssigned()) return;

        this.manorPlayers.values().forEach(ManorPlayer::onTick);
    }

    @Override
    protected void handleStop() {
        this.manorPlayers.values().forEach(ManorPlayer::onRemove);
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

        protected final EventPlayer eventPlayer;
        protected final Manor context;

        public ManorPlayer(EventPlayer eventPlayer, Manor context) {
            this.eventPlayer = eventPlayer;
            this.context = context;
        }

        public UUID getUUID() {
            return this.eventPlayer.getUuid();
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

        protected boolean hideFromOthers() {
            Player me = this.eventPlayer.getPlayer();
            if (me == null) return false;

            for (ManorPlayer other : this.context.manorPlayers.values()) {
                if (other == this) continue;
                Player otherPlayer = other.eventPlayer.getPlayer();
                if (otherPlayer != null) {
                    otherPlayer.hidePlayer(LumaItems.getInstance(), me);
                }
            }
            return true;
        }

        protected boolean showToOthers() {
            Player me = this.eventPlayer.getPlayer();
            if (me == null) return false;

            for (ManorPlayer other : this.context.manorPlayers.values()) {
                if (other == this) continue;
                Player otherPlayer = other.eventPlayer.getPlayer();
                if (otherPlayer != null) {
                    otherPlayer.showPlayer(LumaItems.getInstance(), me);
                }
            }
            return true;
        }

        public abstract void onTick();
        public abstract void onRemove();
    }

    private static class Hunter extends ManorPlayer {

        public Hunter(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            this.hideFromOthers();
        }

        @Override
        public void onTick() {
            this.broadcastHeartbeatSound();

            for (Runner runner : this.context.getManorPlayers().getRunners()) {
                if (this.isWithinDistance(runner, 3.0)) {
                    runner.revealLocation();
                }
            }
        }

        @Override
        public void onRemove() {
            this.showToOthers();
        }

        private void broadcastHeartbeatSound() {
            // Broadcast heartbeat sound to all runners
            // increase volume and frequency of heartbeat the closer they are to the hunter
            Player hunterPlayer = this.eventPlayer.getPlayer();
            if (hunterPlayer == null) return;

            for (Runner runner : this.context.getManorPlayers().getRunners()) {
                Player runnerPlayer = runner.eventPlayer.getPlayer();
                Double distance = this.distanceTo(runner);


                if (runnerPlayer == null || distance == null || distance > 30.0) continue;


                double proximity = Math.max(0, 1 - (distance / 30.0));
                long delay = (long) (40L - (proximity * 30L)); // 40 ticks (2s) → 10 ticks (0.5s)
                float volume = (float) (0.4 + proximity * 1.6); // 0.4 to 2.0
                float pitch = (float) (0.8 + proximity * 0.4);  // slight pitch rise near hunter

                runner.scheduleHeartbeat(delay, volume, pitch);
            }
        }


    }

    private static class Runner extends ManorPlayer {

        private BukkitTask heartbeatTask;

        public Runner(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);
        }

        @Override
        public void onTick() {

        }

        @Override
        public void onRemove() {

        }

        public void revealLocation() {
            this.eventPlayer.sendActionBar("<b><red>YOUR LOCATION HAS BEEN REVEALED");
            Player player = this.eventPlayer.getPlayer();
            if (player != null) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, true));
            }
        }

        public void scheduleHeartbeat(long delay, float volume, float pitch) {
            this.stopHeartbeatTask();

            this.heartbeatTask = Executors.repeatingSync(delay, () -> {
                Player player = this.eventPlayer.getPlayer();
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                }
            });
        }

        public void stopHeartbeatTask() {
            if (this.heartbeatTask != null) {
                this.heartbeatTask.cancel();
                this.heartbeatTask = null;
            }
        }
    }


    private static class Spectator extends ManorPlayer {

        public Spectator(EventPlayer eventPlayer, Manor context) {
            super(eventPlayer, context);

            this.hideFromOthers();
        }

        @Override
        public void onTick() {

        }

        @Override
        public void onRemove() {
            this.showToOthers();
        }
    }



    private static class ManorPlayerMap extends HashMap<UUID, ManorPlayer> {


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

        private <T extends ManorPlayer> T swapRole(EventPlayer eventPlayer, Supplier<? extends ManorPlayer> newRoleSupplier) {
            ManorPlayer currentRole = this.get(eventPlayer.getUuid());
            if (currentRole != null) {
                currentRole.onRemove();
            }
            ManorPlayer newRole = newRoleSupplier.get();
            this.add(newRole);
            return (T) newRole;
        }
    }
}
