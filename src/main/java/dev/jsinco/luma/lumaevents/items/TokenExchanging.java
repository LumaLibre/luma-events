package dev.jsinco.luma.lumaevents.items;

import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.LumaItems;
import dev.jsinco.luma.lumaitems.manager.CustomItem;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TokenExchanging {

//    public static void giveWithChances(Player player, int amount) {
//        int amountClone = amount;
//        for (int i = 0; i < amount; i++) {
//            TokenType type = TokenType.OPAL;
//            if (Util.RANDOM.nextInt(101) < 3) { // 3% chance to give refined opal
//                type = TokenType.REFINED_OPAL;
//                Util.sendMsg(player, "You got <yellow>1</yellow> " + TokenType.REFINED_OPAL.customName + "!");
//                amountClone--;
//            }
//            give(player, type, 1);
//        }
//        if (amountClone <= 0) {
//            return;
//        }
//        Util.sendMsg(player, "You got <yellow>" + amountClone + "</yellow> " + TokenType.OPAL.customName + "(s)!");
//    }

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
        Util.sendMsg(player, "You got <gold>" + amount + "</gold> " + type.customName + "(s)!");

    }

    public static boolean take(Player player, TokenType type, int amount) {
        return Util.takeItem(player, type.namespace, amount);
    }

    public static int getAmount(Player player, TokenType type) {
        int total = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || !itemStack.hasItemMeta()) {
                continue;
            }

            if (itemStack.getPersistentDataContainer().has(type.namespace)) {
                total += itemStack.getAmount();
            }
        }
        return total;
    }


    @Getter
    public enum TokenType {
        POWDER(HallowsPowderItem.class, "Powder", "hallows-powder"),
        //REFINED_OPAL(RefinedSummerOpal.class, "Refined Opal", "refined-summer-opal"),
        ;

        private final Class<? extends CustomItem> tokenClass;
        private final String customName;
        private final NamespacedKey namespace;

        TokenType(Class<? extends CustomItem> tokenClass, String customName, String namespace) {
            this.tokenClass = tokenClass;
            this.customName = customName;
            this.namespace = new NamespacedKey(LumaItems.getInstance(), namespace);
        }
    }
}
