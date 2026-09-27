package dev.lumas.events.games.interfaces.models;

import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.model.EventPlayer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public abstract class MinigameRole implements Scorer {

    protected final EventPlayer eventPlayer;

    protected MinigameRole(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
    }


    public UUID getUuid() {
        return eventPlayer.getUuid();
    }

    @Override
    public String getName() {
        return eventPlayer.getName();
    }

    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return eventPlayer.teleportAsync(location);
    }

    public void operatePlayer(Consumer<Player> consumer) {
        eventPlayer.operatePlayer(consumer);
    }

}
