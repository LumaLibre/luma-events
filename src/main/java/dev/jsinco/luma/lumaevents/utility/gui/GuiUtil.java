package dev.jsinco.luma.lumaevents.utility.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener.CustomStackNameSpace;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiUtil {

    private static final Map<ItemStack, int[]> defaultItems = new HashMap<>();
    private static final UUID STATIC_UUID = UUID.fromString("dac456f8-bf29-40ce-9373-96947782b57f");

    static {
        defaultItems.put(borderItem(Material.GREEN_STAINED_GLASS_PANE), new int[]{0, 8, 45, 53});
        defaultItems.put(borderItem(Material.SHORT_GRASS), new int[]{1, 7, 46, 52});
        defaultItems.put(borderItem(Material.FERN), new int[]{2, 6, 47, 51});
        defaultItems.put(borderItem(Material.PINK_TULIP), new int[]{3, 5, 48, 50});
        defaultItems.put(borderItem(Material.LILY_PAD), new int[]{4, 49});
    }

    public static Inventory getBaseInv(InventoryHolder holder, int size, String title) {
        return getBaseInv(holder, size, Util.color(title));
    }

    public static Inventory getBaseInv(InventoryHolder holder, int size, Component title) {
        // When inv size is 54, do not modify any slot values.
        // When inv size is 45, modify the second half of the values by -9.
        // So, {0, 8, 45, 53} becomes {0, 8, 36, 44}
        // and: {4, 49} becomes {4, 40}

        Inventory inv = Bukkit.createInventory(holder, size, title);
        if (size < 18) { // Inventory size is too small for us to put our borders.
            return inv;
        }

        for (Map.Entry<ItemStack, int[]> entry : defaultItems.entrySet()) {
            ItemStack item = entry.getKey();
            int[] slots = entry.getValue();
            for (int i = 0; i < slots.length; i++) {
                if (size == 54) {
                    inv.setItem(slots[i], item);
                } else {
                    int factor = 54 - size;
                    // split list in half
                    // and subtract 9 from the second half
                    if (i < slots.length / 2) {
                        inv.setItem(slots[i], item);
                    } else {
                        inv.setItem(slots[i] - factor, item);
                    }
                }
            }
        }
        return inv;
    }

    public static ItemStack borderItem(Material m) {
        return item(m, false, "<black>");
    }

    public static ItemStack playerHead(String base64, String name, String... lore) {
        ItemStack itemStack = item(Material.PLAYER_HEAD, false, name, lore);
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(STATIC_UUID);
        profile.getProperties().add(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack item(Material m, boolean glint, String name, String... lore) {
        ItemStack item = new ItemStack(m);
        return createItemStack(glint, name, item, lore);
    }

    public static ItemStack item(CustomStackNameSpace csns, boolean glint, String name, String... lore) {
        ItemStack item = IAItemStacksListener.getCachedIAStack(csns);
        if (item == null) {
            return item(Material.BARRIER, false, "<red>Item not found");
        }
        return createItemStack(glint, name, item, lore);
    }

    @NotNull
    private static ItemStack createItemStack(boolean glint, String name, ItemStack item, String[] lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Util.color(name));
        List<Component> loreList = new ArrayList<>();
        for (String s : lore) {
            loreList.add(Util.color(s));
        }
        meta.lore(loreList);
        if (glint) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }


    public static List<String> formatLore(String input) {
        String[] words = input.trim().split("\\s+"); // Split by one or more spaces
        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        int count = 0;

        for (String word : words) {
            if (count >= 5) {
                chunks.add(chunk.toString().trim());
                chunk.setLength(0);
                count = 0;
            }
            chunk.append(word).append(" ");
            count++;
        }

        // Add remaining words if any
        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }

    public static List<String> formatLore(String[] inputs) {
        List<String> chunks = new ArrayList<>();
        for (String input : inputs) {
            chunks.addAll(formatLore(input));
        }
        return chunks;
    }

    private static int getWordCount(String str) {
        return str.split(" ").length;
    }

    public static ItemStack guiArrow(GuiArrow type) {
        return switch (type) {
            case LEFT -> item(Material.ARROW, false, "<b>Previous");
            case RIGHT -> item(Material.ARROW, false, "<b>Next");
        };
    }

    public enum GuiArrow {
        LEFT,
        RIGHT
    }
}
