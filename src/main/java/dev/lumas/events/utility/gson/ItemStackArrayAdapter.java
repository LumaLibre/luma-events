package dev.lumas.events.utility.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Base64;

public class ItemStackArrayAdapter extends TypeAdapter<ItemStack[]> {

    @Override
    public void write(JsonWriter out, ItemStack[] items) throws IOException {
        if (items == null) {
            out.nullValue();
            return;
        }

        byte[] serialized = ItemStack.serializeItemsAsBytes(items);
        String serializedString = Base64.getEncoder().encodeToString(serialized);
        out.value(serializedString);
    }

    @Override
    public ItemStack[] read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String serializedString = in.nextString();
        if (serializedString.isEmpty()) {
            return new ItemStack[0];
        }

        byte[] serialized = Base64.getDecoder().decode(serializedString);
        ItemStack[] items = ItemStack.deserializeItemsFromBytes(serialized);
        return items;
    }
}
