package dev.jsinco.lumacarnival.obj;

import dev.jsinco.lumacarnival.CarnivalMain;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Represents an Easter Egg. Made up of an eggshell (ItemDisplay) and an egg yolk (Interaction Entity)
 */
public class Thing {

    private static final NamespacedKey ENTITY_ID = new NamespacedKey(CarnivalMain.getInstance(), "display_entity");

    private final ItemDisplay display;
    private final Interaction interaction;



    public Thing(ItemStack itemStack, Location location) {
        this.display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.display.setItemStack(itemStack);


        this.interaction = (Interaction) location.getWorld().spawnEntity(location.add(0.0, -0.41, 0.0), EntityType.INTERACTION);
        this.interaction.setInteractionHeight(0.5f);
        this.interaction.setInteractionWidth(0.5f);
        this.interaction.setResponsive(true);


        // Prevent saving to disk
        this.display.setPersistent(false);
        this.interaction.setPersistent(false);


        // Identifiers
        this.interaction.getPersistentDataContainer().set(ENTITY_ID, PersistentDataType.BOOLEAN, true);
        this.display.getPersistentDataContainer().set(ENTITY_ID, PersistentDataType.BOOLEAN, true);

    }


    public ItemDisplay getDisplay() {
        return display;
    }

    public Interaction getInteraction() {
        return interaction;
    }



    public void remove() {
        interaction.remove();
        display.remove();
    }


    public void transformationRight(Vector axis, float angle) {
        display.setInterpolationDuration(40);
        display.setInterpolationDelay(-1);
        Transformation transformation = display.getTransformation();
        transformation.getRightRotation()
                .set(new AxisAngle4f(angle, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ()));
        display.setTransformation(transformation);
    }

    public void transformationLeft(Vector axis, float angle) {
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(-1);
        Transformation transformation = display.getTransformation();
        transformation.getLeftRotation()
                .set(new AxisAngle4f(angle, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ()));
        display.setTransformation(transformation);
    }

    public void transformationScale(Vector scale) {
        boolean isShrinking = scale.getX() < display.getTransformation().getScale().x;

        display.setInterpolationDuration(0);
        display.setInterpolationDelay(-1);
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ()));
        display.setTransformation(transformation);

        double yFactor = 0.05;
        if (isShrinking) {
            yFactor = yFactor * -1;
        }

        display.teleport(display.getLocation().add(0, yFactor, 0));
        interaction.teleport(interaction.getLocation().add(0, yFactor / 2, 0));
        //eggStamp.teleport(eggStamp.getLocation().add(0, yFactor, 0));

        modifyInteractionSize(scale);
    }

    public void modifyInteractionSize(Vector scale) { // This is always getting bigger BTW
        interaction.setInteractionWidth((float) scale.getX() / 2);
        interaction.setInteractionHeight((float) scale.getY() / 2);
    }
}
