package dev.lumas.events.games.logic.manor2.light;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class Visibility {

    private final Set<UUID> hiddenFrom = ConcurrentHashMap.newKeySet();
    private final Manor2 manor2;
    private final Object vanishLock = new Object(); // TODO: probably a better way to do this, i didnt want to deal with the race conditions - Jsinco
    private volatile boolean stopping = false;
    private final EventPlayer player;

    public Visibility(Manor2 manor2, EventPlayer player) {
        this.manor2 = manor2;
        this.player = player;
    }

    public void stop() {
        List<UUID> toShow;
        synchronized (vanishLock) {
            if (stopping) {
                return;
            }
            stopping = true;
            toShow = List.copyOf(hiddenFrom);
        }

        Player self = player.getPlayer();
        if (self == null) return;
        for (UUID viewerId : toShow) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) {
                hiddenFrom.remove(viewerId);
                continue;
            }

            Executors.runSync(viewer, () -> showTo(viewer, self));
        }
    }

    public void setVanished(Predicate<Manor2Role> vanished) {
        Player self = player.getPlayer();
        if (self == null) {
            return;
        }
        for (Manor2Role role : manor2.getRoleMap()) {
            Player viewer = role.getEventPlayer().getPlayer();
            if (viewer == null || viewer == self) {
                continue;
            }

            boolean hide = vanished.test(role) && manor2.getParticipants().contains(role.getEventPlayer());
            if (viewer.canSee(self) != hide) {
                continue;
            }

            role.operatePlayer(target -> {
                if (hide) hideFrom(target, self);
                else showTo(target, self);
            });
        }
    }

    private void hideFrom(Player viewer, Player self) {
        synchronized (vanishLock) {
            if (stopping) {
                return;
            }
            viewer.hidePlayer(EventMain.getInstance(), self);
            hiddenFrom.add(viewer.getUniqueId());
        }
    }

    private void showTo(Player viewer, Player self) {
        viewer.showPlayer(EventMain.getInstance(), self);
        hiddenFrom.remove(viewer.getUniqueId());
    }
}
