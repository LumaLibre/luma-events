package dev.lumas.events.utility;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumas.events.EventMain;
import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Util {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ActiveExplorerMile.class, new ActiveExplorerMile.GsonTypeAdapter())
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .setPrettyPrinting()
            .create();
    public static final Random RANDOM = new Random();
    public static final String PREFIX = "<b><gradient:#954381:#ee78c0:#ec6e95:#cb354e>Event</gradient></b> <dark_gray>»</dark_gray> ";
    public static final String TEXT_COLOR = "#EC6E95";


    public static void sendMsg(CommandSender receiver, String message) {
        if (receiver == null) {
            return;
        }
        receiver.sendMessage(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString(TEXT_COLOR)));
    }

    public static void sendMsg(Audience audience, String message) {
        audience.sendMessage(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString(TEXT_COLOR)));
    }

    public static void broadcast(String message) {
        Bukkit.broadcast(color(PREFIX + message).colorIfAbsent(TextColor.fromHexString(TEXT_COLOR)));
    }

    public static void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static Component prefixed(String message) {
        return color(PREFIX + message).colorIfAbsent(TextColor.fromHexString(TEXT_COLOR));
    }

    public static Component color(String string) {
        return MiniMessage.miniMessage().deserialize("<!i>" + string);
    }

    public static Component color(String string, TextColor ifAbsentColor) {
        return color(string).colorIfAbsent(ifAbsentColor);
    }

    public static List<Component> color(List<String> strings) {
        return strings.stream().map(Util::color).toList();
    }

    public static List<Component> color(TextColor ifAbsent, String... strings) {
        return Arrays.stream(strings).map(string -> {
            Component component = color(string);
            return component.colorIfAbsent(ifAbsent);
        }).toList();
    }

    public static List<Component> color(List<String> strings, TextColor textColor) {
        return strings.stream().map(string -> {
            Component component = color(string);
            return component.colorIfAbsent(textColor);
        }).toList();
    }

    public static Title title(String title, String subtitle) {
        return Title.title(Util.color(title), Util.color(subtitle));
    }

    @Nullable
    public static <P, C> C getPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType) {
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(EventMain.getInstance(), strKey), dataType);
    }

    @Nullable
    public static <P, C> C getPersistentKey(PersistentDataHolder holder, String strKey, PersistentDataType<P, C> dataType) {
        return holder.getPersistentDataContainer().get(new NamespacedKey(EventMain.getInstance(), strKey), dataType);
    }

    public static <P, C> void setPersistentKey(ItemStack item, String strKey, PersistentDataType<P, C> dataType, C value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(EventMain.getInstance(), strKey), dataType, value);
        item.setItemMeta(meta);
    }

    public static <P, C> void setPersistentKey(PersistentDataHolder holder, String strKey, PersistentDataType<P, C> dataType, C value) {
        holder.getPersistentDataContainer().set(new NamespacedKey(EventMain.getInstance(), strKey), dataType, value);
    }


    public static boolean hasPersistentKey(PersistentDataHolder holder, String strKey) {
        return holder.getPersistentDataContainer().has(new NamespacedKey(EventMain.getInstance(), strKey));
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

    public static void giveItem(Player player, ItemStack itemStack, int amount) {
        giveItem(player, itemStack.asQuantity(amount));
    }

    public static boolean takeItem(Player player, ItemStack itemStack) {
        int amount = itemStack.getAmount();
        return takeItem(player, itemStack, amount);
    }

    public static void giveItem(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Executors.runSync(player, () -> {
            Map<Integer, ItemStack> didntFit = player.getInventory().addItem(itemStack);
            if (!didntFit.isEmpty()) {
                for (ItemStack itemStack1 : didntFit.values()) {
                    player.getWorld().dropItem(player.getLocation(), itemStack1);
                }
            }
        });
    }

    public static boolean takeItem(Player player, ItemStack itemStack, int amount) {
        if (player == null) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        if (!inventory.containsAtLeast(itemStack, amount)) {
            return false;
        }

        Map<Integer, ItemStack> couldNotRemove = inventory.removeItemAnySlot(itemStack.asQuantity(amount));
        if (couldNotRemove.isEmpty()) {
            return true;
        }
        throw new RuntimeException("Failed to remove: " + couldNotRemove + " from " + player.getName() + "'s inventory!");
    }

    public static boolean takeItem(Player player, NamespacedKey namespacedKey, int amount) {
        if (player == null) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();

        int total = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null || !itemStack.hasItemMeta()) {
                continue;
            }

            if (itemStack.getPersistentDataContainer().has(namespacedKey)) {
                total += itemStack.getAmount();
            }
        }

        if (total < amount) {
            return false;
        }

        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null || !itemStack.hasItemMeta()) {
                continue;
            }

            if (itemStack.getPersistentDataContainer().has(namespacedKey)) {
                int toRemove = Math.min(itemStack.getAmount(), amount);
                itemStack.setAmount(itemStack.getAmount() - toRemove);
                amount -= toRemove;

                if (amount <= 0) {
                    return true;
                }
            }
        }

        throw new RuntimeException("Failed to remove: " + amount + "/" + namespacedKey + " from " + player.getName() + "'s inventory!");
    }

    public static String getTextColor() {
        return "<" + TEXT_COLOR + ">";
    }

    public static ItemStack createBasicItem(Material material, boolean glint) {
        return createBasicItem(material, "<gray>ItemStack", glint, new ArrayList<>(), new ArrayList<>());
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

    public static int ticksToSecs(long ticks) {
        return Math.toIntExact(ticks / 20);
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

    public static boolean hasCustomItem(ItemStack[] contents, String... ignored) {
        Plugin lumaitemsPluginInstance = Bukkit.getPluginManager().getPlugin("LumaItems");
        if (lumaitemsPluginInstance == null) {
            throw new IllegalStateException("LumaItems instance not found!");
        }
        NamespacedKey lumaitemsKey = new NamespacedKey(lumaitemsPluginInstance, "lumaitem");
        List<NamespacedKey> ignoredKeys = Arrays.stream(ignored).map(it -> new NamespacedKey(lumaitemsPluginInstance, it)).toList();
        for (ItemStack itemStack : contents) {
            if (itemStack == null || !itemStack.hasItemMeta()) continue;

            ItemMeta itemMeta = itemStack.getItemMeta();

            if (!ignoredKeys.isEmpty() && ignoredKeys.stream().anyMatch(key -> itemMeta.getPersistentDataContainer().has(key))) {
                continue;
            }

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

    public static ItemStack createItem(Material material, EditMeta editMeta) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        editMeta.edit(itemMeta);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static Color bukkitToAwtColor(org.bukkit.Color bukkitColor) {
        return new Color(bukkitColor.getRed(), bukkitColor.getGreen(), bukkitColor.getBlue());
    }

    public static Class<?> classForNameOr(String className, Class<?> defaultClass) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return defaultClass;
        }
    }

    public static String formatMaterialName(String s) {
        return formatSnakeCase(s);
    }

    public static String formatSnakeCase(String input) {
        String[] words = input.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1));
            }
        }
        return result.toString();
    }


    public static ChatColor chatColorFromNamedTextColor(NamedTextColor c1) {
        ChatColor chatColor = getEnumFromString(ChatColor.class, c1.toString().toUpperCase());
        if (chatColor != null) {
            return chatColor;
        }
        return ChatColor.GRAY;
    }

    public static boolean isAssignableFromAny(Class<?> target, Class<?>... candidates) {
        for (Class<?> candidate : candidates) {
            if (target.isAssignableFrom(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean isAssignableFromAny(Class<?> target, Set<Class<T>> candidates) {
        for (Class<?> candidate : candidates) {
            if (target.isAssignableFrom(candidate)) {
                return true;
            }
        }
        return false;
    }
}
