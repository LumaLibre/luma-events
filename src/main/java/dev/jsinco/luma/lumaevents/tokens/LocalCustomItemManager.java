package dev.jsinco.luma.lumaevents.tokens;

import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;
import dev.jsinco.luma.lumaitems.manager.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocalCustomItemManager {

    private static final LumaItemsAPI lumaItemsAPI = LumaItemsAPI.getInstance();
    private static final List<CustomItem> customItems = new ArrayList<>();


    public static void addCustomItem(CustomItem customItem) {
        customItems.add(customItem);
        lumaItemsAPI.registerCustomItem(customItem);
    }

    public static void registerCustomItems() {
        for (CustomItem customItem : customItems) {
            lumaItemsAPI.registerCustomItem(customItem);

            if (customItem instanceof CustomItemFunctionsWithRecipe withRecipe) {
                var pair = withRecipe.recipe();
                if (Bukkit.getRecipe(pair.getFirst()) != null) {
                    Bukkit.removeRecipe(pair.getFirst());
                }
                Bukkit.addRecipe(pair.getSecond());
            }
        }
    }

    @Nullable
    public static <T extends CustomItem> T getCustomItem(Class<T> clazz) {
        for (CustomItem item : customItems) {
            if (item.getClass().equals(clazz)) {
                return (T) item;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack getCustomItemStack(Class<? extends CustomItem> clazz) {
        CustomItem customItem = getCustomItem(clazz);
        if (customItem != null) {
            return customItem.createItem().getSecond();
        }
        return null;
    }

}
