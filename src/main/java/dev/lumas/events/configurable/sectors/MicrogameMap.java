package dev.lumas.events.configurable.sectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ConfigSerializable
public class MicrogameMap {

    private String structureName;
    private int padding;

}
