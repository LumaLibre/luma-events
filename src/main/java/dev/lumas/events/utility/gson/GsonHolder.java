package dev.lumas.events.utility.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumas.events.explorer.mile.ActiveExplorerMile;
import dev.lumas.events.explorer.order.ActiveOrder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Modifier;

public class GsonHolder {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ActiveExplorerMile.class, new ActiveExplorerMile.GsonTypeAdapter())
            .registerTypeAdapter(ItemStack[].class, new ItemStackArrayAdapter())
            .registerTypeAdapter(ActiveOrder.class, new ActiveOrder.GsonTypeAdapter())
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .setPrettyPrinting()
            .create();
}
