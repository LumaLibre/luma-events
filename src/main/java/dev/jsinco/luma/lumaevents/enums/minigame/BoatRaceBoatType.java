package dev.jsinco.luma.lumaevents.enums.minigame;

import lombok.Getter;
import org.bukkit.entity.Boat;
import org.bukkit.entity.boat.AcaciaBoat;
import org.bukkit.entity.boat.BirchBoat;
import org.bukkit.entity.boat.CherryBoat;
import org.bukkit.entity.boat.DarkOakBoat;
import org.bukkit.entity.boat.JungleBoat;
import org.bukkit.entity.boat.MangroveBoat;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.entity.boat.PaleOakBoat;
import org.bukkit.entity.boat.SpruceBoat;

@Getter
public enum BoatRaceBoatType {

    OAK(OakBoat.class),
    SPRUCE(SpruceBoat.class),
    BIRCH(BirchBoat.class),
    JUNGLE(JungleBoat.class),
    ACACIA(AcaciaBoat.class),
    DARK_OAK(DarkOakBoat.class),
    MANGROVE(MangroveBoat.class),
    CHERRY(CherryBoat.class),
    PALE_OAK(PaleOakBoat.class);

    private final Class<? extends Boat> boatType;

    BoatRaceBoatType(Class<? extends Boat> boatType) {
        this.boatType = boatType;
    }
}
