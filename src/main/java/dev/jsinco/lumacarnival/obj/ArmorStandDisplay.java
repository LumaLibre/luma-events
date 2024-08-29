package dev.jsinco.lumacarnival.obj;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class ArmorStandDisplay {


    private final ArmorStand armorStand;

    public ArmorStandDisplay(Location spawn,) {
        this.armorStand = spawn.getWorld().spawn(spawn, ArmorStand.class);

        this.armorStand.
    }
}
