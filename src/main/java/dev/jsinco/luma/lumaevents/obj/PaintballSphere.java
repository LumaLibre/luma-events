package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.games.logic.Paintball;
import dev.jsinco.luma.lumaitems.shapes.Sphere;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class PaintballSphere extends Sphere {
    
    
    private EventTeamType painter;
    
    public PaintballSphere(Location center, EventTeamType painter) {
        super(center, 3, 14);
        this.painter = painter;
    }

    public boolean isOverlaying(PaintballSphere paintballSphere) {
        return this.isWithinMarge(paintballSphere.getCenter(), paintballSphere.getRadius());
    }

    public boolean isNearCenter(Location location) {
        return this.isWithinMarge(location, -2);
    }

    public void paint(List<EventPlayer> participants, BlockData blockData) {
        players: for (EventPlayer player : participants) {
            for (Block block : this.getSphere().stream().filter(b -> !Paintball.BLACKLISTED_MATERIALS.contains(b.getType())).toList()) {
                Player bukkitPlayer = player.getPlayer();
                if (bukkitPlayer == null) {
                    continue players;
                }
                bukkitPlayer.sendBlockChange(block.getLocation(), blockData);
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        PaintballSphere that = (PaintballSphere) object;
        return painter == that.painter;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(painter);
    }
}
