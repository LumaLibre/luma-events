package dev.jsinco.luma.lumaevents.items;

import dev.jsinco.luma.api.LumaItemsAPI;

public class CustomItemsManager {

    public static PresentItem presentItem = new PresentItem();
    public static HolidayCandleItem holidayCandleItem = new HolidayCandleItem();


    public static void register() {
        LumaItemsAPI.getInstance().registerCustomItem(presentItem);
        LumaItemsAPI.getInstance().registerCustomItem(holidayCandleItem);
    }
}
