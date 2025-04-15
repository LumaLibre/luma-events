package dev.jsinco.luma.lumaevents.utility;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.explorer.ActiveExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.custom.EarnTokenExplorerEvent;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListeners;
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class Util {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ActiveExplorerMile.class, new ActiveExplorerMile.GsonTypeAdapter())
            .excludeFieldsWithModifiers(Modifier.STATIC)
            .setPrettyPrinting()
            .create();

    public static final String PREFIX = "<b><#954381>E<#EC60B0>v<#EE80C6>e<#C262A4>n<#954381>t</b> <dark_gray>»</dark_gray> ";

    public static void giveTokens(Player player, int amount) {
        EarnTokenExplorerEvent explorerEvent = new EarnTokenExplorerEvent(amount);
        ExplorerListeners.fire(explorerEvent, player.getUniqueId());
        // TODO: impl
    }

    public static void log(String msg) {
        sendMsg(Bukkit.getConsoleSender(), msg);
    }

    public static void sendMsg(CommandSender receiver, String message) {
        receiver.sendMessage(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString("#CBB6E9")));
    }

    public static void sendMsg(Audience audience, String message) {
        audience.sendMessage(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString("#CBB6E9")));
    }

    public static void broadcast(String message) {
        Bukkit.broadcast(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString("#CBB6E9")));
    }

    public static void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static Component color(String string) {
        return MiniMessage.miniMessage().deserialize("<!i>" + string);
    }

    public static List<Component> color(List<String> strings) {
        return strings.stream().map(Util::color).toList();
    }

    public static List<Component> colorList(List<String> strings, TextColor textColor) {
        return strings.stream().map(string -> {
            Component component = color(string);
            return component.colorIfAbsent(textColor);
        }).toList();
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

    public static <T> T getRandom(Collection<T> collection, Collection<T> collection2) {
        Collection<T> merged = new ArrayList<>(collection);
        merged.addAll(collection2);
        return getRandom(merged);
    }

    public static <T> T getRandom(T[] array) {
        return array[(int) (Math.random() * array.length)];
    }

    public static <T> T getRandom(List<T> list) {
        return list.get((int) (Math.random() * list.size()));
    }

    @Nullable
    public static <E extends Enum<E>> E getEnumFromString(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    public static String formatInt(int num) {
        return String.format("%,d", num);
    }

    public static long secsToMillis(long seconds) {
        return seconds * 1000;
    }

    public static int millisToSecs(long millis) {
        return (int) (millis / 1000);
    }

    public static int getInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static <T> String formatList(List<T> list, String objColor, String sepColor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(objColor).append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(sepColor).append(", ");
            }
        }
        return sb.toString();
    }

    public static <E extends Enum<E>> E getNextEnum(E current) {
        // Get next enum or first if at the end
        E[] values = current.getDeclaringClass().getEnumConstants();
        int nextIndex = (current.ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    public static boolean isItemsWithAttributes(ItemStack[] contents, Attribute... attributes) {
        for (ItemStack itemStack : contents) {
            if (!itemStack.hasItemMeta()) continue;

            ItemMeta itemMeta = itemStack.getItemMeta();
            if (!itemMeta.hasAttributeModifiers()) continue;

            for (Attribute attribute : attributes) {
                if (itemMeta.getAttributeModifiers(attribute) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasCustomItem(ItemStack[] contents) {
        Plugin lumaitemsPluginInstance = Bukkit.getPluginManager().getPlugin("LumaItems");
        if (lumaitemsPluginInstance == null) {
            throw new IllegalStateException("LumaItems instance not found!");
        }
        NamespacedKey lumaitemsKey = new NamespacedKey(lumaitemsPluginInstance, "lumaitem");
        for (ItemStack itemStack : contents) {
            if (itemStack == null || !itemStack.hasItemMeta()) continue;

            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.getPersistentDataContainer().has(lumaitemsKey)) {
                return true;
            }
        }
        return false;
    }




    public static void sleepThread(long millis) {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Cannot sleep on the main thread!");
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static ItemStack editMeta(ItemStack itemStack, EditMeta editMeta) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        editMeta.edit(itemMeta);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
