package dev.jsinco.luma.lumaevents.commands;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumacore.manager.commands.AbstractCommandManager;
import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(
        name = "event",
        aliases = {"valentide"},
        permission = "lumaevents.default"
)
public class CommandManager extends AbstractCommandManager<EventMain, CommandModule> {

    public CommandManager() {
        super(EventMain.getInstance());
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        return super.handle(sender, label, args);
    }
}
