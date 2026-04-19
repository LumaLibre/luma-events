package dev.lumas.events.commands.modules.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.hooks.VaultService;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.constant.Ranks;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "reclaim",
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> reclaim",
        playerOnly = true
)
public class ReclaimCommand implements CommandModule {
    @Override
    public boolean execute(@NonNull EventMain eventMain, @NonNull CommandSender commandSender, @NonNull String s, @NonNull String @NonNull [] strings) {
        Player player = (Player) commandSender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());

        if (eventPlayer == null || eventPlayer.getDeaths() <= 0) {
            Util.sendMsg(player, "No legacy deaths.");
            return false;
        } else if (eventPlayer.isPaleSide$reclaimed()) {
            Util.sendMsg(player, "You have already reclaimed your legacy lives.");
            return false;
        }

        int totalLives = eventPlayer.getLives() + eventPlayer.getDeaths();
        Ranks rank = Ranks.getRank(player);

        Economy econ = VaultService.getInstance().getEconomy();
        if (econ == null) {
            return false;
        }


        if (totalLives < 3) {
            Util.sendMsg(player, "You need at least 3 legacy lives to reclaim your legacy lives.");
            return false;
        }


        double total = rank.getLegacyPaleSideEntryCost() - rank.getPaleSideEntryCost();
        totalLives -= 3;
        for (int i = 0; i < totalLives; i++) {
            total += rank.getLegacyPaleSideLifeCost() - rank.getPaleSideLifeCost();
        }

        eventPlayer.setPaleSide$reclaimed(true);
        EventPlayerManager.save(eventPlayer);
        econ.depositPlayer(player, total);
        eventPlayer.sendMessage("Reclaimed your legacy life costs <gold>($" + String.format("%,d", (long) total) + ").");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NonNull EventMain eventMain, @NonNull CommandSender commandSender, @NonNull String @NonNull [] strings) {
        return List.of();
    }
}
