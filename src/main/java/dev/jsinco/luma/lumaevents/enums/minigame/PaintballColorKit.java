package dev.jsinco.luma.lumaevents.enums.minigame;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum PaintballColorKit {

    DEFAULT(Material.RED_WOOL, Material.LIME_WOOL, Material.CYAN_WOOL),
    A(Material.BLUE_WOOL, Material.ORANGE_WOOL, Material.PURPLE_WOOL),
    B(Material.YELLOW_WOOL, Material.PURPLE_WOOL, Material.PINK_WOOL),
    C(Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_GRAY_WOOL),
    D(Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL),
    E(Material.LIGHT_BLUE_WOOL, Material.PINK_WOOL, Material.LIME_WOOL),
    ;

    private final Material rosethorn;
    private final Material sweethearts;
    private final Material heartbreakers;

    PaintballColorKit(Material rosethorn, Material sweethearts, Material heartbreakers) {
        this.rosethorn = rosethorn;
        this.sweethearts = sweethearts;
        this.heartbreakers = heartbreakers;
    }
}
