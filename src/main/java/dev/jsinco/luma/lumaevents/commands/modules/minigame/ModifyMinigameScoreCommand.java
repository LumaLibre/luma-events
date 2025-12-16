package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.utility.Text;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "modifymgscore",
        permission = "lumaevents.admin",
        description = "Modify a player's minigame score",
        parent = CommandManager.class,
        usage = "/<command> modifymgscore <add|remove|set> <minigame> <player> <amount>"
)
public class ModifyMinigameScoreCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length < 4) {
            Text.msg(commandSender, "Usage: /event " + s + " <add|remove|set> <minigame> <player> <amount>");
            return true;
        }

        Action action = Util.getEnumFromString(Action.class, strings[0]);
        if (action == null) {
            Text.msg(commandSender, "Invalid action. Use add, remove, or set.");
            return true;
        }

        MinigameConstant minigame = Util.getEnumFromString(MinigameConstant.class, strings[1]);
        if (minigame == null) {
            Text.msg(commandSender, "Invalid minigame: " + strings[1]);
            return true;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(strings[2]);
        if (!targetPlayer.hasPlayedBefore()) {
            Text.msg(commandSender, "Player not found: " + strings[2]);
            return true;
        }

        int amount = Util.getInt(strings[3], 0);
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(targetPlayer.getUniqueId());

        int currentScore = eventPlayer.getPermanentScore(minigame);

        switch (action) {
            case ADD -> eventPlayer.addPermanentScore(minigame, currentScore + amount);
            case REMOVE -> eventPlayer.setPermanentScore(minigame, currentScore - amount);
            case SET -> eventPlayer.setPermanentScore(minigame, amount);
        }

        EventPlayerManager.save(eventPlayer);
        Text.msg(commandSender, "Successfully modified " + targetPlayer.getName() + "'s score for " + minigame.name() + ".");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return switch (strings.length) {
            case 1 -> List.of("add", "remove", "set");
            case 2 -> Arrays.stream(MinigameConstant.values()).map(it -> it.name().toLowerCase()).toList();
            case 3 -> null;
            case 4 -> List.of("<amount>");
            default -> List.of();
        };
    }

    private enum Action {
        ADD,
        REMOVE,
        SET
    }
}
