package dev.jsinco.luma.lumaevents.tokens;

import dev.jsinco.luma.lumaevents.explorer.custom.EarnTokenExplorerEvent;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListeners;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.manager.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TokenExchanging {

    public static void give(Player player, TokenType type, int amount) {
        if (amount < 1) {
            return;
        }

        ItemStack itemStack = LocalCustomItemManager.getCustomItemStack(type.tokenClass);
        if (itemStack == null) {
            Util.log("<red>Could not give NULL token: " + type.name() + " amount: " + amount + " to: " + player.getName());
            return;
        }
        Util.giveItem(player, itemStack, amount);

        Util.sendMsg(player, "You got <yellow>" + amount  + "</yellow> " + type.customName + "(s)!");

        EarnTokenExplorerEvent explorerEvent = new EarnTokenExplorerEvent(type, amount);
        ExplorerListeners.fire(explorerEvent, player);
    }

    public static boolean take(Player player, TokenType type, int amount) {
        ItemStack itemStack = LocalCustomItemManager.getCustomItemStack(type.tokenClass);

        if (itemStack == null) {
            Util.log("<red>Could not take NULL token: " + type.name() + " amount: " + amount + " from: " + player.getName());
            return false;
        }

        return Util.takeItem(player, itemStack, amount);
    }

    public static int getAmount(Player player, TokenType type) {
        ItemStack token = LocalCustomItemManager.getCustomItemStack(type.tokenClass);
        int total = 0;

        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.isSimilar(token)) {
                total += itemStack.getAmount();
            }
        }
        return total;
    }


    public enum TokenType {
        CARROT(EasterCarrotToken.class, "Carrot"),
        BASKET(EasterBasketToken.class, "Basket"),;

        private final Class<? extends CustomItem> tokenClass;
        private final String customName;

        TokenType(Class<? extends CustomItem> tokenClass, String customName) {
            this.tokenClass = tokenClass;
            this.customName = customName;
        }
    }
}
