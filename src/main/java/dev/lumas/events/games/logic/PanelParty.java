package dev.lumas.events.games.logic;

import com.google.common.base.Preconditions;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.PanelPartyMinigameDefinition;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.games.exceptions.MinigameException;
import dev.lumas.events.games.interfaces.InventoryUnifiedMinigame;
import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.models.Scoreboard;
import dev.lumas.events.games.tokenformula.FlatIntTokenFormula;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
            "<gold>%s</gold> became a spectator."
    };

    private final Location spawnLocation;
    private final Location center;
    private final int reachableRadius;
    private final GenericStructure blankPanel;
    private final List<GenericStructure> panels;
    private final MinigameRoleMap<AbstractPanelPlayer> roleMap;
    private final Scoreboard<EventPlayer> scoreboard;
    private final TokenFormula<Integer> tokenFormula;
    private final int maxRounds;
    private final boolean superSpeed;
    private final int superSpeedBoost;

    private int round;
    private @NotNull PanelPartyProcess currentProcess;

    public PanelParty(PanelPartyMinigameDefinition def) {
        super("Panel Party", "Stand on the correct color!", DURATION, TICK_INTERVAL, true, true, true, false);
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
        this.spawnLocation = def.getSpawnLocation();
        this.center = def.getCenter();
        this.reachableRadius = def.getReachableRadius();
        this.blankPanel = new GenericStructure(this.center, def.getBlankPanelSchematic());
        this.panels = new ArrayList<>(def.getSchematics().stream()
                .map(fileName -> new GenericStructure(this.center, fileName))
                .toList());
        this.roleMap = new MinigameRoleMap<>(AbstractPanelPlayer::cleanup);
        this.scoreboard = new Scoreboard<>();
        this.tokenFormula = new FlatIntTokenFormula(12);
        this.maxRounds = panels.size();
        this.superSpeed = def.isSuperSpeed();
        this.superSpeedBoost = def.getSuperSpeedBoost();



        this.round = 0;
        this.currentProcess = new PanelPartyProcess(this, PanelDifficulty.fromRounds(this.round, this.maxRounds));

        Collections.shuffle(this.panels, RANDOM); // shuffle panels

        Executors.sync(center, () -> {
            this.currentProcess.paste(this.round); // pre-paste first panel to count materials and prepare for the game start
        });
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {
        int score = this.scoreboard.getScore(participant);
        this.tokenFormula.giveTokens(participant, score);
        participant.addPermanentScore(MinigameConstant.PANEL_PARTY, score);
    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer participant) {
        participant.teleportAsync(this.spawnLocation);
        return super.handleParticipantJoin(participant);
    }

    @Override
    public boolean removeParticipant(EventPlayer participant, boolean doTeleport) {
        AbstractPanelPlayer role = this.roleMap.remove(participant.getUuid());
        if (role != null) {
            role.cleanup();
            if (role instanceof PanelParticipant panelParticipant) {
                panelParticipant.checkEnd();
            }
        }
        return super.removeParticipant(participant, doTeleport);
    }

    @Override
    protected void handleStart() {
        Location initialTeleportLocation = this.center.clone().add(RANDOM.nextDouble(0, 5), 3, RANDOM.nextDouble(0, 5));
        for (EventPlayer participant : this.getParticipants()) {
            PanelParticipant role = new PanelParticipant(this, participant);
            participant.teleportAsync(initialTeleportLocation).thenAccept(result -> {
                role.setReadyToTick(true);
            });
            this.roleMap.put(role);
        }

        if (this.panels.isEmpty()) {
            this.stop();
            throw new MinigameException("No schematics available to start the game.");
        }

        Preconditions.checkNotNull(this.currentProcess, "Current process cannot be null during handleStart.");

        Executors.runSync(this.center, () -> {
            this.currentProcess.run(this.round);
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {
        if (this.shouldStop()) {
            this.stop();
            return;
        }

        boolean isFinished = this.currentProcess.isFinished();


        Material chosenMaterial = this.currentProcess.getChosenMaterial();
        Component component;
        if (!isFinished && chosenMaterial != null) {
            TextColor textColor = TextColor.color(chosenMaterial.createBlockData().getMapColor().asRGB());
            component = Component.text("Stand on: " + Util.formatMaterialName(chosenMaterial.toString()))
                    .color(textColor)
                    .decorate(TextDecoration.UNDERLINED);
        } else {
            component = null;
        }

        this.roleMap.forEach(abstractPanelPlayer -> {
            abstractPanelPlayer.tick();

            if (component != null) {
                abstractPanelPlayer.getEventPlayer().sendActionBar(component);
            }
        });

        Preconditions.checkNotNull(this.currentProcess, "Current process cannot be null during onRunnable.");

        if (!isFinished) {
            return;
        }

        int points = this.currentProcess.difficulty.getScoreboardWeight();
        for (PanelParticipant panelParticipant : this.roleMap.getMatching(PanelParticipant.class)) {
            this.scoreboard.addScore(panelParticipant.getEventPlayer(), points); // Award points for surviving the round
        }

        this.round++;
        if (this.shouldStop()) {
            this.stop();
        } else {
            this.currentProcess = new PanelPartyProcess(this, PanelDifficulty.fromRounds(this.round, this.maxRounds));
            Executors.runSync(this.center, () -> {
                this.currentProcess.run(this.round);
            });
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

        Executors.runSync(this.center, () -> {
            this.blankPanel.paste();
        });

        this.scoreboard.handleGameEnd(this.getAudience(), () -> {
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.BLUE)
                    .title("<aqua><b>Game Over")
                    .seconds(10)
                    .callback(() -> {
                        try {
                            this.participants.forEach(eventPlayer -> {
                                Location loc = this.getGameDropOffLocation();
                                if (loc != null) {
                                    Executors.runDelayedAsync(TimeUnit.MILLISECONDS, 100, (t) -> {
                                        eventPlayer.teleportAsync(loc);
                                    });
                                }
                                eventPlayer.sendMessage("This minigame has concluded.");
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        this.ensureNotIllegal();
        Player player = event.getPlayer();
        AbstractPanelPlayer role = this.roleMap.get(player.getUniqueId());
        if (role != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        this.ensureNotIllegal();
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        AbstractPanelPlayer victimRole = this.roleMap.get(victim.getUniqueId());
        AbstractPanelPlayer attackerRole = this.roleMap.get(attacker.getUniqueId());

        if (victimRole != null && attackerRole != null) {
            victimRole.attacked(event, attackerRole);
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
        public abstract void attacked(EntityDamageByEntityEvent event, AbstractPanelPlayer attacker);
    }

    private static class PanelParticipant extends AbstractPanelPlayer {

        private static final PotionEffect JUMP_BOOST = new PotionEffect(PotionEffectType.JUMP_BOOST, 210, 0, true, false);
        private static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 210, 99, true, false);
        private static final ItemStack AIR = ItemStack.of(Material.AIR);
        private static final int ELIMINATION_Y_LEVEL_OFFSET = 50;

        private final int eliminationYLevel;
        private boolean eliminated;
        @Setter
        private boolean readyToTick;

        public PanelParticipant(PanelParty context, EventPlayer eventPlayer) {
            super(context, eventPlayer);
            this.eliminationYLevel = context.center.getBlockY() - ELIMINATION_Y_LEVEL_OFFSET;
            this.eliminated = false;
            this.readyToTick = false;
            this.eventPlayer.operatePlayer(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
        }

        @Override
        public void tick() {
            if (!this.readyToTick) return;

            this.eventPlayer.operatePlayer(player -> {
                PanelPartyProcess process = this.context.currentProcess;


                boolean ss = this.context.superSpeed;

                if (process.difficulty.hasModifier(PanelDifficultyModifier.JUMP_BOOST_ENABLED)) {
                    player.addPotionEffect(JUMP_BOOST);
                    player.getActivePotionEffects().forEach(potionEffect -> {
                        if (!potionEffect.equals(JUMP_BOOST)) {
                            player.removePotionEffect(potionEffect.getType());
                        }
                    });
                } else if (!ss) {
                    player.clearActivePotionEffects();
                }

                if (ss) {
                    player.addPotionEffect(SPEED.withAmplifier(this.context.superSpeedBoost));
                }

                if (player.getLocation().getY() <= this.eliminationYLevel) {
                    this.eliminate();
                    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
                } else {
                    if (player.isFlying()) player.setFlying(false); // tfly works with enough lag, so we cancel it here
                    player.setFoodLevel(20);
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
            this.eventPlayer.operatePlayer(player ->  {
                player.setFallDistance(0);
                player.teleportAsync(location);
            });

            //this.context.scoreboard.addScore(this.eventPlayer, 1);

            this.eventPlayer.sendMessage("You have been eliminated and are now a spectator!");
            this.context.sendAudienceMessage(String.format(Util.getRandom(ELIMINATION_MESSAGES), this.eventPlayer.getName()));

            this.checkEnd();
        }

        @Override
        public void attacked(EntityDamageByEntityEvent event, AbstractPanelPlayer attacker) {
            PanelPartyProcess process = this.context.currentProcess;
            if (process.isIntermission() && process.difficulty.hasModifier(PanelDifficultyModifier.PVP_ENABLED) && attacker instanceof PanelParticipant) {
                return; // allow pvp
            }
            event.setCancelled(true);
        }

        public void checkEnd() {
            if (this.context.roleMap.getMatching(PanelParticipant.class).size() <= 1) {
                PanelParticipant winner = this.context.roleMap.getMatching(PanelParticipant.class).stream().findFirst().orElse(null);
                if (winner != null) {
                    this.context.sendAudienceMessage("<gold><b>" + winner.getEventPlayer().getName() + "</b></gold> was the last player standing!");
                    this.context.scoreboard.addScore(winner.getEventPlayer(), 1); // bonus points for winning
                }
                Executors.runDelayedAsync(TimeUnit.MILLISECONDS, 50, task -> {
                    this.context.stop();
                });
            }
        }


        public void setItemInHand(ItemStack itemStack) {
            this.eventPlayer.operatePlayer(player -> {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !Tag.TERRACOTTA.isTagged(item.getType())) {
                        return; // lazy fix, probably broke because of folia scheduling
                    }
                }
                player.getInventory().setItemInMainHand(itemStack);
            });
        }
    }

    private static class PanelSpectator extends AbstractPanelPlayer {

        private static final PotionEffect INVISIBILITY = new PotionEffect(PotionEffectType.INVISIBILITY, 210, 0, true, true);

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
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
                if (!player.isFlying()) {
                    player.setFlying(true);
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

        @Override
        public void attacked(EntityDamageByEntityEvent event, AbstractPanelPlayer attacker) {
            event.setCancelled(true); // spectators cannot be attacked
        }

        private void hide() {
            this.eventPlayer.operatePlayer(self -> {
                for (PanelParticipant other : this.context.roleMap.getMatching(PanelParticipant.class)) {
                    Player bukkitOther = other.getEventPlayer().getPlayer();
                    if (bukkitOther != null) {
                        bukkitOther.hidePlayer(EventMain.getInstance(), self);
                    }
                }
            });
        }

        private void show() {
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

    private static class PanelPartyProcess {

        private static final float ROUND_INTERVAL_SECONDS = 5;
        private static final int MINIMUM_BLOCKS_PER_MATERIAL = 10;

        private final PanelParty context;
        private final PanelDifficulty difficulty;
        private final Set<Material> availableMaterials;
        private final AtomicBoolean finished;
        private final AtomicBoolean intermission;

        private CountdownBossBar countdownBossBar;
        private CountdownBossBar intermissionBossBar;

        @Getter
        private Material chosenMaterial;
        @MonotonicNonNull
        private GenericStructure panel;


        public PanelPartyProcess(PanelParty context, PanelDifficulty difficulty) {
            this.context = context;
            this.difficulty = difficulty;
            this.availableMaterials = new HashSet<>();
            this.finished = new AtomicBoolean(false);
            this.intermission = new AtomicBoolean(false);
        }

        public boolean isFinished() {
            return this.finished.get();
        }

        public boolean isIntermission() {
            return this.intermission.get();
        }

        public void paste(int round) {
            if (this.panel != null) {
                return; // already pasted
            }
            // paste the next panel and count block data
            this.panel = this.context.panels.get(round);
            this.pastePanelWithCounting(panel);
        }

        public boolean run(final int round) {
            // check if more rounds are available
            if (round + 1 > this.context.maxRounds) {
                return false;
            }

            this.paste(round);

            this.chosenMaterial = Util.getRandom(this.availableMaterials);
            Preconditions.checkNotNull(this.chosenMaterial, "Chosen block data cannot be null.");
            TextColor textColor = TextColor.color(chosenMaterial.createBlockData().getMapColor().asRGB());
            Component component = Component.text("Stand on: " + Util.formatMaterialName(chosenMaterial.toString()))
                    .color(textColor)
                    .decorate(TextDecoration.UNDERLINED);
            this.context.sendAudienceMessage(component);


            // give players the chosen block in hand if applicable
            ItemStack itemStack = ItemStack.of(this.chosenMaterial);
            if (this.difficulty.hasModifier(PanelDifficultyModifier.SHOW_PHYSICAL_BLOCK)) {
                for (PanelParticipant panelParticipant : this.context.roleMap.getMatching(PanelParticipant.class)) {
                    panelParticipant.setItemInHand(itemStack);
                }
            }


            StringBuilder titleBuilder = new StringBuilder();
            titleBuilder.append(this.difficulty.formatted(this.panelName(panel.getLocalSchemPath())));
            titleBuilder.append(" <gray>#").append(round + 1);
            if (this.difficulty.hasModifier(PanelDifficultyModifier.PVP_ENABLED)) {
                titleBuilder.append(this.difficulty.formatted(" (PvP)"));
            }

            String intermissionTitle = this.difficulty.formatted(!this.difficulty.hasModifier(PanelDifficultyModifier.PVP_ENABLED) ? "Don't move!" : "Don't move! (PvP)");

            this.context.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);

            this.countdownBossBar = CountdownBossBar.builder()
                    .seconds(this.difficulty.getSeconds())
                    .title(titleBuilder.toString())
                    .audience(this.context.getAudience())
                    .color(this.difficulty.getBossBarColor())
                    .callback(() -> {
                        Preconditions.checkNotNull(this.chosenMaterial, "Chosen block data cannot be null during countdown callback.");
                        // remove panel
                        Executors.sync(context.center, () -> {
                            panel.remove((vector3i, blockData) ->
                                    !blockData.getMaterial().equals(this.chosenMaterial)
                            );
                        });

                        if (this.difficulty.hasModifier(PanelDifficultyModifier.SHOW_PHYSICAL_BLOCK)) {
                            for (PanelParticipant panelParticipant : this.context.roleMap.getMatching(PanelParticipant.class)) {
                                panelParticipant.setItemInHand(PanelParticipant.AIR);
                            }
                        }

                        // start intermission
                        this.intermission.set(true);
                        this.context.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1.0f, 1.0f);

                        this.intermissionBossBar = CountdownBossBar.builder()
                                .seconds(ROUND_INTERVAL_SECONDS)
                                .title(intermissionTitle)
                                .audience(this.context.getAudience())
                                .color(this.difficulty.getBossBarColor())
                                .callback(() -> {
                                    this.intermission.set(false);
                                    this.finished.set(true);
                                    //this.context.playAudienceSound(Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.8f, 1.0f);
                                })
                                .build()
                                .start();
                    })
                    .build()
                    .start();

            return true;
        }

        private void pastePanelWithCounting(GenericStructure panel) {
            if (this.panel == null) {
                throw new IllegalStateException("Panel structure must be initialized before pasting.");
            }

            Map<Material, Integer> materialCountMap = new HashMap<>();

            panel.paste((vector3i, blockData) -> {
                if (!isReachable(vector3i)) return true;

                Material material = blockData.getMaterial();
                // If it's already qualified, no need to keep counting
                if (this.availableMaterials.contains(material)) {
                    return true;
                } else if (this.difficulty.hasModifier(PanelDifficultyModifier.IGNORE_MINIMUM_BLOCKS)) {
                    this.availableMaterials.add(material);
                    return true;
                }

                int newCount = materialCountMap.merge(material, 1, Integer::sum);

                if (newCount >= MINIMUM_BLOCKS_PER_MATERIAL) {
                    this.availableMaterials.add(material);
                }
                return true;
            });
        }

        private boolean isReachable(Vector3i v) {
            int r = this.context.reachableRadius;
            return v.x >= -r && v.x < r && v.z >= -r && v.z < r;
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
    private enum PanelDifficulty {
        EASY(11, BossBar.Color.GREEN, "<green>", 1, PanelDifficultyModifier.SHOW_PHYSICAL_BLOCK),
        MEDIUM(7, BossBar.Color.YELLOW, "<yellow>", 1, PanelDifficultyModifier.SHOW_PHYSICAL_BLOCK),
        HARD(6, BossBar.Color.RED, "<dark_red>", 1, PanelDifficultyModifier.IGNORE_MINIMUM_BLOCKS),
        HARDER_THAN_HARD_I_GUESS(4, BossBar.Color.PURPLE, "<dark_purple>", 2, PanelDifficultyModifier.PVP_ENABLED, PanelDifficultyModifier.IGNORE_MINIMUM_BLOCKS);

        private final int seconds;
        private final BossBar.Color bossBarColor;
        private final String textColorPrefix;
        private final int scoreboardWeight;
        private final PanelDifficultyModifier[] modifiers;

        PanelDifficulty(int seconds, BossBar.Color bossBarColor, String textColorPrefix, int scoreboardWeight, PanelDifficultyModifier... modifiers) {
            this.seconds = seconds;
            this.bossBarColor = bossBarColor;
            this.scoreboardWeight = scoreboardWeight;
            this.textColorPrefix = textColorPrefix;
            this.modifiers = modifiers;
        }

        public String formatted(String message) {
            return this.textColorPrefix + "<b>" + message;
        }

        public boolean hasModifier(PanelDifficultyModifier modifier) {
            for (PanelDifficultyModifier mod : this.modifiers) {
                if (mod == modifier) {
                    return true;
                }
            }
            return false;
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


    private enum PanelDifficultyModifier {
        PVP_ENABLED,
        IGNORE_MINIMUM_BLOCKS,
        JUMP_BOOST_ENABLED,
        SHOW_PHYSICAL_BLOCK
    }
}
