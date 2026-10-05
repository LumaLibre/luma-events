package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.utility.Util;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public final class ArtifactType {

    private ItemType itemType;
    private String displayName;
    private List<String> lore;

    public ArtifactType() {
        // NO-OP: For deserialization
    }

    public ArtifactType(ItemType itemType, String displayName, List<String> lore) {
        this.itemType = itemType;
        this.displayName = displayName;
        this.lore = lore;
    }

    public ItemStack toItem() {
        ItemStack itemStack = itemType.createItemStack(1);
        itemStack.setData(DataComponentTypes.CUSTOM_NAME, Util.color(displayName));
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lore.stream()
                .map(Util::color)
                .toList())
        );
        itemStack.editPersistentDataContainer(pdc ->
                pdc.set(Manor2.ITEM_TAG, PersistentDataType.STRING, "artifact")
        );
        return itemStack;
    }

}
