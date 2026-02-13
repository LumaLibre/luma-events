package dev.lumas.events.items;

import dev.lumas.lumacore.utility.Logging;
import dev.lumas.events.EventMain;
import dev.lumas.events.utility.Util;
import dev.lumas.lumaitems.LumaItems;
import dev.lumas.lumaitems.model.CustomItem;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.internal.parser.TokenType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static void give(@NotNull Player player, @NotNull TokenType type, int amount) {
        give(player, type, amount, null);
    }

    public static void give(@NotNull Player player, @NotNull TokenType type, int amount, @Nullable String source) {
        if (amount < 1) {
            return;
        }
        final int finalAmount = (int) Math.ceil(EventMain.getOkaeriConfig().getTokenMultiplier() * amount);

        ItemStack itemStack = LocalCustomItemManager.getCustomItemStack(type.tokenClass);
        if (itemStack == null) {
            Logging.log("<red>Could not give NULL token: " + type.name() + " amount: " + finalAmount + " to: " + player.getName());
            return;
        }
        Util.giveItem(player, itemStack, finalAmount);

        String msg = "You got <gold> " + finalAmount + "</gold> " + type.customName + "(s)!";
        if (source != null) {
            msg += "<dark_gray> (" + source + ")";
        }
        Util.sendMsg(player, msg);
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
        CANDIED_APPLE(CandiedAppleItem.class, "<gradient:#DD2785:#fc5b8d:#cb354e>Candied Apple</gradient>", "candied-apple"),
        CARAMEL_APPLE(CaramelAppleItem.class, "<gradient:#a76d3c:#d2b48c:#c79c6b:#f3e7c4:#9b7652>Caramel Apple</gradient>", "caramel-apple"),
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
