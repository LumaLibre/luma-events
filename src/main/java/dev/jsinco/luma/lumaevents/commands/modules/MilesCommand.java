package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.explorer.gui.ExplorerMilesGui;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "miles",
        permission = "lumaevents.default",
        description = "miles",
        parent = CommandManager.class,
        usage = "/<command> reload"
)
public class MilesCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            return true;
        }
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), (task) -> {
            EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
            ExplorerMilesGui gui = new ExplorerMilesGui(eventPlayer);

            if (!eventPlayer.hasCompletedTutorialSection(TutorialSection.MILES_COMMAND)) {
                if (eventPlayer.getUnlockedExplorerMiles().size() >= 5) {
                    TutorialSection.MILES_COMMAND.completeTutorial(eventPlayer, new DialogueText(eventPlayer, NamedTextColor.YELLOW, 0.5f), () -> gui.open(player));
                } else {
                    eventPlayer.sendMessage("You need to complete <aqua>5</aqua> Explorer Miles to unlock this command!");
                }
            } else {
                gui.open(player);
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
