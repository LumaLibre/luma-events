package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.configurable.sectors.CylinderRegion;
import dev.jsinco.luma.lumaevents.configurable.sectors.CylindricalMinigameDefinition;
import dev.jsinco.luma.lumaevents.games.interfaces.InventoryUnifiedMinigame;
import dev.jsinco.luma.lumaevents.obj.CylinderBoundingBox;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.Location;

public class TNTRun extends InventoryUnifiedMinigame {

    private static final int LAYER_GAP = 8; // The gap between layer of TNT/Sand blocks
    private static final int LAYER_AMOUNT = 3; // The amount of layers of TNT/Sand blocks

    private final Location spawnLocation;

    protected TNTRun(CylindricalMinigameDefinition def) {
        super("TNT Run", "Don't fall down!", 480000, 20, true, true);
        CylinderRegion r = def.getRegion();
        this.boundingBox = CylinderBoundingBox.of(r.getCenter(), r.getRadius(), r.getHeight());
        this.spawnLocation = def.getSpawnLocation();

        if (LAYER_AMOUNT * LAYER_GAP > r.getHeight()) {
            throw new IllegalArgumentException("The height of the region is too small for the configured layers.");
        }
    }

    @Override
    protected void handleStart() {

    }

    @Override
    protected void onRunnable(long timeLeft) {

    }

    @Override
    protected void handleStop() {

    }

    @Override
    protected boolean handleParticipantJoin(EventPlayer player) {
        return false;
    }


    public enum TNTLayerType {
        TNT, SAND
    }
}
