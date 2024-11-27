package dev.jsinco.luma.commands.subcommands;

import dev.jsinco.luma.PrisonMinePlayer;
import dev.jsinco.luma.PrisonMinePlayerManager;
import dev.jsinco.luma.ThanksgivingEvent;
import dev.jsinco.luma.Util;
import dev.jsinco.luma.commands.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class MineUpgrade implements Subcommand {
    @Override
    public void execute(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        Player player = (Player) sender;
        PrisonMinePlayer prisonMinePlayer = PrisonMinePlayerManager.getByUUID(player.getUniqueId());
        Map<String, Integer> mines = ThanksgivingEvent.getOkaeriConfig().getMines();

        List<String> mineList = List.copyOf(mines.keySet());
        int currentIndex = mineList.indexOf(prisonMinePlayer.getCurrentMine());


        for (Map.Entry<String, Integer> entry : mines.entrySet()) {
            String mine = entry.getKey();
            int cost = entry.getValue();
            if (prisonMinePlayer.getCurrentMine().equals(mine)) {
                if (prisonMinePlayer.getPoints() >= cost) {


                    // set their mine to the next one
                    if (currentIndex == mineList.size() - 1) {
                        Util.sendMsg(player, "You have reached the maximum mine level.");
                    } else {
                        prisonMinePlayer.setPoints(prisonMinePlayer.getPoints() - cost);
                        String newMine = mineList.get(currentIndex + 1);
                        prisonMinePlayer.setCurrentMine(newMine);
                        // TODO: ADD PERMISSION
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set jetsprisonmines.break." + newMine);
                        Util.sendMsg(player, "You have upgraded your mine to <gold>" + mine + "<reset>.");
                    }
                } else {
                    Util.sendMsg(player,"You do not have enough tokens to upgrade your mine to <gold>" + mine + "<reset>. <gray>(" + cost + " tokens required)");
                }
                return;
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, ThanksgivingEvent plugin) {
        return List.of();
    }

    @Override
    public String permission() {
        return "lumaevent.default";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
