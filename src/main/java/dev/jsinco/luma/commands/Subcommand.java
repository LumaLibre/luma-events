package dev.jsinco.luma.commands;

import dev.jsinco.luma.ThanksgivingEvent;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {

    void execute(CommandSender sender, String[] args, ThanksgivingEvent plugin);

    List<String> tabComplete(CommandSender sender, String[] args, ThanksgivingEvent plugin);

    String permission();

    boolean isPlayerOnly();
}
