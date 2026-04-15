package dev.lumas.events.commands.modules.gui;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.explorer.gui.ExplorerOrderGui;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "orders",
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> orders",
        playerOnly = true
)
public class ExplorerOrderCommand implements CommandModule {

    @Override
    public boolean execute(EventMain plugin, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        ExplorerOrderGui gui = new ExplorerOrderGui(eventPlayer);
        gui.open(player);
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(EventMain plugin, CommandSender sender, String[] args) {
        return List.of();
    }
}
