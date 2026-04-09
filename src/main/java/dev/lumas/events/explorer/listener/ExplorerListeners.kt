package dev.lumas.events.explorer.listener

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.custom.BlockBrokenExplorerEvent
import dev.lumas.events.explorer.custom.BlockPlacedExplorerEvent
import io.papermc.paper.event.block.PlayerShearBlockEvent
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerArmSwingEvent
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent
import io.papermc.paper.event.player.PlayerFailMoveEvent
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerNameEntityEvent
import io.papermc.paper.event.player.PlayerShieldDisableEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.command.UnknownCommandEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerShearEntityEvent

@Register(Autowire.LISTENER)
class ExplorerListeners : AbstractExplorerListener {
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        fire(event, event.player)
        fire(BlockBrokenExplorerEvent(event.block), event.player)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        fire(event, event.player)
        fire(BlockPlacedExplorerEvent(event.block), event.player)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity.killer ?: event.entity
        fire(event, entity)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        fire(event, event.getEntity())
    }

    //    @EventHandler
    //    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    //        if (event.getDamager() instanceof Player player) {
    //            fire(event, player);
    //        }
    //    }
    @EventHandler
    fun onPlayerFailMove(event: PlayerFailMoveEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.hasExplicitlyChangedBlock()) {
            fire(event, event.getPlayer())
        }
    }

    @EventHandler
    fun onPlayerJump(event: PlayerJumpEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        fire(event, event.whoClicked)
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onUnknownCommand(event: UnknownCommandEvent) {
        val player = event.sender as? Player ?: return
        fire(event, player)
    }

    @EventHandler
    fun onPlayerAttemptPickupItem(event: PlayerAttemptPickupItemEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerClientOptionsChange(event: PlayerClientOptionsChangeEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerFlowerPotManipulate(event: PlayerFlowerPotManipulateEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerItemBreak(event: PlayerItemBreakEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerNameEntity(event: PlayerNameEntityEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerEditBook(event: PlayerEditBookEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerElytraBoost(event: PlayerElytraBoostEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerRiptide(event: PlayerRiptideEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerShieldDisable(event: PlayerShieldDisableEvent) {
        val player = event.damager as? Player ?: return
        fire(event, player)
    }

    @EventHandler
    fun onPlayerShearBlock(event: PlayerShearBlockEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerChangeBeaconEffect(event: PlayerChangeBeaconEffectEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerEggThrow(event: PlayerEggThrowEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerItemFrameChange(event: PlayerItemFrameChangeEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerItemMend(event: PlayerItemMendEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val viewers = event.viewers
        if (viewers.isEmpty()) return
        fire(event, viewers.first())
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        fireLater(event, event.getPlayer(), 400L)
    }

    @EventHandler
    fun onPlayerAnimation(event: PlayerArmSwingEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        fire(event, event.getPlayer())
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        fire(event, event.player)
    }

    @EventHandler
    fun onVillagerReplenishTrade(event: VillagerReplenishTradeEvent) {
        fire(event, event.entity)
    }

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        fire(event, event.entity)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        fire(event, event.entity)
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        fire(event, event.entity)
    }

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        fire(event, event.entity)
    }

    @EventHandler
    fun onEntityTame(event: EntityTameEvent) {
        fire(event, event.owner as? Entity ?: return)
    }
}