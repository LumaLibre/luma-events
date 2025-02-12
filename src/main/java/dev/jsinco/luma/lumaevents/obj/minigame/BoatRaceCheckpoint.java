package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class BoatRaceCheckpoint extends WorldTiedBoundingBox {

    private final int index;

    public BoatRaceCheckpoint(WorldTiedBoundingBox boundingBox, int index) {
        super(boundingBox.getWorld(), boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        this.index = index;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BoatRaceCheckpoint that = (BoatRaceCheckpoint) obj;
        return index == that.index;
    }
}
