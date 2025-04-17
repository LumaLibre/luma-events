package dev.jsinco.luma.lumaevents.npc.module;

import dev.jsinco.luma.lumacore.manager.commands.AbstractCommandManager;
import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(
        name = "anais",
        permission = "lumaevents.internal",
        usage = "/<command> <player> <subcommand> <args?>"
)
public class AnaisModuleManager extends AbstractCommandManager<EventMain, NPCCommandModule> {

    public AnaisModuleManager() {
        super(EventMain.getInstance());
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        // First arg should be the player Anais is talking to
        String targetPlayerName = args.length > 1 ? args[0] : ""; // need at least 2 args
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            Util.sendMsg(sender, "Player not found: " + targetPlayerName);
            return false;
        }

        NPCCommandModule subCommand = this.subCommands.get(args[1]);
        if (subCommand == null) {
            return false;
        }

        if (subCommand.playerOnly() && !(sender instanceof Player)) {
            return false;
        } else if (subCommand.permission() != null && !sender.hasPermission(subCommand.permission())) {
            return false;
        } else {
            String[] newArgs = new String[args.length - 2];
            System.arraycopy(args, 1, newArgs, 0, args.length - 2);
            if (!subCommand.execute(this.plugin, targetPlayer, label, newArgs)) {
                Util.sendMsg(sender, "Invalid usage. Usage: " + subCommand.info().usage());
            }

            return true;
        }
    }

    @Override
    public @Nullable List<String> handleTabComplete(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return null; // Player list
        }
        if (args.length == 2) {
            List<String> subCommandNames = new ArrayList<>();
            for (NPCCommandModule subCommand : this.subCommands.values()) {
                if (subCommand.permission() != null && !sender.hasPermission(subCommand.permission())) {
                    continue;
                }
                subCommandNames.add(subCommand.name());
            }
            return subCommandNames;
        } else {
            NPCCommandModule subCommand = this.subCommands.get(args[1]);
            if (subCommand != null) {
                String[] newArgs = new String[args.length - 2];
                System.arraycopy(args, 1, newArgs, 0, args.length - 2);
                return subCommand.tabComplete(this.plugin, sender, newArgs);
            }
            return null;
        }
    }
}
