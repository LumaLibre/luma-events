package dev.lumas.events.utility.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ItemStackArrayAdapter extends TypeAdapter<ItemStack[]> {

    @Override
    public void write(JsonWriter out, ItemStack[] items) throws IOException {
        if (items == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        for (ItemStack item : items) {
            if (item == null) {
                out.nullValue();
            } else {
                out.value(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }
        out.endArray();
    }

    @Override
    public ItemStack[] read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        List<ItemStack> items = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                items.add(null);
            } else {
                byte[] bytes = Base64.getDecoder().decode(in.nextString());
                items.add(ItemStack.deserializeBytes(bytes));
            }
        }
        in.endArray();
        return items.toArray(new ItemStack[0]);
    }
}
