package dev.jsinco.luma.lumaevents.archives.guis;

import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;
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

public class ChallengesGui extends AbstractGui {

    private final static Map<Material, List<Integer>> borderItems = new HashMap<>();

    static {
        borderItems.put(Material.LIGHT_BLUE_STAINED_GLASS_PANE, List.of(0, 8, 18, 26));
        borderItems.put(Material.BLUE_ICE, List.of(1, 7, 19, 25));
        borderItems.put(Material.SNOWBALL, List.of(2, 6, 9, 17, 20, 24));
        borderItems.put(Material.CORNFLOWER, List.of(3, 5, 10, 21, 23));
        borderItems.put(Material.TORCHFLOWER, List.of(4, 22));
    }

    private final Inventory inventory;
    private final EventPlayer eventPlayer;

    public ChallengesGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.inventory = Bukkit.createInventory(this, 27, Util.color("<b><#b986f9>Christmas Challenges"));
        init();
    }

    private void init() {
        for (Material material : borderItems.keySet()) {
            List<Integer> slots = borderItems.get(material);
            for (int slot : slots) {
                inventory.setItem(slot, Util.createBasicItem(material, "", false, List.of(), List.of()));
            }
        }

        for (ChallengeType challengeEnum : ChallengeType.values()) {
            Challenge challenge = eventPlayer.getChallenge(challengeEnum, false);
            inventory.setItem(challengeEnum.getInvLoc(), challengeEnum.icon(challenge));
            challenge.passiveImpl(eventPlayer);
        }

        ItemStack complete = Util.createBasicItem(
                Material.PLAYER_HEAD,
                "<b><gold>Claim Reward",
                false,
                List.of("<gray>Click to claim your reward!"),
                List.of("reward_claim"));
        Util.setPlayerHead(complete, PresentItem.BASE_64_TEXTURE);
        inventory.setItem(16, complete);
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
        if (eventPlayer.claimChallengesReward(onlinePlayer)) {
            Util.sendMsg(onlinePlayer, "<green>You have claimed your reward!");
            onlinePlayer.closeInventory();
        } else {
            Util.sendMsg(onlinePlayer, "<red>You cannot claim this.");
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
