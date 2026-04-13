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
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.team.EventTeam;
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

        eventPlayer.sendNoPrefixedMessage(BORDER);
        eventPlayer.sendNoPrefixedMessage("Stats for: <gold>" + player.getName() + " " + Util.getRandom(FACES));
        eventPlayer.sendNoPrefixedMessage(Text.mm("Team: ").append((eventTeam == null ? Text.mm("<gray>None") : eventTeam.getDisplayName())));
        eventPlayer.sendNoPrefixedMessage("Souls: <gold>" + eventPlayer.getSouls());
        eventPlayer.sendNoPrefixedMessage("Suspended: <gold>" + (eventPlayer.isSuspended() ? "Yes" : "No"));
        for (var entry : eventPlayer.getPermanentScores().entrySet()) {
            eventPlayer.sendNoPrefixedMessage(Util.formatSnakeCase(entry.getKey().name()) + ": <gold>" + entry.getValue());
        }
        eventPlayer.sendNoPrefixedMessage(BORDER);
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(EventMain plugin, CommandSender sender, String[] args) {
        return null;
    }
}
