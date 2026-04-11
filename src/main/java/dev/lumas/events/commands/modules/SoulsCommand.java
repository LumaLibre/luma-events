package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "souls",
        permission = "lumaevents.admin",
        parent = CommandManager.class,
        usage = "/<command> souls <player> <add|set|remove> <amount>"
)
public class SoulsCommand implements CommandModule {

    private static final List<String> OPERATIONS = List.of("add", "set", "remove");

    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            Util.sendMsg(sender, "Usage: /<command> souls <player> <add|set|remove> <amount>");
            return true;
        }

        String playerName = args[0];
        String operation = args[1].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Util.sendMsg(sender, "Invalid amount: <yellow>" + args[2]);
            return true;
        }

        if (!OPERATIONS.contains(operation)) {
            Util.sendMsg(sender, "Unknown operation <yellow>" + operation + "</yellow>. Use: add/set/remove.");
            return true;
        }

        if (amount < 0) {
            Util.sendMsg(sender, "Amount must be non-negative.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();
        if (target.getName() == null && !target.hasPlayedBefore()) {
            Util.sendMsg(sender, "Player not found: <yellow>" + playerName);
            return true;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(uuid);
        int before = eventPlayer.getSouls();
        int after = switch (operation) {
            case "add" -> before + amount;
            case "set" -> amount;
            case "remove" -> Math.max(0, before - amount);
            default -> before;
        };

        eventPlayer.setSouls(after);

        String targetName = target.getName() != null ? target.getName() : uuid.toString();
        Util.sendMsg(sender, "Set <yellow>" + targetName + "</yellow>'s souls: <yellow>" + before + "</yellow> → <yellow>" + after);

        if (target.isOnline()) {
            eventPlayer.sendMessage("Your souls have been updated: <yellow>" + before + "</yellow> → <yellow>" + after);
        }

        Executors.runAsync(() -> EventPlayerManager.save(eventPlayer));
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return OPERATIONS.stream().filter(op -> op.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
