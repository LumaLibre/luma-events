package dev.jsinco.luma.lumaevents.placeholders;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.AbstractPlaceholderManager;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "valentide",
        author = "Jsinco",
        version = "1.0"
)
public class PlaceholderManager extends AbstractPlaceholderManager<EventMain, PlaceholderModule> {
    public PlaceholderManager() {
        super(EventMain.getInstance());
    }
}
