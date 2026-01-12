package dev.jsinco.luma.lumaevents.games.logic;

import com.google.common.base.Preconditions;
import dev.jsinco.luma.lumaevents.configurable.sectors.PanelPartyMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.exceptions.MinigameException;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRole;
import dev.jsinco.luma.lumaevents.games.interfaces.models.MinigameRoleMap;
import dev.jsinco.luma.lumaevents.games.interfaces.structures.Structure;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelParty extends InventoryUnifiedMinigame {

    private static final long DURATION = 600000L;
    private static final long TICK_INTERVAL = 2L;
    private static final long ROUND_DURATION = 10000L;
    private static final long ROUND_INTERVAL = 2000L;

    private final Location spawnLocation;
    private final Location center;
    private final List<Structure> panels;

    private final MinigameRoleMap<AbstractPanelPlayer> roleMap;


    private Map<BlockData, Integer> currentBlockDataCount;
    private @Nullable BlockData chosenBlockData;
    private int round;
    private int totalRounds;
    private CountdownBossBar countdownBossBar;

    public PanelParty(PanelPartyMinigameDefinition def) {
        super("Panel Party", "Stand on the correct color!", DURATION, TICK_INTERVAL, true);
        this.boundingBox = def.getRegion().toWorldTiedBoundingBox();
        this.spawnLocation = def.getSpawnLocation();
        this.center = def.getCenter();
        this.panels = def.getSchematics().stream()
                .map(fileName -> new Structure(this.center, fileName))
                .toList();
        this.roleMap = new MinigameRoleMap<>();
        this.currentBlockDataCount = new HashMap<>();
        this.round = 0;
        this.totalRounds = panels.size();
    }

    @Override
    protected int minimumParticipants() {
        return 1;
    }

    @Override
    protected void tokenHandler(EventPlayer participant) {

    }

    @Override
    protected void handleStart() {
        for (EventPlayer participant : this.getParticipants()) {
            PanelParticipant role = new PanelParticipant(participant);
            this.roleMap.put(role);
        }

        if (this.panels.isEmpty()) {
            this.stop();
            throw new MinigameException("No schematics available to start the game.");
        }


        Executors.sync(() -> {
            this.nextRound();
        });
    }

    @Override
    protected void onRunnable(long timeLeft) {

    }

    @Override
    protected void handleStop() {

    }


    public boolean nextRound() {
        // check if more rounds are available
        System.out.println("Current round: " + this.round + ", Total rounds: " + this.totalRounds);
        if (this.round + 1 > this.totalRounds) {
            return false;
        }

        // paste the next panel and count block data
        Structure panel = this.panels.get(this.round);
        System.out.println("Pasting panel for round " + (this.round + 1));
        panel.paste((vector3i, blockData) -> {
            Integer count = currentBlockDataCount.getOrDefault(blockData, 0);
            currentBlockDataCount.put(blockData, count + 1);
            return true;
        });

        // Teleport players to the center
        for (PanelParticipant role : roleMap.getMatching(PanelParticipant.class)) {
            role.teleportAsync(center);
        }

        // choose a random block data from the current panel
        this.chosenBlockData = Util.getRandom(currentBlockDataCount.keySet());

        // increment round
        this.round += 1;

        Preconditions.checkNotNull(this.chosenBlockData, "Chosen block data cannot be null.");

        // start countdown
        this.countdownBossBar = CountdownBossBar.builder()
                .miliseconds(ROUND_DURATION)
                .title("Round %d, stand on: %s".formatted(this.round, this.chosenBlockData.getMaterial().name()))
                .audience(this.getAudience())
                .color(BossBar.Color.WHITE)
                .callback(() -> {
                    Preconditions.checkNotNull(this.chosenBlockData, "Chosen block data cannot be null during countdown callback.");
                    // remove panel
                    Executors.sync(() -> {
                        panel.remove((vector3i, blockData) ->
                                !blockData.getMaterial().equals(this.chosenBlockData.getMaterial())
                        );
                    });

                    // recursively start next round
                    CountdownBossBar.builder()
                            .miliseconds(ROUND_INTERVAL)
                            .title("Hold Your Positions!")
                            .audience(this.getAudience())
                            .color(BossBar.Color.YELLOW)
                            .callback(() -> {
                                Executors.sync(() -> {
                                    if (!this.nextRound()) {
                                        this.stop();
                                    }
                                });
                            })
                            .build()
                            .start();
                })
                .build();
        this.countdownBossBar.start();
        return true;
    }


    private static class AbstractPanelPlayer extends MinigameRole {
        protected AbstractPanelPlayer(EventPlayer eventPlayer) {
            super(eventPlayer);
        }
    }

    public static class PanelParticipant extends AbstractPanelPlayer {
        protected PanelParticipant(EventPlayer eventPlayer) {
            super(eventPlayer);
        }
    }

    private static class PanelSpectator extends MinigameRole {
        protected PanelSpectator(EventPlayer eventPlayer) {
            super(eventPlayer);
        }
    }
}
