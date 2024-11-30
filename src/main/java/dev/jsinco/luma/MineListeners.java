package dev.jsinco.luma;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import me.jet315.prisonmines.events.PlayerBreakBlockInMineEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MineListeners implements Listener {

    public static final String DROP_KEY = "event_drop";
    private static final ItemStack AIR = new ItemStack(Material.AIR);
    private static final ItemStack DROP_ITEM = new ItemStack(Material.ORANGE_CANDLE);
    static {
        DROP_ITEM.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        Util.setPersistentKey(DROP_ITEM, DROP_KEY, PersistentDataType.BOOLEAN, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getWorld().getName().contains("event")) {
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onPlayerBreaksMineBlock(PlayerBreakBlockInMineEvent event) {
        PrisonMinePlayer prisonMinePlayer = PrisonMinePlayerManager.getByUUID(event.getPlayer().getUniqueId());
        prisonMinePlayer.addBlocksMined(1);


        int points = ThanksgivingEvent.getOkaeriConfig().blockValues
                .getOrDefault(event.getBlock().getType(), 1);
        prisonMinePlayer.addPoints(points);

        event.getBlockBreakEvent().setDropItems(false);
//        Block block = event.getBlock();
//
//        if (block.getDrops().isEmpty()) {
//            return;
//        }
//
//        ItemStack drop = block.getDrops().stream().toList().getFirst();
//        Util.setPersistentKey(drop, DROP_KEY, PersistentDataType.BOOLEAN, true);
//        drop.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
//        for (int i = 0; i < points; i++) {
//            block.getLocation().getWorld().dropItemNaturally(block.getLocation(), drop);
//        }
    }

//    @EventHandler
//    public void onPlayerPickupItem(EntityPickupItemEvent event) {
//        ItemStack item = event.getItem().getItemStack();
//        if (!Util.hasPersistentKey(item, DROP_KEY)) {
//            return;
//        }
//
//        // set the item to nothing
//        event.getItem().setItemStack(AIR);
//        if (event.getEntity() instanceof Player player) {
//            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.7f);
//        }
//    }
}
