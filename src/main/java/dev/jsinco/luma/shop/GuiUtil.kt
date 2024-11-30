package dev.jsinco.luma.shop

import dev.jsinco.luma.ThanksgivingEvent
import dev.jsinco.luma.Util
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object GuiUtil {

    private val plugin = ThanksgivingEvent.getInstance()

    fun getButtons(): Pair<ItemStack, ItemStack> {
        val back = ItemStack(Material.PAPER)
        val next = ItemStack(Material.PAPER)
        val backMeta = back.itemMeta
        val nextMeta = next.itemMeta
        backMeta.setCustomModelData(10000)
        nextMeta.setCustomModelData(10001)

        backMeta.displayName(Util.color("<red>Back"))
        nextMeta.displayName(Util.color("<red>Next"))

        backMeta.persistentDataContainer.set(NamespacedKey(plugin, "page"), PersistentDataType.BOOLEAN, false)
        nextMeta.persistentDataContainer.set(NamespacedKey(plugin, "page"), PersistentDataType.BOOLEAN, true)

        back.itemMeta = backMeta
        next.itemMeta = nextMeta
        return Pair(back, next)
    }

    fun isGuiButton(item: ItemStack): Boolean {
        return item.itemMeta?.persistentDataContainer?.has(NamespacedKey(plugin, "page"), PersistentDataType.BOOLEAN) ?: false
    }

    fun getGuiButtonDirection(item: ItemStack): Boolean? {
        return item.itemMeta?.persistentDataContainer?.get(NamespacedKey(plugin, "page"), PersistentDataType.BOOLEAN)
    }


    fun basicItem(m: Material): ItemStack {
        return createGuiItem(m, "<black>", listOf(), false)
    }


    fun createGuiItem(material: Material, name: String, lore: List<String>, glow: Boolean): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.displayName(Util.color(name))
        meta.lore(Util.color(lore))
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        item.itemMeta = meta
        return item
    }
}