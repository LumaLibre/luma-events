package dev.jsinco.lumacarnival.obj;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class InteractionDisplay extends AbstractedDisplay{

    private final Interaction interaction;

    public InteractionDisplay(Location spawn, Class<? extends Display> displayClass) {
        super(spawn, displayClass);

        this.interaction = spawn.getWorld().spawn(spawn, Interaction.class);
        this.interaction.setPersistent(false);
        this.interaction.getPersistentDataContainer().set(ID, PersistentDataType.BOOLEAN, true);
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public void remove() {
        super.remove();
        interaction.remove();
    }

    @Override
    public void modifyBackgroundSize(Vector scale) { // This is always getting bigger BTW
        interaction.setInteractionWidth((float) scale.getX() / 2);
        interaction.setInteractionHeight((float) scale.getY() / 2);
    }
}
