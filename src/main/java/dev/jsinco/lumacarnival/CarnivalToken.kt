package dev.jsinco.lumacarnival

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CarnivalToken : Listener {

    private val key = NamespacedKey(CarnivalMain.instance, "carnival-token")

    val CARNIVAL_TOKEN = ItemStack(Material.GOLDEN_APPLE).apply {
        itemMeta = itemMeta?.apply {
            displayName(Util.mm("<b><gradient:#8ec4f7:#ff9ccb>Can</gradient><gradient:#ff9ccb:#d7f58d>died</gradient><gradient:#d7f58d:#fffe8a> Ap</gradient><gradient:#fffe8a:#ffd365>ple</gradient></b>"))
            lore(Util.mml("<gray>Sweetness X", "", "<white>A glistening caramel apple,", "<white>covered in a golden wrap, just", "<white>waiting to be eaten!", "", "<dark_gray>Use at the carnival shop.",
                "",
                "<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st>",
                "<#EEE1D5>Tier • <b><#8EC4F7>C<#C7B0E1>a<#FF9CCB>r<#EBC9AC>n<#D7F58D>i<#FFFE8A>v<#FFE978>a<#FFD365>l</b>",
                "<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st>"))
            addEnchant(Enchantment.DURABILITY, 10, true)
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
        }
    }

    fun isToken(itemStack: ItemStack): Boolean {
        return itemStack.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BOOLEAN) ?: false
    }

    @JvmStatic
    fun give(player: Player?, amt: Int) {
        if (player == null) return
        else if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable { give(player, amt) })
            return
        }
        Util.giveItem(player, CARNIVAL_TOKEN.asQuantity(amt))
        Util.msg(player, "You have received <b><gold>$amt</gold></b> <b><gradient:#8ec4f7:#ff9ccb>Can</gradient><gradient:#ff9ccb:#d7f58d>died</gradient><gradient:#d7f58d:#fffe8a> Ap</gradient><gradient:#fffe8a:#ffd365>ples</gradient></b>!")
    }

    fun take(player: Player, amt: Int): Boolean {
        var totalFound = 0
        for (item in player.inventory.contents) {
            if (item != null && item.isSimilar(CARNIVAL_TOKEN)) {
                totalFound += item.amount
                if (totalFound >= amt) {
                    player.inventory.removeItem(CARNIVAL_TOKEN.asQuantity(amt))
                    return true
                }
            }
        }
        return false
    }

    // prevent people from eating the token
    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        if (isToken(event.item)) {
            event.isCancelled = true
            event.player.sendMessage(Util.mm("<yellow>Bleh, too sweet!"))
        }
    }
}