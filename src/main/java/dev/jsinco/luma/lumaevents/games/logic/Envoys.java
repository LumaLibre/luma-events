package dev.jsinco.luma.lumaevents.games.logic;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.enums.minigame.EnvoyBlockType;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameScoreboard;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.obj.minigame.EnvoyBlock;
import dev.jsinco.luma.lumaevents.utility.MinigameConstants;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// FIXME: Envoys becoming bad blocks such as dirt_path or flowers
public non-sealed class Envoys extends Minigame {

    private static final List<Material> PROTECTED_BLOCKS = List.of(
            Material.DIRT_PATH, Material.RED_TULIP, Material.WHITE_TULIP, Material.ROSE_BUSH,
            Material.ALLIUM, Material.CORNFLOWER, Material.ORANGE_TULIP,
            Material.SPRUCE_SLAB, Material.SPRUCE_TRAPDOOR, Material.SPRUCE_FENCE,
            Material.SPRUCE_STAIRS
    );

    private final ConcurrentLinkedQueue<EnvoyBlock> cachedEnvoys;
    private final Location spawnPoint;
    private final MinigameScoreboard scoreboard;
    private CountdownBossBar countdownBossBar;


    public Envoys(MinigameDefinition def) {
        super("Envoys", MinigameConstants.ENVOYS_DESC, MinigameConstants.ENVOYS_DURATION, 30, false);
        this.boundingBox = WorldTiedBoundingBox.of(def.getRegion().getLoc1(), def.getRegion().getLoc2());
        this.spawnPoint = def.getSpawnLocation().toCenterLocation();
        this.cachedEnvoys = new ConcurrentLinkedQueue<>();
        this.scoreboard = new MinigameScoreboard(30);
    }


    @Override
    protected void handleStart() {
        countdownBossBar = CountdownBossBar.builder()
                .title("<green><b>Time Remaining</b><gray>:</gray> <b>%s</b></green>")
                .color(BossBar.Color.GREEN)
                .miliseconds(this.getDuration())
                .audience(this.audience)
                .build();
        countdownBossBar.start();
    }

    @Override
    protected void onRunnable(long timeLeft) {
        for (int i = 0; i < this.participants.size(); i++) {
            Location loc1 = this.boundingBox.getRandomLocation().toCenterLocation();
            EnvoyBlockType envoyBlockType = Util.getRandFromList(EnvoyBlockType.values());
            FallingBlock fallingBlock = this.boundingBox.getWorld()
                    .spawnFallingBlock(loc1, envoyBlockType.getFallingBlock().createBlockData());
            fallingBlock.setDropItem(false);
            fallingBlock.setHurtEntities(false);

            cachedEnvoys.add(EnvoyBlock.fromFallingBlock(fallingBlock, envoyBlockType));
        }

        for (EventPlayer participant : this.participants) {
            Player player = participant.getPlayer();
            if (player == null) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 250, 7));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 250, 3));
        }


        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> {
            for (EnvoyBlock envoyBlock : this.cachedEnvoys) {
                Location loc = envoyBlock.getLocation().toCenterLocation();
                if (envoyBlock.isSolid() && !EnvoyBlockType.getSolidBlocks().contains(loc.getBlock().getType())) {
                    Bukkit.getScheduler().runTask(EventMain.getInstance(), envoyBlock::remove);
                    this.cachedEnvoys.remove(envoyBlock);
                    continue;
                }

                this.boundingBox.getWorld()
                        .spawnParticle(Particle.FIREWORK, loc, 10, 0.5, 0.5, 0.5, 0.1);
            }
            for (EventPlayer eventPlayer : this.getParticipants()) {
                eventPlayer.sendActionBar(
                        "<green>Total Envoys<gray>: <gold>"+scoreboard.getScore(eventPlayer)+
                                " <dark_gray>(Points: "+scoreboard.getPoints(eventPlayer)+")"
                );
            }
        });
    }


    @Override
    protected void handleStop() {
        if (Bukkit.isPrimaryThread()) {
            for (EnvoyBlock envoyBlock : this.cachedEnvoys) {
                envoyBlock.remove();
            }
        } else {
            Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                for (EnvoyBlock envoyBlock : this.cachedEnvoys) {
                    envoyBlock.remove();
                }
            });
        }
        if (countdownBossBar != null) {
            countdownBossBar.stop(false);
        }

        scoreboard.handleGameEnd(this.participants, this.audience, () -> {
            this.participants.stream().filter(
                    p -> p.getPlayer() != null
            ).forEach(p -> p.getPlayer().teleportAsync(this.spawnPoint));
            CountdownBossBar.builder()
                    .audience(this.audience)
                    .color(BossBar.Color.YELLOW)
                    .title("<yellow><b>Game Over</b></yellow>")
                    .seconds(15)
                    .callback(() -> this.participants.forEach(player -> {
                        Player bukkitPlayer = player.getPlayer();
                        if (bukkitPlayer != null && this.boundingBox.isInWithMarge(bukkitPlayer.getLocation(), 400)) {
                            bukkitPlayer.teleportAsync(this.getGameDropOffLocation());
                        }
                        player.sendMessage("This minigame has concluded.");
                    }))
                    .build()
                    .start();
        });
    }

    @Override
    protected void handleParticipantJoin(EventPlayer player) {
        player.teleportAsync(this.spawnPoint);
    }

    @EventHandler
    public void onEnvoyLand(EntityChangeBlockEvent event) {
        this.ensureNotIllegal();
        EnvoyBlock envoyBlock = this.getEnvoyBlock(event.getEntity());

        if (envoyBlock == null) {
            return;
        }

        Block block = event.getBlock();

        if (!block.isEmpty() || PROTECTED_BLOCKS.contains(block.getRelative(BlockFace.DOWN).getType())) {
            block = block.getRelative(0, 2, 0);
        }

        block.setType(envoyBlock.getEnvoyBlockType().getSolidBlock());

        envoyBlock.updateToBlock(block);
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.0F);
        event.setCancelled(true);
    }

    @EventHandler
    public void onEnvoyInteract(PlayerInteractEvent event) {
        this.ensureNotIllegal();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        EnvoyBlock envoyBlock = this.getEnvoyBlock(block);
        if (envoyBlock == null) {
            return;
        }

        envoyBlock.remove();
        this.cachedEnvoys.remove(envoyBlock);
        EventPlayer player = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());
        if (!this.participants.contains(player)) {
            player.sendMessage("You are not participating in this minigame");
            return;
        }
        this.scoreboard.addScore(player, 1);

        if (scoreboard.getScore(player) % 15 == 0) {
            Util.giveTokens(event.getPlayer(), 1);
        }
    }


    private EnvoyBlock getEnvoyBlock(Object thing) {
        return cachedEnvoys.stream()
                .filter(envoyBlock -> envoyBlock.is(thing))
                .findFirst()
                .orElse(null);
    }
}
