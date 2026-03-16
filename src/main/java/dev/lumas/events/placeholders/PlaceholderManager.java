package dev.lumas.events.placeholders;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.PlaceholderMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.placeholder.AbstractPlaceholderManager;
import dev.lumas.events.EventMain;

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
        identifier = "events",
        author = "Jsinco",
        version = "1.0"
)
public class PlaceholderManager extends AbstractPlaceholderManager<EventMain, PlaceholderModule> {
    public PlaceholderManager() {
        super(EventMain.getInstance());
    }
}
