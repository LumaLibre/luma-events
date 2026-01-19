package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.sectors.PanelPartyMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.exceptions.MinigameException;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRole;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRoleMap;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.GenericStructure;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.obj.Scoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PanelParty extends InventoryUnifiedMinigame {

    private static final long DURATION = 600000L; // 10 mins, unlikely to reach
    private static final long TICK_INTERVAL = 1L;
    private static final String[] ELIMINATION_MESSAGES = {
            "<red>%s</red> fell to their doom.",
            "<aqua>%s</aqua> couldn't keep up.",
            "<green>%s</green> isn't great with colors...",
            "<yellow>%s</yellow> chose a bad block.",
            "<light_purple>%s</light_purple> was eliminated!",
            "<gold>%s</gold> became a spectator.",
    };

    private final Location spawnLocation;
    private final Location center;
    private final GenericStructure blankPanel;
    private final List<GenericStructure> panels;
    private final MinigameRoleMap<AbstractPanelPlayer> roleMap;
    private final Scoreboard<EventPlayer> scoreboard;
    private final int maxRounds;


    private int round;
    private @NotNull PanelPartyProcess currentProcess;

    public PanelParty(PanelPartyMinigameDefinition def) {
        super("Panels", "Stand on the correct color!", DURATION, TICK_INTERVAL, true, true, true, true);
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
        this.spawnLocation = def.getSpawnLocation();
        this.center = def.getCenter();
        this.blankPanel = new GenericStructure(this.center, def.getBlankPanelSchematic());
        this.panels = new ArrayList<>(def.getSchematics().stream()
                .map(fileName -> new GenericStructure(this.center, fileName))
                .toList());
        this.roleMap = new MinigameRoleMap<>(AbstractPanelPlayer::cleanup);
        this.scoreboard = new Scoreboard<>();
        this.maxRounds = panels.size();


        this.round = 0;
        this.currentProcess = new PanelPartyProcess(this, PanelDifficulty.fromRounds(this.round, this.maxRounds));

        Collections.shuffle(this.panels, RANDOM); // shuffle panels
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        // TODO: implement token handling
        participant.sendMessage("TODO: Panel Party Token Handler");
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant) {
        AbstractPanelPlayer role = this.roleMap.remove(participant.getUuid());
        if (role != null) {
            role.cleanup();
        }
        return super.removeParticipant(participant);
    }

    @Override
    protected void handleStart() {
        Location initialTeleportLocation = this.center.clone().add(RANDOM.nextDouble(0, 5), 2, RANDOM.nextDouble(0, 5));
        for (EventPlayer participant : this.getParticipants()) {
            PanelParticipant role = new PanelParticipant(this, participant);
            this.roleMap.put(role);
            participant.teleportAsync(initialTeleportLocation);
        }

        if (this.panels.isEmpty()) {
            this.stop();
            throw new MinigameException("No schematics available to start the game.");
        }

        Preconditions.checkNotNull(this.currentProcess, "Current process cannot be null during handleStart.");

        Executors.runSync(() -> {
            this.currentProcess.run(this.round);
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.shouldStop()) {
            this.stop();
            return;
        }

        this.roleMap.forEach(abstractPanelPlayer -> {
            abstractPanelPlayer.tick();
        });

        Preconditions.checkNotNull(this.currentProcess, "Current process cannot be null during onRunnable.");

        if (!this.currentProcess.isFinished()) {
            return;
        }


        this.round++;
        if (this.shouldStop()) {
            this.stop();
        } else {
            this.currentProcess = new PanelPartyProcess(this, PanelDifficulty.fromRounds(this.round, this.maxRounds));
            Executors.runSync(() -> {
                this.currentProcess.run(this.round);
            });

            for (PanelParticipant panelParticipant : this.roleMap.getMatching(PanelParticipant.class)) {
                this.scoreboard.addScore(panelParticipant.getEventPlayer(), 1); // Award 1 point for surviving the round
            }
        }
    }

    @Override
    protected void handleStop() {
        if (!this.currentProcess.isFinished()) {
            this.currentProcess.interrupt(false);
        }

        for (AbstractPanelPlayer role : this.roleMap) {
            role.teleportAsync(this.spawnLocation);
            role.cleanup();
        }

        Executors.runSync(() -> {
            this.blankPanel.paste();
        });

        this.scoreboard.handleGameEnd(this.getAudience(), () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
                    .seconds(10)
                    .callback(() -> {
                        this.participants.forEach(eventPlayer -> {
                            Location loc = this.getGameDropOffLocation();
                            if (loc != null) {
                                eventPlayer.teleportAsync(loc);
                            }
                            eventPlayer.sendMessage("This minigame has concluded.");
                        });
                    })
                    .build()
                    .start();
        });
    }

    public boolean shouldStop() {
        if (this.round >= this.maxRounds) {
            this.sendAudienceMessage("No more panels left to play!");
            return true;
        } else if (this.roleMap.getMatching(PanelParticipant.class).isEmpty()) {
            this.sendAudienceMessage("No players remaining!");
            return true;
        }
        return false;
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.ensureNotIllegal();
        Player player = event.getEntity();
        AbstractPanelPlayer role = this.roleMap.get(player.getUniqueId());
        if (role != null) {
            event.setCancelled(true);
            role.eliminate();
        }
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        this.ensureNotIllegal();
        Player player = event.getPlayer();
        AbstractPanelPlayer role = this.roleMap.get(player.getUniqueId());
        if (role != null) {
            event.setCancelled(true);
        }
    }


    private static abstract class AbstractPanelPlayer extends MinigameRole {
        protected final PanelParty context;

        protected AbstractPanelPlayer(PanelParty context, EventPlayer eventPlayer) {
            super(eventPlayer);
            this.context = context;
        }

        public abstract void tick();
        public abstract void cleanup();
        public abstract void eliminate();
    }

    public static class PanelParticipant extends AbstractPanelPlayer {

        private static final PotionEffect JUMP_BOOST = new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 2, true, false);
        private static final ItemStack AIR = ItemStack.of(Material.AIR);

        private final int eliminationYLevel;
        private boolean eliminated;

        public PanelParticipant(PanelParty context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
            this.eliminationYLevel = context.center.getBlockY() - 35;
            this.eliminated = false;
        }

        @Override
        public void tick() {
            this.eventPlayer.operatePlayer(player -> {
                player.addPotionEffect(JUMP_BOOST);
                player.setFoodLevel(20);
                if (player.getLocation().getY() <= this.eliminationYLevel) {
                    this.eliminate();
                }
            });
        }

        @Override
        public void cleanup() {
            this.eventPlayer.operatePlayer(player -> {
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            });
        }

        @Override
        public void eliminate() {
            if (this.eliminated) return;
            this.eliminated = true;

            this.setItemInHand(AIR);

            this.context.roleMap.swapRole(this, () -> new PanelSpectator(this.context, this.eventPlayer));

            Location location = this.context.center.clone().add(0, 10, 0);
            this.teleportAsync(location);

            this.eventPlayer.sendMessage("You have been eliminated and are now a spectator!");
            this.context.sendAudienceMessage(String.format(Util.getRandom(ELIMINATION_MESSAGES), this.eventPlayer.getName()));

            if (this.context.roleMap.getMatching(PanelParticipant.class).size() <= 1) {
                PanelParticipant winner = this.context.roleMap.getMatching(PanelParticipant.class).stream().findFirst().orElse(null);
                if (winner != null) {
                    this.context.sendAudienceMessage("<gold><b>" + winner.getEventPlayer().getName() + "</b> was the last player standing!");
                }
                this.context.stop();
            }
        }


        public void setItemInHand(ItemStack itemStack) {
            this.eventPlayer.operatePlayer(player -> {
                player.getInventory().setItemInMainHand(itemStack);
            });
        }
    }

    private static class PanelSpectator extends AbstractPanelPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, true, false);

        public PanelSpectator(PanelParty context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
            this.hide();
            this.eventPlayer.operatePlayer(player -> {
                player.setAllowFlight(true);
                player.setFlying(true);
            });
        }

        @Override
        public void tick() {
            this.eventPlayer.operatePlayer(player -> {
                if (!player.isFlying()) {
                    player.setFlying(true);
                }
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
                player.addPotionEffect(INVISIBILITY);
            });
        }

        @Override
        public void cleanup() {
            this.show();
            this.eventPlayer.operatePlayer(player -> {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            });
        }

        @Override
        public void eliminate() {
            Location location = this.context.center.clone().add(0, 10, 0);
            this.teleportAsync(location);
        }

        public void hide() {
            this.eventPlayer.operatePlayer(self -> {
                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther != null) {
                        bukkitOther.hidePlayer(EventMain.getInstance(), self);
                    }
                }
            });
        }

        public void show() {
            this.eventPlayer.operatePlayer(self -> {
                for (EventPlayer other : this.context.getParticipants()) {
                    Player bukkitOther = other.getPlayer();
                    if (bukkitOther != null) {
                        bukkitOther.showPlayer(EventMain.getInstance(), self);
                    }
                }
            });
        }

    }

    public static class PanelPartyProcess {

        private static final float ROUND_INTERVAL_SECONDS = 5;

        private final PanelParty context;
        private final PanelDifficulty difficulty;
        private final Set<Material> availableMaterials;
        private final AtomicBoolean finished; // TODO: could be volatile boolean instead

        private CountdownBossBar countdownBossBar;
        private CountdownBossBar intermissionBossBar;

        private Material chosenMaterial;


        public PanelPartyProcess(PanelParty context, PanelDifficulty difficulty) {
            this.context = context;
            this.difficulty = difficulty;
            this.availableMaterials = new HashSet<>();
            this.finished = new AtomicBoolean(false);
        }

        public boolean isFinished() {
            return this.finished.get();
        }

        public boolean run(int round) {
            // check if more rounds are available
            if (round + 1 > this.context.maxRounds) {
                return false;
            }

            // paste the next panel and count block data
            GenericStructure panel = this.context.panels.get(round);
            panel.paste((vector3i, blockData) -> {
                Material material = blockData.getMaterial();
                this.availableMaterials.add(material);
                return true;
            });

            this.chosenMaterial = Util.getRandom(this.availableMaterials);
            Preconditions.checkNotNull(this.chosenMaterial, "Chosen block data cannot be null.");


            // Teleport players to the center
            ItemStack itemStack = ItemStack.of(this.chosenMaterial);
            for (PanelParticipant panelParticipant : this.context.roleMap.getMatching(PanelParticipant.class)) {
                Location location = this.context.center.clone();
                panelParticipant.teleportAsync(location.add(RANDOM.nextDouble(0, 5), 2, RANDOM.nextDouble(0, 5)));
                panelParticipant.setItemInHand(itemStack);
            }


            this.countdownBossBar = CountdownBossBar.builder()
                    .seconds(this.difficulty.getSeconds())
                    .title(this.difficulty.formatted(this.panelName(panel.getLocalSchemPath()) + " <gray>(" + round + "/" + this.context.maxRounds + ")"))
                    .audience(this.context.getAudience())
                    .color(this.difficulty.getBossBarColor())
                    .callback(() -> {
                        Preconditions.checkNotNull(this.chosenMaterial, "Chosen block data cannot be null during countdown callback.");
                        // remove panel
                        Executors.sync(() -> {
                            panel.remove((vector3i, blockData) ->
                                    !blockData.getMaterial().equals(this.chosenMaterial)
                            );
                        });

                        // recursively start next round
                        this.intermissionBossBar = CountdownBossBar.builder()
                                .seconds(ROUND_INTERVAL_SECONDS)
                                .title(this.difficulty.formatted("Hold Your Positions!"))
                                .audience(this.context.getAudience())
                                .color(this.difficulty.getBossBarColor())
                                .callback(() -> {
                                    this.finished.set(true);
                                })
                                .build()
                                .start();
                    })
                    .build()
                    .start();

            return true;
        }


        public boolean interrupt(boolean callback) {
            if (this.isFinished()) {
                return false;
            }

            this.context.unsafe(() -> {
                if (this.countdownBossBar != null && !this.countdownBossBar.isCancelled()) {
                    this.countdownBossBar.stop(callback);
                }
            });
            this.context.unsafe(() -> {
                if (this.intermissionBossBar != null && !this.intermissionBossBar.isCancelled()) {
                    this.intermissionBossBar.stop(callback);
                }
            });
            this.finished.set(true);
            return true;
        }


        private String panelName(String localSchemPathName) {
            return "\"" + Util.formatMaterialName(localSchemPathName
                    .replace("and", "&")
                    .replace("panel_", "")
                    .replace(".schem", "")) +"\"";
        }
    }


    @Getter
    public enum PanelDifficulty {
        EASY(12, BossBar.Color.GREEN, "<green>"),
        MEDIUM(9, BossBar.Color.YELLOW, "<yellow>"),
        HARD(6, BossBar.Color.RED, "<dark_red>"),
        HARDER_THAN_HARD_I_GUESS(3, BossBar.Color.PURPLE, "<dark_purple>");

        private final int seconds;
        private final BossBar.Color bossBarColor;
        private final String textColorPrefix;

        PanelDifficulty(int seconds, BossBar.Color bossBarColor, String textColorPrefix) {
            this.seconds = seconds;
            this.bossBarColor = bossBarColor;
            this.textColorPrefix = textColorPrefix;
        }

        public String formatted(String message) {
            return this.textColorPrefix + "<b>" + message;
        }

        public static PanelDifficulty fromRounds(int currentRound, int maxRounds) {
            // Evenly distribute difficulties across rounds
            double ratio = (double) currentRound / maxRounds;
            if (ratio < 0.25) {
                return EASY;
            } else if (ratio < 0.5) {
                return MEDIUM;
            } else if (ratio < 0.75) {
                return HARD;
            } else {
                return HARDER_THAN_HARD_I_GUESS;
            }
        }
    }
}
