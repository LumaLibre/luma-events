package dev.jsinco.luma.lumaevents.obj.minigame;

import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
public class BoatRaceCheckpoint extends WorldTiedBoundingBox {

    private final String identifier;
    @Setter
    private int worth;

    public BoatRaceCheckpoint(WorldTiedBoundingBox boundingBox, String identifier) {
        super(boundingBox.getWorld(), boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        this.identifier = identifier;
    }


    public int getWorth() {
        return Math.max(1, worth);
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
        return this.identifier.equals(that.identifier);
    }
}
