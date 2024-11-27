package dev.jsinco.luma.commands.subcommands;

import dev.jsinco.luma.PrisonMinePlayer;
import dev.jsinco.luma.PrisonMinePlayerManager;
import dev.jsinco.luma.ThanksgivingEvent;
import dev.jsinco.luma.Util;
import dev.jsinco.luma.commands.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ViewTokens implements Subcommand {
    @Override
    public void execute(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        Player player = (Player) sender;
        PrisonMinePlayer prisonMinePlayer = PrisonMinePlayerManager.getByUUID(player.getUniqueId());

        Util.sendMsg(player, "You have <gold>" + prisonMinePlayer.getPoints() + "<reset> tokens.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        return List.of();
    }

    @Override
    public String permission() {
        return "lumaevent.default";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
