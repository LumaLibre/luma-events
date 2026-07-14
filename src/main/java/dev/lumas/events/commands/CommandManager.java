package dev.lumas.events.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.command.AbstractCommandManager;
import dev.lumas.events.EventMain;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Register(Autowire.COMMAND)
@CommandMeta(
        name = "event",
        aliases = {"wonderland", "wland", "wonder", "explorer"},
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
