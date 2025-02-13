package dev.jsinco.luma.lumaevents.customitems;

import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.BoatRace;
import dev.jsinco.luma.lumaevents.games.logic.Envoys;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.logic.Paintball;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.items.ItemFactory;
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions;
import dev.jsinco.luma.lumaitems.obj.QuickTasks;
import dev.jsinco.luma.lumaitems.util.tiers.Tier;
import kotlin.Pair;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StartMinigameItem extends CustomItemFunctions {

    private static final List<Class<? extends Minigame>> MINIGAMES = List.of(Envoys.class, Paintball.class, BoatRace.class);

    @Override
    public @NotNull Pair<String, ItemStack> createItem() {
        return ItemFactory.builder()
                .name("<b><gradient:#954381:#EC60B0:#EE80C6:#954381>Start Minigame")
                .vanillaEnchants(new Pair<>(Enchantment.UNBREAKING, 10))
                .lore(
                        "<gray>Start a random minigame.",
                        "",
                        "<#EC60B0>Right-click <gray>to use!",
                        "",
                        "<#EC60B0>1hr <gray>cooldown"
                )
                .material(Material.BLAZE_POWDER)
                .persistentData("valentide-start-minigame")
                .tier(Tier.VALENTIDE_2025)
                .buildPair();
    }

    @Override
    public void onRightClick(@NotNull Player player, @NotNull PlayerInteractEvent event) {
        if (QuickTasks.isOnCooldown(this, player.getUniqueId())) {
            Util.sendMsg(player, "You are on cooldown for this item!");
            return;
        } else if (QuickTasks.getActiveCooldowns(this) >= 2) {
            Util.sendMsg(player, "There are too many active cooldowns for this item! (2)");
            return;
        }


        if (MinigameManager.getInstance().tryNewMinigameSafely(Util.getRandFromList(MINIGAMES), true)){
            ItemStack item = event.getItem();
            item.setAmount(item.getAmount() - 1);
            QuickTasks.addCooldown(this, player.getUniqueId(), 72000L);
            Util.broadcast(player.getName() + " has started a minigame!");
        } else {
            Util.sendMsg(player, "Failed to start minigame. Is there another minigame active?");
        }
    }
}
