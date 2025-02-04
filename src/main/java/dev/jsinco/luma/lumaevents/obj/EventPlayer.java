package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class EventPlayer implements Serializable {

    private final UUID uuid;
    private EventTeamType teamType;
    private int points;

    public EventPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public void sendMessage(String m) {
        Util.sendMsg(this.getPlayer(), m);
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public void removePoints(int points) {
        this.points -= points;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

}
