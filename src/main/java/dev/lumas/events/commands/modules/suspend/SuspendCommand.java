package dev.lumas.events.commands.modules.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.hooks.VaultService;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.constant.Ranks;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "suspend",
        permission = "lumaevents.default",
        description = "Suspend a player/yourself",
        parent = CommandManager.class,
        usage = "/<command> suspend",
        playerOnly = true
)
public class SuspendCommand implements CommandModule {

    @Override
    public boolean execute(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (!EventMain.getOkaeriConfig().getExplorer().isExplorerOrders()) {
            commandSender.sendMessage("Not enabled.");
            return false;
        }

        Player target = (Player) commandSender;

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(target.getUniqueId());
        boolean confirmed = Arrays.asList(strings).contains("--confirm");

        Minigame current = MinigameManager.getInstance().getCurrent();
        if (current.getParticipants().contains(eventPlayer)) {
            eventPlayer.sendMessage("You can't suspend while participating in a minigame.");
            return true;
        }

        if (eventPlayer.isSuspended()) {
            eventPlayer.sendMessage("You are already suspended.");
            return true;
        }


        Ranks rank = Ranks.getRank(target);

        if (rank.ordinal() < Ranks.STELLARIS.ordinal()) {
            eventPlayer.sendMessage("Only players with <gold>Stellaris</gold> or higher can participate in this.");
            return true;
        }

        if (isLivesNeeded(eventPlayer, rank)) {

            int purchased = purchaseLives(eventPlayer, rank);
            if (confirmed && purchased > 0) {
                eventPlayer.setLives(eventPlayer.getLives() + purchased);
                EventPlayerManager.save(eventPlayer);
            } else {
                return true;
            }
        }

        eventPlayer.suspend();
        eventPlayer.sendMessage("You have been suspended. You have <gold>" + eventPlayer.getLives() + "</gold> Pale Side lives left.");
        EventPlayerManager.save(eventPlayer);
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String[] strings) {
        return List.of("--confirm");
    }


    private boolean isLivesNeeded(EventPlayer eventPlayer, Ranks rank) {
        if (eventPlayer.getLives() <= 0) {
            eventPlayer.sendMessage("You have no Pale Side lives. Purchase a life by running this command again with <gold>--confirm</gold> at the end.");
            if (eventPlayer.getActiveExplorerMiles().isEmpty()) {
                eventPlayer.sendMessage("This will cost you <gold>" + String.format("%,.2f", rank.getPaleSideEntryCost()) + "</gold> to purchase. (3 lives)");
            } else {
                eventPlayer.sendMessage("This will cost you <gold>" + String.format("%,.2f", rank.getPaleSideLifeCost()) + "</gold> to purchase. (1 life)");
            }
            return true;
        }
        return false;
    }

    private int purchaseLives(EventPlayer eventPlayer, Ranks rank) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return 0;
        }

        Economy econ = VaultService.getInstance().getEconomy();
        if (econ == null) {
            eventPlayer.sendMessage("No economy provider found.");
            return 1;
        }

        EconomyResponse response;
        int expectedAmount;

        if (eventPlayer.getActiveExplorerMiles().isEmpty()) { // initial purchase
            response = econ.withdrawPlayer(player, rank.getPaleSideEntryCost());
            expectedAmount = 3;
        } else {
            response = econ.withdrawPlayer(player, rank.getPaleSideLifeCost());
            expectedAmount = 1;
        }

        if (response.transactionSuccess()) {
            // lazy log
            EventMain.getInstance().getLogger().info("Purchased " + expectedAmount + " Pale Side live(s) for " + player.getName());
            eventPlayer.sendMessage("Purchased " + expectedAmount + " Pale Side live(s).");
            return expectedAmount;
        }
        eventPlayer.sendMessage("You do not have enough money to purchase Pale Side live(s).");
        return 0;
    }
}
