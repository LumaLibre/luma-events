package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.gmail.nossr50.api.TreeFellerBlockBreakEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.AbstractExplorerListener;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "mcMMO")
public class McMMOListeners extends AbstractExplorerListener {

    @EventHandler
    public void onTreeFellerBlockBreak(TreeFellerBlockBreakEvent event) {
        fire(event, event.getPlayer());
    }
}
