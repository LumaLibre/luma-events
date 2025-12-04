package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.interfaces.Scorer;
import dev.jsinco.luma.lumaevents.items.LocalCustomItemManager;
import dev.jsinco.luma.lumaevents.items.PresentItem;
import dev.jsinco.luma.lumaevents.utility.Executors;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Getter
@Setter
@AllArgsConstructor
public class EventPlayer implements Serializable, Scorer {

    private final UUID uuid;
    private final Map<MinigameConstant, Integer> scores;
    private final List<Challenge> challenges;
    private boolean claimedArchive;
    private boolean claimedCharm;


    // Initial creation
    public EventPlayer(UUID uuid) {
        this(uuid, new HashMap<>(), new ArrayList<>(), false, false);
    }


    public void sendMessage(String m) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        Util.sendMsg(player, m);
    }

    public void sendNoPrefixedMessage(String m) {
        this.sendNoPrefixedMessage(Util.color(m));
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

    public void operatePlayer(Consumer<Player> consumer) {
        Player player = this.getPlayer();
        if (player != null) {
            Executors.runSync(() -> consumer.accept(player));
        }
    }

    public void addBossBar(BossBar bossBar) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        bossBar.addViewer(player);
    }

    public void removeBossBar(BossBar bossBar) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        bossBar.removeViewer(player);
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

    public void addPermanentScore(MinigameConstant minigame, int score) {
        this.scores.put(minigame, this.scores.getOrDefault(minigame, 0) + score);
    }

    public int getPermanentScore(MinigameConstant minigame) {
        return this.scores.getOrDefault(minigame, 0);
    }



    // re-add from winter 2024


    public void addChallenge(Challenge challenge) {
        challenge.setAssigned(true);
        challenges.add(challenge);
    }

    public void removeChallenge(Challenge challenge) {
        challenge.setAssigned(false);
        challenges.remove(challenge);
    }

    public List<Challenge> getActiveChallenges() {
        return List.copyOf(challenges);
    }

    public int getTotalChallenges() {
        return ChallengeType.values().length;
    }

    public int getCompletedChallenges() {
        if (challenges.isEmpty()) {
            return 0;
        }
        return (int) challenges.stream().filter(Challenge::isCompleted).count();
    }

    public boolean completedAllChallenges() {
        return !challenges.isEmpty() && (getCompletedChallenges() >= getTotalChallenges());
    }

    public boolean hasCompleted(ChallengeType challengeType) {
        if (challenges.isEmpty()) {
            return false;
        }
        return challenges.stream().anyMatch(
                c -> c.getType() == challengeType && c.isCompleted()
        );
    }

    @NotNull
    public Challenge getChallenge(ChallengeType challengeType, boolean createIfNotExists) {
        Challenge challenge = challenges.stream().filter(
                c -> c.getType() == challengeType
        ).findFirst().orElse(null);

        if (challenge == null) {
            challenge = challengeType.newInstance(0);
            if (createIfNotExists) {
                addChallenge(challenge);
            }
        }

        return challenge;
    }


    public boolean claimChallengesReward(Player claimer) {
        if (this.claimedArchive || !completedAllChallenges()) {
            return false;
        }

        PresentItem customItem = LocalCustomItemManager.getCustomItem(PresentItem.class);
        if (customItem == null) {
            return false;
        }

        this.claimedArchive = true;
        String randomPlayerName = Util.getRandom(Bukkit.getOnlinePlayers()).getName();
        ItemStack item = customItem.getItemFormatted(randomPlayerName, claimer.getName());
        Executors.runAsync(() -> {
            EventPlayerManager.save(this);
            Executors.runSync(() -> Util.giveItem(claimer, item));
        });
        return true;
    }

}
