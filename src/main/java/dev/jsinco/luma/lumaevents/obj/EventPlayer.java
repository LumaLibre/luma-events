package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles;
import dev.jsinco.luma.lumaevents.explorer.ActiveExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.utility.Util;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class EventPlayer implements Serializable {

    private transient volatile Object LOCK;
    private Object getLock() {
        if (LOCK == null) {
            synchronized (this) {
                if (LOCK == null) {
                    LOCK = new Object();
                }
            }
        }
        return LOCK;
    }

    private final UUID uuid;
    private final List<TutorialSection> completedTutorialSections;
    private final Set<ActiveExplorerMile> unlockedExplorerMiles;

    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new ArrayList<>(), new HashSet<>());

        // Unlock 10 random ExplorerMiles for them, rest will have to be discovered or unlocked
        int i = 0;
        Collection<ExplorerMile<?>> explorerMiles = ExplorerMiles.values();
        if (explorerMiles.size() < 10) {
            for (ExplorerMile<?> explorerMile : explorerMiles) {
                unlockedExplorerMiles.add(new ActiveExplorerMile(explorerMile));
            }
        } else {
            while (i < 10) {
                ExplorerMile<?> explorerMile = Util.getRandom(explorerMiles);
                if (hasUnlockedExplorerMile(explorerMile)) {
                    continue;
                }

                unlockedExplorerMiles.add(new ActiveExplorerMile(explorerMile));
                i++;
            }
        }
    }

    public EventPlayer(UUID uuid, List<TutorialSection> completedTutorialSections, Set<ActiveExplorerMile> unlockedExplorerMiles) {
        this.uuid = uuid;
        this.completedTutorialSections = completedTutorialSections;
        this.unlockedExplorerMiles = unlockedExplorerMiles;
    }

    public void addCompletedTutorialSection(TutorialSection section) {
        if (this.completedTutorialSections.contains(section)) {
            return;
        }
        this.completedTutorialSections.add(section);
    }

    public boolean hasCompletedTutorialSection(TutorialSection section) {
        return this.completedTutorialSections.contains(section);
    }

    public int getCompletedExplorerMiles() {
        int completed = 0;
        for (ActiveExplorerMile activeExplorerMile : this.unlockedExplorerMiles) {
            if (activeExplorerMile.getUnchangeableLevelSnapshot().isCompleted()) {
                completed++;
            }
        }
        return completed;
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


    public <T> boolean hasUnlockedExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : this.unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return true;
            }
        }
        return false;
    }

    public <T> boolean unlockExplorerMile(ExplorerMile<T> explorerMile) {
        return this.unlockedExplorerMiles.add(new ActiveExplorerMile(explorerMile));
    }

    public <T> ActiveExplorerMile getActiveExplorerMile(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile;
            }
        }
        return null;
    }

    public <T> int getCurrentQuantity(ExplorerMile<T> explorerMile) {
        for (ActiveExplorerMile activeExplorerMile : unlockedExplorerMiles) {
            if (Objects.equals(activeExplorerMile.getMile().getFIELD_NAME(), explorerMile.getFIELD_NAME())) {
                return activeExplorerMile.getCurrentQuantity();
            }
        }
        return 0;
    }

    public void fireForExplorerMiles(Object event) {
        synchronized (this.getLock()) {
            Set<ActiveExplorerMile> testableExplorerMiles = new HashSet<>(this.unlockedExplorerMiles);

            for (ExplorerMile<?> explorerMile : ExplorerMiles.asMap().values()) {
                if (explorerMile.getEventClass() == event.getClass() && !hasUnlockedExplorerMile(explorerMile)) {
                    //Util.log("Testing an ExplorerMile for which a player does not have any data for: " + explorerMile);
                    testableExplorerMiles.add(new ActiveExplorerMile(explorerMile));
                }
            }


            for (ActiveExplorerMile activeExplorerMile : testableExplorerMiles) {
                if (activeExplorerMile == null) {
                    continue;
                }
                if (activeExplorerMile.getMile().getEventClass() == event.getClass()) {
                    activeExplorerMile.apply(event, this);
                }
            }

            testableExplorerMiles.forEach(activeExplorerMile -> {
                if (!hasUnlockedExplorerMile(activeExplorerMile.getMile())) {
                    if (activeExplorerMile.hasProgress()) {
                        this.unlockedExplorerMiles.add(activeExplorerMile);
                        activeExplorerMile.playMilesUnlockEffect(this, null);
                    }
                }
            });
        }
    }

}
