package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.EventMain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public record ArtifactEntity(Interaction interaction, ItemDisplay itemDisplay, long spawnTimestamp) {

    private static final float INTERACTION_HITBOX = 1f;
    private static final NamespacedKey ENTITY_TAG = NamespacedKey.fromString("manor2/entity", EventMain.getInstance());
    private static final NamespacedKey LINKED_ENTITY = NamespacedKey.fromString("manor2/entity_link", EventMain.getInstance());
    private static final String ARTIFACT_ID = "artifact";

    public boolean isDead() {
        return interaction.isDead() || itemDisplay.isDead();
    }

    public void remove() {
        interaction.remove();
        itemDisplay.remove();
    }

    public ArtifactEntity withTimeStamp(long newTimestamp) {
        return new ArtifactEntity(interaction, itemDisplay, newTimestamp);
    }

    public static ArtifactEntity spawn(Location location, ArtifactType artifactType, long spawnTimestamp) {
        ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class, itemDisplayPreSpawn -> {
            itemDisplayPreSpawn.setItemStack(artifactType.toItem());
            itemDisplayPreSpawn.setPersistent(false);
            itemDisplayPreSpawn.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        });
        Interaction interaction = location.getWorld().spawn(location, Interaction.class, interactionPreSpawn -> {
            interactionPreSpawn.setPersistent(false);
            interactionPreSpawn.setInteractionWidth(INTERACTION_HITBOX);
            interactionPreSpawn.setInteractionHeight(INTERACTION_HITBOX);
            interactionPreSpawn.getPersistentDataContainer().set(ENTITY_TAG, PersistentDataType.STRING, ARTIFACT_ID);
            interactionPreSpawn.getPersistentDataContainer().set(LINKED_ENTITY, PersistentDataType.STRING, itemDisplay.getUniqueId().toString());
        });
        return new ArtifactEntity(interaction, itemDisplay, spawnTimestamp);
    }

    public static Optional<ItemStack> fromEntity(Entity entity) {
        if (!(entity instanceof Interaction interaction)) {
            return Optional.empty();
        }
        if (!ARTIFACT_ID.equalsIgnoreCase(interaction.getPersistentDataContainer().get(ENTITY_TAG, PersistentDataType.STRING)) ||
                !(interaction.getPersistentDataContainer().get(LINKED_ENTITY, PersistentDataType.STRING) instanceof String string) ||
                !(Bukkit.getEntity(UUID.fromString(string)) instanceof ItemDisplay itemDisplay)
        ) {
            return Optional.empty();
        }
        return Optional.of(itemDisplay.getItemStack());
    }
}
