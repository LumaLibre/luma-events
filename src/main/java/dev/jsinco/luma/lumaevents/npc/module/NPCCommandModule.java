package dev.jsinco.luma.lumaevents.npc.module;

import dev.jsinco.luma.lumacore.manager.commands.AbstractSubCommand;
import dev.jsinco.luma.lumaevents.EventMain;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface NPCCommandModule extends AbstractSubCommand<EventMain> {
    boolean execute(EventMain plugin, Player target, String label, String[] args);

    default boolean execute(EventMain plugin, CommandSender sender, String label, String[] args) {
        return false;
    }
}
