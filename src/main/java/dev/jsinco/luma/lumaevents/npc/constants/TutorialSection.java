package dev.jsinco.luma.lumaevents.npc.constants;

import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;

public enum TutorialSection {
    // Anais explains what 'Explorer Miles' are and once her dialogue is finished,
    //  players can start completing Explorer Miles
    EXPLORER_MILES(
            "Hm, what's this?",
            "These are your <aqua>Explorer Miles<green>!",
            "You can earn rewards by completing the tasks specified on them.",
            "Just like little <gold><i>quests!",
            "Come back to me when ya want to check your miles.",
            "Once you complete <aqua>5<green> miles,",
            "I'll let ya have access to <gold>/easter miles<green>!",
            "<i>So you won't have to chat with me all the time. ♥(ˆ⌣ˆԅ)",
            "...",
            "Oops, one last thing!",
            "Most miles aren't unlocked by default.",
            "So you'll have to <aqua>discover<green> them first.",
            "You can unlock miles by trading carrots to unlock new ones",
            "or, by completing certain tasks related to a mile.",
            "To start you off,",
            "I'll give you <aqua>10<green> random miles.",
            "Have fun!"
    ),
    // Anais explains what the Stalk Market is and how it works,
    // players won't see her explanation again
    STALK_MARKET(
            "So, ya got some carrots and ya wanna trade?",
            "Here's the dilly-dally...",
            "Every day, I'll buy Carrots for a price I think they're worth",
            "Some days I think Carrots will be worth a bit more, some days less!",
            "I'll give ya <aqua>1 Basket <green>in exchange for my selling price!",
            "So, let's get to trading!"
    ),
    MILES_COMMAND(
            "You completed <aqua>5<green> Explorer Miles!",
            "You can now use <gold>/easter miles<green> to check your miles.",
            "Enjoy!"
    ),
    ANAIS_INTRODUCTION(
            "Hello there, <gold>stranger!",
            "I'm Anais, the <gold>Stalk Market<green> lady!",
            "I sell baskets for carrots, and I also help ya with your Explorer Miles.",
            "Let me know if ya need anything!"
    ),
    QUIZ_REWARD(
            "Oh? How did you find me all the way down here?",
            "I was just about to go on a little adventure myself...",
            "Then I got lost down this rabbit hole and now I'm stuck.",
            "Here, take this and go find some help..."
    ),
    QUIZ_REWARD_FIXED(
            "Oh? How did you find me all the way down here?",
            "I was just about to go on a little adventure myself...",
            "Then I got lost down this rabbit hole and now I'm stuck.",
            "Here, take this and go find some help..."
    );

    private final String[] lines;

    TutorialSection(String... lines) {
        this.lines = lines;
    }

    public void completeTutorial(EventPlayer eventPlayer, DialogueText dialogueText, Runnable callback) {
        dialogueText.queueText(this.lines);
        eventPlayer.addCompletedTutorialSection(this);
        dialogueText.sendQueuedText(callback);
    }
}
