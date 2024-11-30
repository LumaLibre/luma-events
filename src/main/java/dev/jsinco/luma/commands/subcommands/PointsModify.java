package dev.jsinco.luma.commands.subcommands;

import dev.jsinco.luma.PrisonMinePlayer;
import dev.jsinco.luma.PrisonMinePlayerManager;
import dev.jsinco.luma.ThanksgivingEvent;
import dev.jsinco.luma.Util;
import dev.jsinco.luma.commands.Subcommand;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class PointsModify implements Subcommand {
    @Override
    public void execute(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        PrisonMinePlayer prisonPlayer = PrisonMinePlayerManager.getByUUID(target.getUniqueId());
        if (args.length < 4) {
            Util.sendMsg(sender, "Points: " + prisonPlayer.getPoints());
            return;
        }

        int amount = Integer.parseInt(args[3]);

        if (args[2].equalsIgnoreCase("add")) {
            prisonPlayer.addPoints(amount);
        } else if (args[2].equalsIgnoreCase("remove")) {
            prisonPlayer.setPoints(prisonPlayer.getPoints() - amount);
        } else if (args[2].equalsIgnoreCase("set")) {
            prisonPlayer.setPoints(amount);
        }

        PrisonMinePlayerManager.save(prisonPlayer);
        Util.sendMsg(sender, "Points modified for " + target.getName() + " by " + amount + ". <gray>(" + target.getUniqueId() + ")");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        if (args.length == 3) {
            return List.of("add", "remove", "set");
        }
        return null;
    }

    @Override
    public String permission() {
        return "lumaevent.admin";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }
}
