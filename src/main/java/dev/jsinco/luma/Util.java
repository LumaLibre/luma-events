package dev.jsinco.luma;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Util {

    public static void sendMsg(CommandSender receiver, String message) {
        receiver.sendMessage(color(ThanksgivingEvent.getOkaeriConfig().prefix + message));
    }

    public static Component color(String string) {
        return MiniMessage.miniMessage().deserialize("<!i>" + string);
    }

    public static List<Component> color(List<String> strings) {
        return strings.stream().map(Util::color).toList();
    }

    public static <P, C> C getPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType) {
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(ThanksgivingEvent.getInstance(), strKey), dataType);
    }

    public static <P, C> void setPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType, C value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(ThanksgivingEvent.getInstance(), strKey), dataType, value);
        item.setItemMeta(meta);
    }
    
    
    public static boolean hasPersistentKey(ItemStack item, String strKey) {
        return hasPersistentKey(item, new NamespacedKey(ThanksgivingEvent.getInstance(), strKey));
    }
    
    public static boolean hasPersistentKey(ItemStack item, String strKey, PersistentDataType<?, ?> dataType) {
        return hasPersistentKey(item, new NamespacedKey(ThanksgivingEvent.getInstance(), strKey), dataType);
    }
    
    public static boolean hasPersistentKey(ItemStack item, NamespacedKey key) {
        return item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key);
    }
    
    public static boolean hasPersistentKey(ItemStack item, NamespacedKey key, PersistentDataType<?, ?> dataType) {
        return item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, dataType);
    }



    public static void giveItem(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        boolean itemAdded = false;

        for (int i = 0; i < 36; i++) {  // Inventory slots 0 to 35
            if (inventory.getItem(i) == null || inventory.getItem(i).isSimilar(item)) {
                inventory.addItem(item);  // Add the item to the inventory
                itemAdded = true;
                break;
            }
        }
        // If item wasn't added (inventory full or no matching slots), drop the item at the player's location
        if (!itemAdded) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
