package dev.lumas.events.utility.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import dev.lumas.events.explorer.order.ActiveExplorerOrder;
import dev.lumas.events.manager.EventTeamManager;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Modifier;

public class GsonHolder {

    public static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(ActiveExplorerMile.class, new ActiveExplorerMile.GsonTypeAdapter())
                .registerTypeAdapter(ItemStack[].class, new ItemStackArrayAdapter())
                .registerTypeAdapter(ActiveExplorerOrder.class, new ActiveExplorerOrder.GsonTypeAdapter())
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .setPrettyPrinting();

        // teams
        for (EventTeamManager.Provider provider : EventTeamManager.Provider.values()) {
            builder.registerTypeAdapter(provider.getTeamClass(), (InstanceCreator<?>) type -> provider.newInstance());
        }

        GSON = builder.create();
    }
}
