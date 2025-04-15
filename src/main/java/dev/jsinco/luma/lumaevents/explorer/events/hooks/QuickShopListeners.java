package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListener;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "QuickShop-Hikari")
public class QuickShopListeners extends ExplorerListener {

    @EventHandler
    public void onShopSuccessfulTransaction(ShopSuccessPurchaseEvent event) {
        fire(event, event.getPurchaser().getUniqueId());
    }

}
