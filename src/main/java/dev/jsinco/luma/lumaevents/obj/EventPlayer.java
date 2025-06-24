package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.games.Scorer;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventPlayer implements Serializable, Scorer {

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
    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new ArrayList<>());
    }

    public EventPlayer(UUID uuid, List<TutorialSection> completedTutorialSections) {
        this.uuid = uuid;
        this.completedTutorialSections = completedTutorialSections;
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
        this.sendActionBar(Util.color(m));
    }

    public void sendActionBar(Component m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendActionBar(m);
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

    @Override
    public String getName() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.uuid);
        return offlinePlayer.getName();
    }
}
