package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.gamingmesh.jobs.api.JobsPaymentEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListeners;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "Jobs")
public class JobsListeners extends ExplorerListeners {

    @EventHandler
    public void onJobsPayment(JobsPaymentEvent event) {
        fire(event, event.getPlayer().getUniqueId());
    }
}
