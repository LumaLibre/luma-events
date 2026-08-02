package dev.lumas.events.items;

import dev.lumas.events.EventMain;
import dev.lumas.lumaitems.api.LumaItemsAPI;
import dev.lumas.lumaitems.model.item.CustomItem;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocalCustomItemManager {

    private static final LumaItemsAPI lumaItemsAPI = LumaItemsAPI.getInstance();
    @Getter
    private static final List<CustomItem> customItems = new ArrayList<>();


    public static void addCustomItem(CustomItem customItem) {
        customItems.add(customItem);
    }

    public static void registerCustomItems() {
        for (CustomItem customItem : customItems) {
            lumaItemsAPI.registerCustomItem(customItem);
            if (customItem instanceof CustomItemFunctionsWithRecipe withRecipe) {
                var pair = withRecipe.recipe();
                // Folia safety
                /*
                if (Bukkit.getRecipe(pair.getFirst()) != null) {
                    Bukkit.removeRecipe(pair.getFirst());
                }
                Bukkit.addRecipe(pair.getSecond());
                 */
                if (Bukkit.getRecipe(pair.getFirst()) == null) {
                    EventMain.getInstance().getLogger().info("Adding Recipe for " + withRecipe);
                    Bukkit.addRecipe(pair.getSecond());
                }
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
