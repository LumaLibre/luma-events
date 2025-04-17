package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.dre.brewery.api.events.IngedientAddEvent;
import com.dre.brewery.api.events.PlayerChatDistortEvent;
import com.dre.brewery.api.events.PlayerPukeEvent;
import com.dre.brewery.api.events.PlayerPushEvent;
import com.dre.brewery.api.events.brew.BrewDrinkEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.AbstractExplorerListener;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "BreweryX")
public class BreweryXListeners extends AbstractExplorerListener {

    @EventHandler
    public void onIngredientAdd(IngedientAddEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerPuke(PlayerPukeEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerStumble(PlayerPushEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerChatDistort(PlayerChatDistortEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onBrewDrink(BrewDrinkEvent event) {
        fire(event, event.getPlayer());
    }

}
