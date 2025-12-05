package dev.jsinco.luma.lumaevents.archives;

import dev.jsinco.luma.lumaevents.archives.challenge.BringItemsChallenge;
import dev.jsinco.luma.lumaevents.archives.challenge.FindHeadsChallenge;
import dev.jsinco.luma.lumaevents.archives.challenge.MinigameRequirementChallenge;
import dev.jsinco.luma.lumaevents.archives.challenge.ParkourChallenge;
import dev.jsinco.luma.lumaevents.archives.challenge.PlaytimeRequirementChallenge;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public enum ChallengeType {

    PLAY_MINIGAMES(MinigameRequirementChallenge.class, List.of(
            "<gray>Play 15 minigames throughout",
            "<gray>the Christmas event to complete",
            "<gray>this challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 11, "<gold><b>Play 15 Minigames"),
    PARKOUR(ParkourChallenge.class, List.of(
            "<gray>Locate and complete the parkour(s)",
            "<gray>to complete this this challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 12, "<gold><b>Complete the Parkour"),
    FIND_HEADS(FindHeadsChallenge.class, List.of(
            "<gray>Find every head located",
            "<gray>on the Christmas map and",
            "<gray>right-click them to complete",
            "<gray>this challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 13, "<gold><b>Locate all the Heads"),
    BRING_ITEMS(BringItemsChallenge.class, List.of(
            "<gray>Speak to frosty and bring him",
            "<gray>the items he requests to",
            "<gray>complete this challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 14, "<gold><b>Bring Frosty the Items"),
    PLAYTIME(PlaytimeRequirementChallenge.class, List.of(
            "<gray>Have a minimum of 24 hours",
            "<gray>of total, non-afk playtime",
            "<gray>on Luma to complete this",
            "<gray>challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 15, "<gold><b>Have 24h of Playtime"),
    ;

    private final Class<? extends Challenge> challengeClass;
    private final List<String> description;
    private final int invLoc;
    private final String iconName;

    ChallengeType(Class<? extends Challenge> challengeClass, List<String> description, int invLoc, String iconName) {
        this.challengeClass = challengeClass;
        this.description = description;
        this.invLoc = invLoc;
        this.iconName = iconName;
    }

    public Challenge newInstance(int currentStage) {
        try {
            return challengeClass.getConstructor(int.class).newInstance(currentStage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Challenge newInstance(int currentStage, boolean assigned) {
        try {
            return challengeClass.getConstructor(int.class, boolean.class).newInstance(currentStage, assigned);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ItemStack icon(Challenge challenge) {
        Material material;
        if (challenge.isCompleted()) {
            material = Material.LIME_DYE;
        } else if (challenge.getCurrentStage() > 0) {
            material = Material.YELLOW_DYE;
        } else {
            material = Material.RED_DYE;
        }
        List<String> fullDescription = description.stream().map(s ->
                String.format(s, challenge.getCurrentStage(), challenge.getStages())).toList();
        return Util.createBasicItem(material, iconName, true, fullDescription, List.of());
    }
}
