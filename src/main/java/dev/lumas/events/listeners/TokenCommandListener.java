package dev.lumas.events.listeners;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.items.TokenSource;
import dev.lumas.events.utility.TokenLog;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;


@Register(Autowire.LISTENER)
public class TokenCommandListener implements Listener {
    // Doing this instead of adding an event to LI because I don't know how well those fare with
    // hot reloading

    private static final Set<String> GIVE_ROOTS = Set.of("lumaitems", "li");
    private static final String GIVE_PERMISSION = "lumaitems.command.give";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        handle(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        handle(event.getSender(), event.getCommand());
    }

    private void handle(CommandSender sender, String rawCommand) {
        String[] args = rawCommand.strip().replaceFirst("^/", "").split("\\s+");
        if (args.length < 3 || !GIVE_ROOTS.contains(stripNamespace(args[0])) || !args[1].equalsIgnoreCase("give")) {
            return;
        }

        TokenExchanging.TokenType type = tokenByName(args[2]);
        if (type == null || !sender.hasPermission(GIVE_PERMISSION)) {
            return;
        }

        int amount = args.length > 4 ? parseAmount(args[4]) : 1;
        if (amount < 1) {
            return;
        }

        TokenSource source = TokenSource.command(sender.getName());
        String target = args.length > 3 ? args[3] : null;

        if (target == null) {
            if (sender instanceof Player player) {
                TokenLog.record(source, player.getName(), player.getUniqueId(), amount, type, null);
            }
            return;
        }

        Player receiver = target.startsWith("@") ? null : Bukkit.getPlayerExact(target);
        if (receiver == null) {
            TokenLog.record(source, target, null, amount, type, "unresolved target");
            return;
        }
        TokenLog.record(source, receiver.getName(), receiver.getUniqueId(), amount, type, null);
    }

    @Nullable
    private TokenExchanging.TokenType tokenByName(String itemName) {
        String normalized = itemName.toLowerCase(Locale.ROOT).replace('_', '-');
        for (TokenExchanging.TokenType type : TokenExchanging.TokenType.values()) {
            if (type.getKey().getKey().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    private int parseAmount(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private String stripNamespace(String root) {
        int separator = root.indexOf(':');
        return (separator == -1 ? root : root.substring(separator + 1)).toLowerCase(Locale.ROOT);
    }
}
