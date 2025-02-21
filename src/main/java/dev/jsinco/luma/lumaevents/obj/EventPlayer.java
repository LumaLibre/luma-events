package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.enums.EventReward;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class EventPlayer implements Serializable {

    private final UUID uuid;
    // FIXME: This should actually be 'claimedRewards' but,
    // This is already on prod and i'm too lazy to change it
    private final List<EventReward> unclaimedRewards;

    @Nullable
    private EventTeamType teamType;
    private int points;
    private boolean disabledTeamChat;

    public EventPlayer(UUID uuid) {
        this.uuid = uuid;
        this.unclaimedRewards = new ArrayList<>();
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

    public void sendTeamStyleMessage(String m) {
        this.sendNoPrefixedMessage(
                "<b>"+teamType.getGradient()+teamType.getFormatted()+" <reset><dark_gray>» <white>"+m
        );
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

    public void addPoints(int points) {
        this.points += points;
    }

    public void removePoints(int points) {
        this.points -= points;
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


    public void enableTeamChat() {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.setMetadata("teamchat", new FixedMetadataValue(EventMain.getInstance(), true));
    }

    public void disableTeamChat() {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.removeMetadata("teamchat", EventMain.getInstance());
    }


    public boolean isTeamChat() {
        Player player = this.getPlayer();
        if (player == null) {
            return false;
        }
        return player.hasMetadata("teamchat");
    }

    public void setTeamChat(boolean enabled) {
        if (enabled) {
            enableTeamChat();
        } else {
            disableTeamChat();
        }
    }

    public void addReward(EventReward reward) {
        this.unclaimedRewards.add(reward);
    }

    public void removeReward(EventReward reward) {
        this.unclaimedRewards.remove(reward);
    }

    public boolean claimAvailableRewards() {
        Player player = this.getPlayer();
        if (player == null) {
            return false;
        }


        boolean claimedAny = false;
        for (EventReward reward : EventReward.values()) {
            if (this.unclaimedRewards.contains(reward) || reward.getTeamType() != null && reward.getTeamType() != this.teamType) {
                continue;
            }

            if (reward.claim(this)) {
                this.unclaimedRewards.add(reward);
                claimedAny = true;
            }
        }
        EventPlayerManager.save(this);
        return claimedAny;
    }
}
