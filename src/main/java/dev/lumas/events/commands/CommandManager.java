package dev.lumas.events.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.command.AbstractCommandManager;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Register(Autowire.COMMAND)
@CommandMeta(
        name = "event",
        aliases = {"lumares", "lumaween", "halloween", "hallo"},
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

        Config config = EventMain.getConfiguration();

        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());

        if (eventPlayer != null && !eventPlayer.isInitiallySpawned() && config.getInitialSpawnLocation() != null) {
            Executors.teleportSafely(player, config.getInitialSpawnLocation().toCenterLocation());
            eventPlayer.setInitiallySpawned(true);
            return true;
        }

        Location loc = config.getEventSpawnLocation();

        if (loc != null) {
            Executors.teleportSafely(player, loc.toCenterLocation());
        }
        return true;
    }
}
