package dev.jsinco.lumacarnival.obj;

import dev.jsinco.lumacarnival.CarnivalMain;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public abstract class AbstractedDisplay {

    protected static final NamespacedKey ID = new NamespacedKey(CarnivalMain.getInstance(), "carnival_display");

    protected final Display display;

    public AbstractedDisplay(Location spawn, Class<? extends Display> displayClass) {
        this.display = spawn.getWorld().spawn(spawn, displayClass);
        this.display.setPersistent(false);
        this.display.getPersistentDataContainer().set(ID, PersistentDataType.BOOLEAN, true);
    }


    public <T extends Display> T getDisplay() {
        return (T) display;
    }


    public void remove() {
        display.remove();
    }


    public void setSize(int width, int height) {
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(-1);
        display.setDisplayWidth(width);
        display.setDisplayHeight(height);

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
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(-1);
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ()));
        display.setTransformation(transformation);


        modifyBackgroundSize(scale);
    }


    public abstract void modifyBackgroundSize(Vector scale);
}
