package dev.lumas.events.utility.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.lumas.events.utility.Util;
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
import java.util.List;
import java.util.UUID;

public class GuiUtil {

    private static final UUID STATIC_UUID = UUID.fromString("dac456f8-bf29-40ce-9373-96947782b57f");


    public record BorderEntry(ItemStack item, int[] slots) {}

    public static final List<BorderEntry> DEFAULT_BORDER = List.of(
            new BorderEntry(borderItem(Material.GREEN_STAINED_GLASS_PANE, false), new int[]{0, 8, 45, 53}),
            new BorderEntry(borderItem(Material.SHORT_GRASS, false), new int[]{1, 7, 46, 52}),
            new BorderEntry(borderItem(Material.FERN, false), new int[]{2, 6, 47, 51}),
            new BorderEntry(borderItem(Material.PINK_TULIP, false), new int[]{3, 5, 48, 50}),
            new BorderEntry(borderItem(Material.LILY_PAD, false), new int[]{4, 49})
    );

    public static final List<BorderEntry> PALE_SIDE_BORDER = List.of(
            new BorderEntry(borderItem(Material.GRAY_STAINED_GLASS_PANE, true), new int[]{0, 8, 45, 53}),
            new BorderEntry(borderItem(Material.DEAD_FIRE_CORAL_FAN, true), new int[]{1, 7, 46, 52}),
            new BorderEntry(borderItem(Material.OPEN_EYEBLOSSOM, true), new int[]{2, 6, 47, 51}),
            new BorderEntry(borderItem(Material.DEAD_FIRE_CORAL_FAN, true), new int[]{3, 5, 48, 50}),
            new BorderEntry(borderItem(Material.PALE_OAK_SAPLING, true), new int[]{4, 49})
    );

    public static Inventory getBaseInv(InventoryHolder holder, int size, String title) {
        return getBaseInv(holder, size, Util.color(title), DEFAULT_BORDER);
    }

    public static Inventory getBaseInv(InventoryHolder holder, int size, Component title) {
        return getBaseInv(holder, size, title, DEFAULT_BORDER);
    }

    public static Inventory getBaseInv(InventoryHolder holder, int size, String title, List<BorderEntry> border) {
        return getBaseInv(holder, size, Util.color(title), border);
    }

    public static Inventory getBaseInv(InventoryHolder holder, int size, Component title, List<BorderEntry> border) {
        Inventory inv = Bukkit.createInventory(holder, size, title);
        if (size < 18) { // Too small for borders
            return inv;
        }

        applyBorder(inv, size, border);
        return inv;
    }

    public static Inventory getPaleSideInv(InventoryHolder holder, String title) {
        return getBaseInv(holder, 54, Util.color(title), PALE_SIDE_BORDER);
    }

    public static Inventory getPaleSideInv(InventoryHolder holder, Component title) {
        return getBaseInv(holder, 54, title, PALE_SIDE_BORDER);
    }


    /**
     * Applies a border config to an inventory. Slot arrays are authored for size 54;
     * for smaller inventories, the second half of each slot array (assumed bottom row)
     * is shifted up by (54 - size).
     */
    private static void applyBorder(Inventory inv, int size, List<BorderEntry> border) {
        int factor = 54 - size;
        for (BorderEntry entry : border) {
            int[] slots = entry.slots();
            ItemStack item = entry.item();
            int half = slots.length / 2;
            for (int i = 0; i < slots.length; i++) {
                int slot = (size == 54 || i < half) ? slots[i] : slots[i] - factor;
                if (slot >= 0 && slot < size) {
                    inv.setItem(slot, item);
                }
            }
        }
    }

    public static ItemStack borderItem(Material m) {
        return borderItem(m, false);
    }

    public static ItemStack borderItem(Material m, boolean hideTooltip) {
        ItemStack stack = item(m, false, "<black>");
        if (hideTooltip) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setHideTooltip(true);
                stack.setItemMeta(meta);
            }
        }
        return stack;
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
        String[] words = input.trim().split("\\s+");
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