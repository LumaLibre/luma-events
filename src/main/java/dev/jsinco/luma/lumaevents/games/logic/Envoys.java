package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public non-sealed class Envoys extends Minigame {

    //private static final List<Material> BLACKLISTED_MATERIALS = List.of(Material.BARRIER);

    private static final String METADATA_KEY = "envoy";

    private final WorldTiedBoundingBox boundingBox;
    private final List<BlockState> cachedEnvoys;
    private final MinigameScoreboard scoreboard;


    public Envoys(Location loc1, Location loc2) {
        super("Envoys", MinigameConstants.ENVOYS_DESC, 30000L, 20, false);
        this.boundingBox = WorldTiedBoundingBox.of(loc1, loc2);
        this.cachedEnvoys = new ArrayList<>();
        this.scoreboard = new MinigameScoreboard();
    }


    @Override
    protected void handleStart() {

    }

    @Override
    protected void onRunnable(long timeLeft) {
        FallingBlock fallingBlock = this.findValidSpawnLocation()
                .getWorld()
                .spawn(this.findValidSpawnLocation(), FallingBlock.class);


        BlockState state = fallingBlock.getBlockState();
        state.setType(Material.BARREL);
        //state.setMetadata(METADATA_KEY, new FixedMetadataValue(EventMain.getInstance(), true));
        state.update(true);
        cachedEnvoys.add(state);
        System.out.println(cachedEnvoys);

        // Particles
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> {
            for (BlockState blockState : this.cachedEnvoys) {
                blockState.getWorld().spawnParticle(Particle.FIREWORK, blockState.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        });
    }


    @Override
    protected void handleStop() {
        for (BlockState state : this.cachedEnvoys) {
            state.setType(Material.AIR);
            state.update(true);
        }
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {

    }

    @EventHandler
    public void onEnvoyLand(EntityChangeBlockEvent event) {
        if (!isEnvoy(event.getBlock().getState())) {
            return;
        }

        this.cachedEnvoys.add(event.getBlock().getState(true));
        event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
    }

    @EventHandler
    public void onEnvoyInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !isEnvoy(block.getState())) {
            return;
        }

        block.setType(Material.AIR);
        this.cachedEnvoys.remove(block.getState(true));
        EventPlayer player = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());
        player.sendMessage("You have found an envoy!");
        player.addPoints(1);
    }

    private Location findValidSpawnLocation() {
        Location loc = this.boundingBox.getRandomLocation();
        short attempts = 0;
        while (!loc.getBlock().getType().isEmpty()) {
            loc = this.boundingBox.getRandomLocation();
            if (attempts++ > 10) {
                break;
            }
        }
        return loc;
    }

    private boolean isEnvoy(BlockState state) {
        return cachedEnvoys.contains(state); //|| state.hasMetadata(METADATA_KEY);
    }
}
