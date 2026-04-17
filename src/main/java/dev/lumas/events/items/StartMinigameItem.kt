package dev.lumas.events.items

import dev.lumas.events.games.MinigameManager
import dev.lumas.events.utility.Util
import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import dev.lumas.lumaitems.util.extensions.QuickTasks
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack


class StartMinigameItem : CustomItemFunctions() {
    override fun createItem(): Pair<String, ItemStack> {
        fun grad(s: String): String {
            return "<gradient:#5d85dc:#E56A91:#F3AA4C:#CA51CB>$s</gradient>"
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
            .persistentData("start-minigame")
            .tier(Tier.WONDERLAND_2026)
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

        val item = event.item ?: return


        if (MinigameManager.getInstance().tryNewMinigameSafely(true)) {
            item.amount -= 1
            QuickTasks.addCooldown(this, player.uniqueId, 72000L)
            Util.broadcast(player.name + " has started a minigame!")
        } else {
            Util.sendMsg(player, "Failed to start minigame. Is there another minigame active?")
        }
    }
}
