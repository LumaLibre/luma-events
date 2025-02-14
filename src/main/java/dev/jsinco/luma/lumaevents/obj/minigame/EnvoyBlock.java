package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.enums.minigame.EnvoyBlockType;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;

@ToString
@Getter
public class EnvoyBlock {

    private Object value;
    private final EnvoyBlockType envoyBlockType;
    private boolean isSolid = false;

    public EnvoyBlock(FallingBlock fallingBlock, EnvoyBlockType envoyBlockType) {
        this.value = fallingBlock;
        this.envoyBlockType = envoyBlockType;
    }

    public void updateToBlock(Block newBlock) {
        this.isSolid = true;
        this.value = newBlock;
    }

    public Location getLocation() {
        if (this.value instanceof FallingBlock fb) {
            return fb.getLocation();
        } else {
            return ((Block) this.value).getLocation();
        }
    }

    public void remove() {
        if (this.value instanceof FallingBlock fb) {
            fb.remove();
        } else {
            ((Block) this.value).setType(Material.AIR);
        }
    }

    public boolean is(FallingBlock fallingBlock) {
        return this.value instanceof FallingBlock fb && fb.equals(fallingBlock);
    }

    public boolean is(Block block) {
        return this.value instanceof Block b && b.equals(block);
    }

    public boolean is(Object value) {
        return this.value.equals(value);
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }

        if (value instanceof EnvoyBlock envoyBlock) {
            return this.is(envoyBlock.value);
        } else if (value instanceof FallingBlock fallingBlock) {
            return this.is(fallingBlock);
        } else if (value instanceof Block block) {
            return this.is(block);
        }
        return false;
    }

    public static EnvoyBlock fromFallingBlock(FallingBlock fallingBlock, EnvoyBlockType envoyBlockType) {
        return new EnvoyBlock(fallingBlock, envoyBlockType);
    }
}
