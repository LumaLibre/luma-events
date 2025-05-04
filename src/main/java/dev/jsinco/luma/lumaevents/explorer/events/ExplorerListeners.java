package dev.jsinco.luma.lumaevents.explorer.events;

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.custom.BlockBrokenExplorerEvent;
import dev.jsinco.luma.lumaevents.explorer.custom.BlockPlacedExplorerEvent;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent;
import io.papermc.paper.event.player.PlayerFailMoveEvent;
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerNameEntityEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.util.List;

@AutoRegister(RegisterType.LISTENER)
public final class ExplorerListeners extends AbstractExplorerListener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        fire(new BlockBrokenExplorerEvent(event.getBlock()), event.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        fire(new BlockPlacedExplorerEvent(event.getBlock()), event.getPlayer());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getDamageSource().getCausingEntity() instanceof Player player) {
            fire(event, player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        fire(event, event.getEntity());
    }

//    @EventHandler
//    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//        if (event.getDamager() instanceof Player player) {
//            fire(event, player);
//        }
//    }

    @EventHandler
    public void onPlayerFailMove(PlayerFailMoveEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.hasExplicitlyChangedBlock()) {
            fire(event, event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        fire(event, event.getWhoClicked().getUniqueId());
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onUnknownCommand(UnknownCommandEvent event) {
        if (event.getSender() instanceof Player player) {
            fire(event, player);
        }
    }

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerClientOptionsChange(PlayerClientOptionsChangeEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerFlowerPotManipulate(PlayerFlowerPotManipulateEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerNameEntity(PlayerNameEntityEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerElytraBoost(PlayerElytraBoostEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerShieldDisable(PlayerShieldDisableEvent event) {
        if (event.getDamager() instanceof Player player) {
            fire(event, player);
        }
    }

    @EventHandler
    public void onPlayerShearBlock(PlayerShearBlockEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangeBeaconEffect(PlayerChangeBeaconEffectEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemFrameChange(PlayerItemFrameChangeEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemMend(PlayerItemMendEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        List<HumanEntity> viewers = event.getViewers();
        if (viewers.isEmpty()) return;
        fire(event, viewers.getFirst().getUniqueId());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        fireLater(event, event.getPlayer(), 400L);
    }

    @EventHandler
    public void onPlayerAnimation(PlayerArmSwingEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        fire(event, event.getPlayer());
    }
}
