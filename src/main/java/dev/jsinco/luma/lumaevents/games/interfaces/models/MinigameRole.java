package dev.jsinco.luma.lumaevents.games.interfaces.models;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import org.bukkit.Location;

import java.util.UUID;

@Getter
public abstract class MinigameRole {

    protected final EventPlayer eventPlayer;

    protected MinigameRole(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
    }


    public UUID getUuid() {
        return eventPlayer.getUuid();
    }

    public String getName() {
        return eventPlayer.getName();
    }

    public void teleportAsync(Location location) {
        eventPlayer.teleportAsync(location);
    }

}
