package dev.jsinco.luma.lumaevents.guis;

import dev.jsinco.luma.lumaevents.challenges.ChallengeType;
import dev.jsinco.luma.lumaevents.items.PresentItem;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumacore.manager.guis.AbstractGui;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengesGui implements AbstractGui {

    private final static Map<Material, List<Integer>> borderItems = new HashMap<>();

    static {
        borderItems.put(Material.LIGHT_BLUE_STAINED_GLASS_PANE, List.of(0, 8, 18, 26));
        borderItems.put(Material.BLUE_ICE, List.of(1, 7, 19, 25));
        borderItems.put(Material.SNOWBALL, List.of(2, 6, 9, 17, 20, 24));
        borderItems.put(Material.CORNFLOWER, List.of(3, 5, 10, 16, 21, 23));
        borderItems.put(Material.TORCHFLOWER, List.of(4, 22));
    }

    private final Inventory inventory;
    private final EventPlayer eventPlayer;

    public ChallengesGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.inventory = Bukkit.createInventory(this, 27, Util.color("<b><#CCD8E9>W<#BCBED5>i<#ACA4C0>n<#9C8AAC>t<#BBA1B6>e<#D9B7C0>r <#F6D5C9>C<#F4DCC7>h<#F2E2C6>a<#F0E9C4>l<#E5E4B8>l<#D9DFAC>e<#CEDAA0>n<#BBD7AE>g<#A7D5BB>e<#94D2C9>s"));
        init();
    }

    private void init() {
        for (ChallengeType challengeEnum : ChallengeType.values()) {
            inventory.setItem(challengeEnum.getInvLoc(), challengeEnum.icon(eventPlayer));
        }

        ItemStack complete = Util.createBasicItem(
                Material.PLAYER_HEAD,
                "<b><gold>Claim Reward",
                false,
                List.of("<gray>Click to claim your reward!"),
                List.of("reward_claim"));
        Util.setPlayerHead(complete, PresentItem.Companion.getBASE_64_TEXTURE());
        inventory.setItem(15, complete);

        for (Material material : borderItems.keySet()) {
            List<Integer> slots = borderItems.get(material);
            for (int slot : slots) {
                inventory.setItem(slot, Util.createBasicItem(material, "", false, List.of(), List.of()));
            }
        }
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (!Util.hasPersistentKey(clickedItem, "reward_claim")) {
            return;
        }

        Player onlinePlayer = (Player) event.getWhoClicked();
        if (eventPlayer.claimReward(onlinePlayer)) {
            Util.sendMsg(onlinePlayer, "<green>You have claimed your reward!");
            onlinePlayer.closeInventory();
        } else {
            Util.sendMsg(onlinePlayer, "<red>You cannot claim this");
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {

    }

    @Override
    public void open(HumanEntity humanEntity) {
        humanEntity.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
