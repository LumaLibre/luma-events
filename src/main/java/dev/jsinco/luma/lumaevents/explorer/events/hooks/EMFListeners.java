package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.oheers.fish.api.EMFFishEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListener;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "EvenMoreFish")
public class EMFListeners extends ExplorerListener {

    @EventHandler
    public void onEMFFish(EMFFishEvent event) {
        fire(event, event.getPlayer());
    }
}
