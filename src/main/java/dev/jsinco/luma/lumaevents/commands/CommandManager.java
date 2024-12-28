package dev.jsinco.luma.lumaevents.commands;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumacore.manager.commands.AbstractCommandManager;
import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(name = "event", aliases = {"winter"})
public class CommandManager extends AbstractCommandManager<EventMain, CommandModule> {
    public CommandManager() {
        super(EventMain.getInstance());
    }
}
