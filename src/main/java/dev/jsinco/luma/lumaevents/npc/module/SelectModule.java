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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;


@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "select",
        permission = "lumaevents.internal",
        parent = AnaisModuleManager.class
)
public class SelectModule implements NPCCommandModule {
    @Override
    public boolean execute(EventMain plugin, Player target, String label, String[] args) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(target.getUniqueId());

        TutorialSection tutorialSection = TutorialSection.ANAIS_INTRODUCTION;

        SelectOptionGui gui = new SelectOptionGui(eventPlayer);

        DialogueText dialogueText = new DialogueText(eventPlayer);
        dialogueText.setIfAbsentColor(NamedTextColor.GREEN);

        if (eventPlayer.hasCompletedTutorialSection(tutorialSection)) {
            dialogueText.queueText("Hello, hello! How are you, " + target.getName() + "?");
            dialogueText.queueText("How can I help you today?");
            dialogueText.sendQueuedText(() -> {
                gui.open(target);
            });
        } else {
            tutorialSection.completeTutorial(eventPlayer, dialogueText, () -> {
                gui.open(target);
            });
        }


        return true;
    }


    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
*/