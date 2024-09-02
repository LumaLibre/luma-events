package dev.jsinco.lumacarnival

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CarnivalToken {

    private val key = NamespacedKey(CarnivalMain.instance, "carnival-token")

    val CARNIVAL_TOKEN = ItemStack(Material.GOLDEN_APPLE).apply {
        itemMeta = itemMeta?.apply {
            displayName(Util.mm("<b><gradient:#8ec4f7:#ff9ccb>Can</gradient><gradient:#ff9ccb:#d7f58d>died</gradient><gradient:#d7f58d:#fffe8a> Ap</gradient><gradient:#fffe8a:#ffd365>ple</gradient></b>"))
            //lore(Util.mml("<light_gray>A golden apple that has been candied!"))
            addEnchant(Enchantment.DURABILITY, 10, true)
            persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
        }
    }

    fun isToken(itemStack: ItemStack): Boolean {
        return itemStack.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BOOLEAN) ?: false
    }

    fun give(player: Player, amt: Int) {
        for (i in 0..35) {
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)!!.isSimilar(CARNIVAL_TOKEN)) {
                player.inventory.addItem(CARNIVAL_TOKEN)
                break
            } else if (i == 35) {
                player.world.dropItem(player.location, CARNIVAL_TOKEN)
            }
        }
    }

    fun take(player: Player, amt: Int): Boolean {
        var totalFound = 0
        for (item in player.inventory.contents) {
            if (item != null && item.isSimilar(CARNIVAL_TOKEN)) {
                totalFound += item.amount
                if (totalFound >= amt) {
                    player.inventory.removeItem(CARNIVAL_TOKEN)
                    return true
                }
            }
        }
        return false
    }
}