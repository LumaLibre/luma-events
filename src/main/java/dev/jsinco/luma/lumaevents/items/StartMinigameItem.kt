package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaevents.games.MinigameManager
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.QuickTasks
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack


class StartMinigameItem : CustomItemFunctions() {
    override fun createItem(): Pair<String, ItemStack> {
        fun grad(s: String): String {
            return "<gradient:#ff4e50:#fc913a:#f9d62e:#eae374:#97c753>$s</gradient>"
        }

        return ItemFactory.builder()
            .name("<b>${grad("Start Minigame")}</b>")
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .lore(
                "<gray>Start a random minigame.</gray>",
                "",
                "${grad("Right-click")} <gray>to use!</gray>",
                "",
                "<red>Cooldown: 1h</red>",
            )
            .material(Material.BLAZE_POWDER)
            .persistentData("summer-start-minigame")
            .tier(Tier.SUMMER_2025)
            .buildPair()
    }

    override fun onRightClick(player: Player, event: PlayerInteractEvent) {
        if (QuickTasks.isOnCooldown(this, player.uniqueId)) {
            Util.sendMsg(player, "You are on cooldown for this item!")
            return
        } else if (QuickTasks.getActiveCooldowns(this) >= 2) {
            Util.sendMsg(player, "There are too many active cooldowns for this item! (2)")
            return
        }


        if (MinigameManager.getInstance().tryNewMinigameSafely(true)) {
            val item = event.item
            item!!.amount = item.amount - 1
            QuickTasks.addCooldown(this, player.uniqueId, 72000L)
            Util.broadcast(player.name + " has started a minigame!")
        } else {
            Util.sendMsg(player, "Failed to start minigame. Is there another minigame active?")
        }
    }
}
