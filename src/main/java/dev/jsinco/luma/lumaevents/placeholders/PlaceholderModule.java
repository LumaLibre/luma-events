package dev.jsinco.luma.lumaevents.placeholders;

import dev.jsinco.luma.lumacore.manager.placeholder.AbstractPlaceholder;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface PlaceholderModule extends AbstractPlaceholder<EventMain> {

    @Nullable
    default EventTeam getFromPosition(int pos) {
        Set<EventTeam> teamSorted = EventTeam.of(true);

        // Convert the Set to an array
        EventTeam[] teamArray = teamSorted.toArray(new EventTeam[0]);

        // Ensure the position is valid
        if (pos < 0 || pos >= teamArray.length) {
            return null;
        }

        return teamArray[pos];
    }

}
