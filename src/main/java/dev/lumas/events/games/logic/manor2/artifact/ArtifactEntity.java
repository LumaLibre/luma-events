package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.roles.Artifact;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public record ArtifactEntity(Interaction interaction, ItemDisplay itemDisplay, long spawnTimestamp,
                             float startRotationRadians, @Nullable Artifact boundRole) {

    private static final float INTERACTION_HITBOX = 1f;
    private static final int ROTATION_INTERVAL_TICKS = 40;
    private static final NamespacedKey ENTITY_TAG = NamespacedKey.fromString("manor2/entity", EventMain.getInstance());
    private static final NamespacedKey LINKED_ENTITY = NamespacedKey.fromString("manor2/entity_link", EventMain.getInstance());
    private static final String ARTIFACT_ID = "artifact";
    private static final int ARTIFACT_SHOW_DURATION = 20 * 10;

    public CompletableFuture<Boolean> ensureValid(long time) {
        CompletableFuture<Boolean> validFuture = new CompletableFuture<>();
        interaction.getScheduler().run(EventMain.getInstance(), ignored -> {
            if (interaction.isDead() || itemDisplay.isDead() || spawnTimestamp() + ARTIFACT_SHOW_DURATION < time) {
                interaction.remove();
                itemDisplay.remove();
                validFuture.complete(false);
            } else {
                validFuture.complete(true);
            }
        }, () -> validFuture.complete(false));
        return validFuture;
    }


    public void remove() {
        interaction.getScheduler().run(EventMain.getInstance(), ignored -> {
            interaction.remove();
            itemDisplay.remove();
        }, null);
    }

    public ArtifactEntity withTimeStamp(long newTimestamp) {
        return new ArtifactEntity(interaction, itemDisplay, newTimestamp, startRotationRadians, boundRole);
    }

    public static ArtifactEntity spawn(Location location, ArtifactType artifactType, long spawnTimestamp, @Nullable Artifact boundRole) {
        Location spawnLocation = location.clone().add(0, 0.125, 0);
        spawnLocation.setYaw(0);
        spawnLocation.setPitch(0);
        float startRotationRadians = ThreadLocalRandom.current().nextFloat((float) (2 * Math.PI));
        ItemDisplay itemDisplay = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, itemDisplayPreSpawn -> {
            itemDisplayPreSpawn.setItemStack(artifactType.toItem());
            itemDisplayPreSpawn.setPersistent(false);
            itemDisplayPreSpawn.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            itemDisplayPreSpawn.setGlowing(true);
            Transformation transformation = itemDisplayPreSpawn.getTransformation();
            transformation.getLeftRotation().set(new AxisAngle4d(startRotationRadians, 0, 1, 0));
            itemDisplayPreSpawn.setTransformation(transformation);
        });
        Interaction interaction = spawnLocation.getWorld().spawn(spawnLocation, Interaction.class, interactionPreSpawn -> {
            interactionPreSpawn.setPersistent(false);
            interactionPreSpawn.setInteractionWidth(INTERACTION_HITBOX);
            interactionPreSpawn.setInteractionHeight(INTERACTION_HITBOX);
            interactionPreSpawn.getPersistentDataContainer().set(ENTITY_TAG, PersistentDataType.STRING, ARTIFACT_ID);
            interactionPreSpawn.getPersistentDataContainer().set(LINKED_ENTITY, PersistentDataType.STRING, itemDisplay.getUniqueId().toString());
        });
        return new ArtifactEntity(interaction, itemDisplay, spawnTimestamp, startRotationRadians, boundRole);
    }

    public static boolean isArtifact(Entity entity) {
        return ARTIFACT_ID.equalsIgnoreCase(entity.getPersistentDataContainer().get(ENTITY_TAG, PersistentDataType.STRING));
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

    public static boolean canBeContainedWithin(Block block) {
        if (Tag.SLABS.isTagged(block.getType())) {
            return false;
        }
        BoundingBox artifactBb = new BoundingBox(0.25, 0.125, 0.25, 0.75, 0.625, 0.75);
        return block.getCollisionShape()
            .getBoundingBoxes()
            .stream()
            .noneMatch(artifactBb::overlaps);
    }

    public void rotate(long time) {
        float rotation = (float) (Math.PI * 2 * (time % ROTATION_INTERVAL_TICKS)) / ROTATION_INTERVAL_TICKS;
        itemDisplay.getScheduler().run(EventMain.getInstance(), ignored -> {
            Transformation transformation = itemDisplay.getTransformation();
            transformation.getLeftRotation().set(new AxisAngle4d(startRotationRadians + rotation, 0, 1, 0));
            itemDisplay.setTransformation(transformation);
        }, null);
    }
}
