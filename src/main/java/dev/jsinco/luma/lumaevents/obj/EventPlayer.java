package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.challenges.ChallengeType;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.challenges.Challenge;
import dev.jsinco.luma.lumaevents.items.CustomItemsManager;
import dev.jsinco.luma.lumaevents.items.PresentItem;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class EventPlayer {

    private final List<Challenge> challenges;
    private final UUID uuid;

    private boolean claimedReward;

    public EventPlayer(UUID uuid) {
        this.uuid = uuid;
        this.challenges = new ArrayList<>();
    }

    public EventPlayer(UUID uuid, List<Challenge> challenges, boolean claimedReward) {
        this.uuid = uuid;
        this.challenges = challenges;
        this.claimedReward = claimedReward;
    }


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
        return challenges.size();
    }

    public int getCompletedChallenges() {
        return (int) challenges.stream().filter(Challenge::isCompleted).count();
    }

    public boolean completedAllChallenges() {
        return challenges.stream().allMatch(Challenge::isCompleted);
    }

    public boolean hasCompleted(ChallengeType challengeType) {
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


    public boolean claimReward(Player claimer) {
        if (claimedReward || !completedAllChallenges()) {
            return false;
        }

        PresentItem customItem = CustomItemsManager.presentItem;
        if (customItem == null) {
            return false;
        }

        claimedReward = true;
        String randomPlayerName = Util.getRandom(Bukkit.getOnlinePlayers()).getName();
        ItemStack item = customItem.getItemFormatted(randomPlayerName, claimer.getName());
        Util.giveItem(claimer, item);
        return true;
    }

}
