package dev.jsinco.luma.lumaevents.placeholders;

import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.manager.placeholder.AbstractPlaceholderManager;
import dev.lumas.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "events",
        author = "Jsinco",
        version = "1.0"
)
public class PlaceholderManager extends AbstractPlaceholderManager<EventMain, PlaceholderModule> {
    public PlaceholderManager() {
        super(EventMain.getInstance());
    }
}
