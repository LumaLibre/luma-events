package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaitems.shapes.Sphere;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
@Setter
public class PaintballSphere extends Sphere {
    
    
    private EventTeamType painter;
    
    public PaintballSphere(Location center, EventTeamType painter) {
        super(center, 3, 5);
        this.painter = painter;
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
