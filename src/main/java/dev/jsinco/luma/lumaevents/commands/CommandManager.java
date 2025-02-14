package dev.jsinco.luma.lumaevents.commands;

import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumacore.manager.commands.AbstractCommandManager;
import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(
        name = "event",
        aliases = {"valentide", "valentines"},
        permission = "lumaevents.default"
)
public class CommandManager extends AbstractCommandManager<EventMain, CommandModule> {

    public CommandManager() {
        super(EventMain.getInstance());
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (args.length > 0) {
            return super.handle(sender, label, args);
        }
        if (!(sender instanceof Player player)) {
            return false;
        }
        Location loc = EventMain.getOkaeriConfig().getEventSpawnLocation();
        if (loc != null) {
            player.teleportAsync(loc.toCenterLocation());
        }
        return true;
    }
}
