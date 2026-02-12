package dev.lumas.events.commands.modules.minigame;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
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
