package dev.jsinco.luma;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class Util {

    public static void sendMsg(CommandSender receiver, String message) {
        receiver.sendMessage(color(ThanksgivingEvent.getOkaeriConfig().getPrefix() + message));
    }

    public static Component color(String string) {
        return MiniMessage.miniMessage().deserialize("<!i>" + string);
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
}
