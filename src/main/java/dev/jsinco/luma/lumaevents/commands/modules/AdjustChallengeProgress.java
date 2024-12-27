package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumaevents.challenges.ChallengeType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.challenges.Challenge;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.manager.commands.CommandInfo;
import dev.jsinco.luma.manager.modules.AutoRegister;
import dev.jsinco.luma.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.EventPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
    parent = CommandManager.class,
    permission = "lumaevent.admin",
    name = "adjustChallengeProgress",
    aliases = {"acp"},
    usage = "/<command> adjustChallengeProgress <add|set|remove> <player!> <challenge!> <amount!>"
)
public class AdjustChallengeProgress implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length != 4) {
            return false;
        }

        Player onlinePlayer = Bukkit.getPlayerExact(strings[1]);
        if (onlinePlayer == null) {
            return false;
        }

        String operation = strings[0].toLowerCase();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(onlinePlayer.getUniqueId());
        ChallengeType challengeType = ChallengeType.valueOf(strings[2].toUpperCase());
        int amount = Integer.parseInt(strings[3]);

        Challenge challenge = eventPlayer.getChallenge(challengeType, true);

        switch (operation) {
            case "add" -> challenge.setCurrentStage(challenge.getCurrentStage() + amount);
            case "set" -> challenge.setCurrentStage(amount);
            case "remove" -> challenge.setCurrentStage(challenge.getCurrentStage() - amount);
        }

        EventMain.getInstance().getLogger().info("Set " + challengeType + " to " + amount + " for " + onlinePlayer.getName());
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return switch (strings.length) {
            case 1 -> List.of("add", "set", "remove");
            case 3 -> Arrays.stream(ChallengeType.values()).map(it -> it.name().toLowerCase()).toList();
            case 4 -> List.of("<amount>");
            default -> null;
        };
    }
}
