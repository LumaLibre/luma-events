package dev.jsinco.luma.lumaevents.challenges;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public enum ChallengeType {
    MAZE(MazeChallenge.class, List.of(
            "<gray>Locate the Winter maze and",
            "<gray>speak to the NPC at the end",
            "<gray>to complete this Winter Event",
            "<gray>challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 11, "<gold><b>Complete the Maze"),
    PARKOUR(ParkourChallenge.class, List.of(
            "<gray>Locate the Winter parkour and",
            "<gray>reach the end to complete this",
            "<gray>Winter Event challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 12, "<gold><b>Complete the Parkour"),
    FIND_HEADS(FindHeadsChallenge.class, List.of(
            "<gray>Find every head located",
            "<gray>on the Winter Event map and",
            "<gray>right-click them to complete",
            "<gray>this Winter Event challenge!",
            "",
            "<red>Progress: %s/%s"
    ), 13, "<gold><b>Locate all the Winter Crystals"),
    BRING_ITEMS(BringItemsChallenge.class, List.of(
            "<gray>Speak to frosty and bring him",
            "<gray>the items he requests to",
            "<gray>complete this Winter Event",
            "",
            "<red>Progress: %s/%s"
    ), 14, "<gold><b>Bring Frosty the Items");

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
        try { // Don't need to use reflection, could use switch statement
            return challengeClass.getConstructor(int.class).newInstance(currentStage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Challenge newInstance(int currentStage, boolean assigned) {
        try { // Don't need to use reflection, could use switch statement
            return challengeClass.getConstructor(int.class, boolean.class).newInstance(currentStage, assigned);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ItemStack icon(EventPlayer eventPlayer) {
        Challenge challenge = eventPlayer.getChallenge(this, false);
        Material material = challenge.isCompleted() ? Material.LIME_DYE : Material.RED_DYE;
        List<String> fullDescription = description.stream().map(s ->
                String.format(s, challenge.getCurrentStage(), challenge.getStages())).toList();
        return Util.createBasicItem(material, iconName, challenge.isCompleted(), fullDescription, List.of());
    }
}
