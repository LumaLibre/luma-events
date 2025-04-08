package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles;
import dev.jsinco.luma.lumaevents.explorer.ActiveExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class EventPlayer implements Serializable {

    private final UUID uuid;
    private final List<ActiveExplorerMile> activeExplorerMiles;

    public EventPlayer(UUID uuid) {
        this.uuid = uuid;
        this.activeExplorerMiles = new ArrayList<>();
    }

    public void sendMessage(String m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        Util.sendMsg(player, m);
    }

    public void sendNoPrefixedMessage(String m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(Util.color(m));
    }

    public void sendNoPrefixedMessage(Component m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(m);
    }

    public void sendActionBar(String m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendActionBar(Util.color(m));
    }

    public void sendTitle(String title, String subtitle) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.showTitle(Title.title(Util.color(title), Util.color(subtitle)));
    }

    public void teleportAsync(Location location) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.teleportAsync(location);
    }


    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public boolean isOnline() {
        Player player = this.getPlayer();
        if (player == null) {
            return false;
        }
        return player.isOnline();
    }


    public <T> boolean hasActiveExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : activeExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return true;
            }
        }
        return false;
    }

    public void fireForExplorerMiles(Object event) {
        List<ActiveExplorerMile> testableExplorerMiles = new ArrayList<>(activeExplorerMiles);

        for (ExplorerMile<?> explorerMile : ExplorerMiles.asMap().values()) {
            if (explorerMile.getEventClass() == event.getClass() && !hasActiveExplorerMile(explorerMile)) {
                Util.log("Testing an ExplorerMile for which a player does not have any data for: " + explorerMile);
                testableExplorerMiles.add(new ActiveExplorerMile(explorerMile, 0));
            }
        }

        for (ActiveExplorerMile activeExplorerMile : testableExplorerMiles) {
            if (activeExplorerMile.getMile().getEventClass() == event.getClass()) {
                activeExplorerMile.apply(event);
            }
        }

        testableExplorerMiles.forEach(activeExplorerMile -> {
            if (activeExplorerMile.getCurrentQuantity() > 0 && !activeExplorerMiles.contains(activeExplorerMile)) {
                activeExplorerMiles.add(activeExplorerMile);
            }
        });
    }

}
