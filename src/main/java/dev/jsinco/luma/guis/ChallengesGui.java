package dev.jsinco.luma.guis;

import dev.jsinco.luma.ChallengeType;
import dev.jsinco.luma.Util;
import dev.jsinco.luma.manager.guis.AbstractGui;
import dev.jsinco.luma.obj.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChallengesGui implements AbstractGui {

    private final Inventory inventory;
    private final EventPlayer eventPlayer;

    public ChallengesGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.inventory = Bukkit.createInventory(this, 27, Util.color("<blue>Winter Challenges"));
        init();
    }

    private void init() {
        // TODO: Borders I guess
        for (ChallengeType challengeEnum : ChallengeType.values()) {
            inventory.setItem(challengeEnum.getInvLoc(), challengeEnum.icon(eventPlayer));
        }

        inventory.setItem(15, Util.createBasicItem(
                Material.CHEST,
                "<b><gold>Claim Reward",
                false,
                List.of("<gray>Click to claim your reward!"),
                List.of("reward_claim"))
        );
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
            Util.sendMsg(onlinePlayer, "<red>You cannot claim this reward.");
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
