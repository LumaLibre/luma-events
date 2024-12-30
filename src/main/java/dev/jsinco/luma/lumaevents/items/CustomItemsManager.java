package dev.jsinco.luma.lumaevents.items;

import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;

public class CustomItemsManager {

    public static PresentItem presentItem = new PresentItem();
    public static HolidayCandleItem holidayCandleItem = new HolidayCandleItem();
    public static ArchiveOfAstralisRerollItem archiveOfAstralisRerollItem = new ArchiveOfAstralisRerollItem();



    public static void register() {
        LumaItemsAPI.getInstance().registerCustomItem(presentItem);
        LumaItemsAPI.getInstance().registerCustomItem(holidayCandleItem);
        LumaItemsAPI.getInstance().registerCustomItem(archiveOfAstralisRerollItem);
    }

}
