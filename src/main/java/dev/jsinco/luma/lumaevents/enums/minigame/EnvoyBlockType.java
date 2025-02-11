package dev.jsinco.luma.lumaevents.enums.minigame;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

@Getter
public enum EnvoyBlockType {

    DEFAULT(Material.MAGENTA_CONCRETE_POWDER, Material.SHULKER_BOX),
    WHITE(Material.WHITE_CONCRETE, Material.WHITE_SHULKER_BOX),
    LIGHT_GRAY(Material.LIGHT_GRAY_CONCRETE, Material.LIGHT_GRAY_SHULKER_BOX),
    GRAY(Material.GRAY_CONCRETE, Material.GRAY_SHULKER_BOX),
    BLACK(Material.BLACK_CONCRETE, Material.BLACK_SHULKER_BOX),
    BROWN(Material.BROWN_CONCRETE, Material.BROWN_SHULKER_BOX),
    RED(Material.RED_CONCRETE, Material.RED_SHULKER_BOX),
    ORANGE(Material.ORANGE_CONCRETE, Material.ORANGE_SHULKER_BOX),
    YELLOW(Material.YELLOW_CONCRETE, Material.YELLOW_SHULKER_BOX),
    LIME(Material.LIME_CONCRETE, Material.LIME_SHULKER_BOX),
    GREEN(Material.GREEN_CONCRETE, Material.GREEN_SHULKER_BOX),
    CYAN(Material.CYAN_CONCRETE, Material.CYAN_SHULKER_BOX),
    LIGHT_BLUE(Material.LIGHT_BLUE_CONCRETE, Material.LIGHT_BLUE_SHULKER_BOX),
    BLUE(Material.BLUE_CONCRETE, Material.BLUE_SHULKER_BOX),
    PURPLE(Material.PURPLE_CONCRETE, Material.PURPLE_SHULKER_BOX),
    MAGENTA(Material.MAGENTA_CONCRETE, Material.MAGENTA_SHULKER_BOX),
    PINK(Material.PINK_CONCRETE, Material.PINK_SHULKER_BOX)
    ;

    private final Material fallingBlock;
    private final Material solidBlock;

    EnvoyBlockType(Material fallingBlock, Material solidBlock) {
        this.fallingBlock = fallingBlock;
        this.solidBlock = solidBlock;
    }

    public static List<Material> getFallingBlocks() {
        return Arrays.stream(values())
                .map(EnvoyBlockType::getFallingBlock)
                .toList();
    }

    public static List<Material> getSolidBlocks() {
        return Arrays.stream(values())
                .map(EnvoyBlockType::getSolidBlock)
                .toList();
    }

    public static EnvoyBlockType getByFallingBlock(Material material) {
        return Arrays.stream(values())
                .filter(type -> type.getFallingBlock() == material)
                .findFirst()
                .orElse(null);
    }

    public static EnvoyBlockType getBySolidBlock(Material material) {
        return Arrays.stream(values())
                .filter(type -> type.getSolidBlock() == material)
                .findFirst()
                .orElse(null);
    }

    public static EnvoyBlockType getByAny(Material material) {
        EnvoyBlockType type = getByFallingBlock(material);
        if (type == null) {
            type = getBySolidBlock(material);
        }
        return type;
    }
}
