package dev.jsinco.luma.lumaevents.customitems;

import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.items.ItemFactory;
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions;
import dev.jsinco.luma.lumaitems.util.tiers.Tier;
import kotlin.Pair;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SwapTeamsItem extends CustomItemFunctions {

    public static final List<UUID> VALID_SWAP_TEAMS = new ArrayList<>();
    public static final String KEY = "valentide-swap-teams";

    @Override
    public @NotNull Pair<String, ItemStack> createItem() {
        return ItemFactory.builder()
                .name("<b><gradient:#954381:#EC60B0:#EE80C6:#954381>Trade Teams")
                .vanillaEnchants(new Pair<>(Enchantment.UNBREAKING, 10))
                .lore(
                        "<gray>Abandon your current",
                        "<gray>team to join another.",
                        "",
                        "<gray>Points and scores",
                        "<gray>will be carried over.",
                        "",
                        "<#EC60B0>Right-click <gray>to use!"
                )
                .material(Material.SUGAR)
                .persistentData(KEY)
                .tier(Tier.VALENTIDE_2025)
                .buildPair();
    }

    @Override
    public void onRightClick(@NotNull Player player, @NotNull PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

        StringBuilder message = new StringBuilder();
        for (EventTeamType teamType : EventTeamType.values()) {
            message.append(teamType.getGradient())
                    .append("<click:suggest_command:/event swapteams ").append(teamType.name()).append(">")
                    .append("<hover:show_text:'Click to join this team.'>[").append(teamType.getFormatted()).append("]</hover></click>")
                    .append(" ");
        }
        message.append("<gold><click:run_command:/event swapteams cancel><hover:show_text:'Click to cancel.'>[Cancel]</hover></click>");
        Util.sendMsg(player, "Choose a team to swap to<gray>:");
        Util.sendMsg(player, message.toString());
        VALID_SWAP_TEAMS.add(player.getUniqueId());
    }
}
