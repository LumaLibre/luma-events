package dev.jsinco.luma.commands.subcommands;

import dev.jsinco.luma.ThanksgivingEvent;
import dev.jsinco.luma.commands.Subcommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadConfig implements Subcommand {
    @Override
    public void execute(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        ThanksgivingEvent.setConfig(ThanksgivingEvent.getInstance().loadConfig());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        return List.of();
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
