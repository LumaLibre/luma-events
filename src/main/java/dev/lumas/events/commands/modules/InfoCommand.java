package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.util.Text;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.model.team.EventTeamPlayerHandle;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "info",
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> info <player?>"
)
public class InfoCommand implements CommandModule {

    private static final String BORDER = "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ";
    private static final String[] FACES = {"♥(ˆ⌣ˆԅ)", "(• ε •)", "✧♡(◕‿◕✿)", "(๑′ᴗ‵๑)", "(☞ﾟ∀ﾟ)☞", "(╯°o°)ᕗ", "ʕ•ᴥ•ʔ", "(´°ω°`)", "(¬_¬)"};

    @Override
    public boolean execute(EventMain plugin, CommandSender sender, String label, String[] args) {
        OfflinePlayer player = args.length > 0 ? Bukkit.getOfflinePlayerIfCached(args[0])
                : sender instanceof Player ? (Player) sender : null;

        if (player == null) {
            Util.sendMsg(sender, "Player not found");
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        EventTeam eventTeam = EventTeamManager.getByMember(eventPlayer);
        EventTeamPlayerHandle handle = eventTeam == null ? null : eventTeam.getMember(eventPlayer);

        sender.sendMessage(Text.mm(BORDER));
        sender.sendMessage(Text.mm("Stats for: <gold>" + player.getName() + " " + Util.getRandom(FACES)));
        sender.sendMessage(Text.mm("Team: " + (eventTeam == null ? "<gray>None" : eventTeam.getDisplayName() + " <gold>(" + eventTeam.getOnlineMembers() + "/" + eventTeam.getTotalMembers() + ")")));
        sender.sendMessage(Text.mm("Souls: <gold>" + eventPlayer.getSouls()));
        sender.sendMessage(Text.mm("Points: <gold>" + (handle != null ? handle.getPoints() : "0") + ")"));
        sender.sendMessage(Text.mm("Suspended: <gold>" + (eventPlayer.isSuspended() ? "Yes" : "No")));
        for (var entry : eventPlayer.getPermanentScores().entrySet()) {
            sender.sendMessage(Text.mm(Util.formatSnakeCase(entry.getKey().name()) + ": <gold>" + entry.getValue()));
        }
        sender.sendMessage(Text.mm(BORDER));
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(EventMain plugin, CommandSender sender, String[] args) {
        return null;
    }
}
