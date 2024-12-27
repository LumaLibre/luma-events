package dev.jsinco.luma.commands;

import dev.jsinco.luma.EventMain;
import dev.jsinco.luma.manager.commands.AbstractCommandManager;
import dev.jsinco.luma.manager.commands.CommandInfo;
import dev.jsinco.luma.manager.modules.AutoRegister;
import dev.jsinco.luma.manager.modules.RegisterType;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(name = "event")
public class CommandManager extends AbstractCommandManager<EventMain, CommandModule> {
    public CommandManager(EventMain plugin) {
        super(plugin);
    }
}
