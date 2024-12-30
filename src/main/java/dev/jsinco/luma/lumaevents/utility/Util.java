package dev.jsinco.luma.lumaevents.utility;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.jsinco.luma.lumaevents.EventMain;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class Util {

    private static final String PREFIX = "<b><#9b8aac>❄</#9b8aac> <#CCD8E9>Event</#CCD8E9></b> <dark_gray>»</dark_gray> ";

    // TODO: I want to use small font
    public static void sendMsg(CommandSender receiver, String message) {
        receiver.sendMessage(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString("#C0D6F0")));
    }

    public static Component color(String string) {
        return MiniMessage.miniMessage().deserialize("<!i>" + string);
    }

    public static List<Component> color(List<String> strings) {
        return strings.stream().map(Util::color).toList();
    }

    public static <P, C> C getPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType) {
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(EventMain.getInstance(), strKey), dataType);
    }

    public static <P, C> void setPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType, C value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(EventMain.getInstance(), strKey), dataType, value);
        item.setItemMeta(meta);
    }
    
    
    public static boolean hasPersistentKey(ItemStack item, String strKey) {
        return hasPersistentKey(item, new NamespacedKey(EventMain.getInstance(), strKey));
    }
    
    public static boolean hasPersistentKey(ItemStack item, String strKey, PersistentDataType<?, ?> dataType) {
        return hasPersistentKey(item, new NamespacedKey(EventMain.getInstance(), strKey), dataType);
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

    public static ItemStack createBasicItem(Material material, String name, boolean glint, List<String> lore, List<String> datas) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(color(name));
        meta.lore(lore.stream().map(Util::color).toList());
        for (String data : datas) {
            meta.getPersistentDataContainer().set(new NamespacedKey(EventMain.getInstance(), data), PersistentDataType.SHORT, (short) 1);
        }
        if (glint) {
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static void setPlayerHead(ItemStack item, String b64) {
        if (item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(new ProfileProperty("textures", b64));
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
    }

    public static <T> T getRandom(Collection<T> collection) {
        int index = (int) (Math.random() * collection.size());
        return collection.stream().skip(index).findFirst().orElse(null);
    }

    @Nullable
    public static <E extends Enum<E>> E getEnumFromString(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
