package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.utility.Executors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
    name = "clearglobalflight",
    permission = "lumaevents.admin",
    parent = CommandManager.class,
    usage = "/<command> clearglobalflight"
)
public class ClearFlightCommand implements CommandModule {
    @Override
    public boolean execute(EventMain plugin, CommandSender sender, String label, String[] args) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Executors.sync(player, () -> {
                if (player.getAllowFlight()) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                sender.sendMessage("Cleared flight for " + player.getName());
            });
        }
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(EventMain plugin, CommandSender sender, String[] args) {
        return List.of();
    }
}
