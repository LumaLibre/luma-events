package dev.jsinco.luma.lumaevents.explorer.events;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@AutoRegister(RegisterType.LISTENER)
public class IAItemStacksListener implements Listener {

    private static final Map<CustomStackNameSpace, CachedIAItemStack> cachedIAItemStacks = new HashMap<>();

    {
        this.cacheFromItemsAdder();
    }

    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent event) {
        this.cacheFromItemsAdder();
    }

    private void cacheFromItemsAdder() {
        for (CustomStackNameSpace customStackNameSpace : CustomStackNameSpace.values()) {
            CustomStack customStack = CustomStack.getInstance(customStackNameSpace.getNamespace());
            if (customStack != null) {
                ItemStack itemStack = customStack.getItemStack();
                if (itemStack == null) {
                    Util.log("ItemStack is null for " + customStackNameSpace.getNamespace());
                    continue;
                }
                Material material = itemStack.getType();
                int customModelData = itemStack.getItemMeta().getCustomModelData();
                cachedIAItemStacks.put(customStackNameSpace, new CachedIAItemStack(material, customModelData));
            } else {
                Util.log("CustomStack is null for " + customStackNameSpace.getNamespace());
            }
        }
    }

    @Nullable
    public static ItemStack getCachedIAStack(CustomStackNameSpace postCard) {
        CachedIAItemStack cachedIAItemStack = cachedIAItemStacks.get(postCard);
        if (cachedIAItemStack == null) {
            return null;
        }
        ItemStack itemStack = new ItemStack(cachedIAItemStack.getMaterial());
        itemStack.editMeta(meta -> meta.setCustomModelData(cachedIAItemStack.getCustomModelData()));
        return itemStack;
    }



    @Getter
    @ToString
    @AllArgsConstructor
    public static class CachedIAItemStack {

        // The only two values I care about.
        private final Material material;
        private final int customModelData;
    }


    @Getter
    @AllArgsConstructor
    public enum CustomStackNameSpace {
        POSTCARD_CASTLE_NO_ART("shizuart_furnitures:postcard_castle"),
        POSTCARD_CITY_NO_ART("shizuart_furnitures:postcard_city"),
        POSTCARD_MOUNTAINS_NO_ART("shizuart_furnitures:postcard_mountains"),
        POSTCARD_OCEAN_NO_ART("shizuart_furnitures:postcard_ocean"),
        POSTCARD_PLAINS_NO_ART("shizuart_furnitures:postcard_plains"),
        POSTCARD_VOLCANO_NO_ART("shizuart_furnitures:postcard_volcan"),
        POSTCARD_CASTLE("shizuart_furnitures:postcard_castle2"),
        POSTCARD_CITY("shizuart_furnitures:postcard_city2"),
        POSTCARD_MOUNTAINS("shizuart_furnitures:postcard_mountains2"),
        POSTCARD_OCEAN("shizuart_furnitures:postcard_ocean2"),
        POSTCARD_PLAINS("shizuart_furnitures:postcard_plains2"),
        POSTCARD_VOLCANO("shizuart_furnitures:postcard_volcan2"),
        POSTCARD_STAND("shizuart_furnitures:postcards_stand"),
        POSTCARD_PILE("shizuart_furnitures:postcards_pile");

        private final String namespace;

        public static final CustomStackNameSpace[] POSTCARDS_NO_ART = {
                POSTCARD_CASTLE_NO_ART,
                POSTCARD_CITY_NO_ART,
                POSTCARD_MOUNTAINS_NO_ART,
                POSTCARD_OCEAN_NO_ART,
                POSTCARD_PLAINS_NO_ART,
                POSTCARD_VOLCANO_NO_ART
        };
        public static final CustomStackNameSpace[] POSTCARDS = {
                POSTCARD_CASTLE,
                POSTCARD_CITY,
                POSTCARD_MOUNTAINS,
                POSTCARD_OCEAN,
                POSTCARD_PLAINS,
                POSTCARD_VOLCANO
        };
        public static final CustomStackNameSpace[] POSTCARDS_ETC = {
                POSTCARD_STAND,
                POSTCARD_PILE
        };
    }
}
