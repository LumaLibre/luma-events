package dev.jsinco.luma.lumaevents.npc.module;

/*
import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.npc.SelectOptionGui;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;


@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "quizreward",
        permission = "lumaevents.internal",
        parent = AnaisModuleManager.class
)
public class QuizRewardModule implements NPCCommandModule {
    @Override
    public boolean execute(EventMain plugin, Player target, String label, String[] args) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(target.getUniqueId());

        DialogueText dialogueText = new DialogueText(eventPlayer);
        dialogueText.setIfAbsentColor(NamedTextColor.YELLOW);
        TutorialSection tutorialSection = TutorialSection.QUIZ_REWARD_FIXED;
        if (!eventPlayer.hasCompletedTutorialSection(tutorialSection)) {
            tutorialSection.completeTutorial(eventPlayer, dialogueText, () -> {
                Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
                    TokenExchanging.give(eventPlayer.getPlayer(), TokenExchanging.TokenType.BASKET, 2);
                });
            });
        } else {
            dialogueText.queueText("What are you doing here?");
            dialogueText.queueText("Go get some help!");
            dialogueText.sendQueuedText();
        }
        return true;
    }


    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
 */