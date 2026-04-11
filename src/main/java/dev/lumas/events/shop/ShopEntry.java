package dev.lumas.events.shop;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ShopEntry {

    record Item(
            String lumaItemId,
            int price,
            int globalStock,
            int maxPerPlayer,
            @Nullable String displayName,
            List<String> lore
    ) implements ShopEntry {}

    record Decoration(
            Material material,
            @Nullable String displayName,
            boolean hideTooltip
    ) implements ShopEntry {}
}
