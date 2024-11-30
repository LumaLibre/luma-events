package dev.jsinco.luma.shop

import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class ShopManager : InventoryHolder {

    companion object {
        val file = SnakeYamlConfig("shop.yml")
        lateinit var shopPages: PaginatedGui
        lateinit var shopItems: MutableList<ShopItem>


        fun getShopItemFromDisplayItem(displayItem: ItemStack): ShopItem? {
            for (shopItem in shopItems) {
                if (shopItem.displayItem == displayItem) {
                    return shopItem
                }
            }
            return null
        }

        fun addShopItem(item: ItemStack, command: String, price: Int, slot: Int) {
            val sectionName = getSectionName(item)
            file.set("$sectionName.item", BukkitSerialization.itemStackToBase64(item))
            file.set("$sectionName.command", command)
            file.set("$sectionName.price", price)
            file.set("$sectionName.slot", slot)
            file.save()
        }

        fun removeShopItem(sectionName: String) {
            file.set(sectionName, null)
            file.save()
        }

        fun openShop(player: Player) {
            player.openInventory(shopPages.getPage(0))
        }

        fun getSectionName(item: ItemStack) = item.itemMeta.displayName()
            ?.let { PlainTextComponentSerializer.plainText().serialize(it).replace(" ", "_") } ?: item.type.name
    }

    override fun getInventory(): Inventory {
        return shopPages.getPage(0)
    }

    init {
        reloadShop()
    }


    fun getShopItem(sectionName: String): ShopItem {
        val item = BukkitSerialization.itemStackFromBase64(file.getString("$sectionName.item")) ?: throw NullPointerException("Item not found")

        val command = file.getString("$sectionName.command") ?: throw NullPointerException("Command not found")
        val tokenPriceRaw = file.get("$sectionName.price")
        val tokenPrice: Int = if (tokenPriceRaw !is Int) {
            (tokenPriceRaw as Double).toInt()
        } else {
            tokenPriceRaw
        }
        val slot = file.getInt("$sectionName.slot")
        return ShopItem(item, command, tokenPrice, slot)
    }

    fun getAllShopItems(): MutableList<ShopItem> {
        val list: MutableList<ShopItem> = mutableListOf()
        for (key in file.keys) {
            if (key == "purchases") continue
            list.add(getShopItem(key))
        }
        return list
    }


    private fun baseInventory(): Inventory {
        val inventory: Inventory = Bukkit.createInventory(this, 54, Component.text("Easter Shop Base"))
        val buttons = GuiUtil.getButtons()
        val items: Map<ItemStack, List<Int>> = mapOf(
            GuiUtil.basicItem(Material.GREEN_STAINED_GLASS_PANE) to listOf(0, 8, 45, 53),
            GuiUtil.basicItem(Material.SHORT_GRASS) to listOf(1, 7, 46, 52),
            GuiUtil.basicItem(Material.FERN) to listOf(2, 6, 47, 51),
            GuiUtil.basicItem(Material.PINK_TULIP) to listOf(3, 5),
            GuiUtil.basicItem(Material.LILY_PAD) to listOf(4, 49),
            buttons.first to listOf(48),
            buttons.second to listOf(50)
        )

        for (item in items) {
            for (slot in item.value) {
                inventory.setItem(slot, item.key)
            }
        }
        return inventory
    }

    fun reloadShop() {
        shopItems = getAllShopItems()
        shopPages = PaginatedGui("<b><#536C40>E<#948D57>x<#D4AE6D>c<#D29B57>h<#CF8741>a<#B85C40>n<#A95F3B>g<#996236>e</b>"
            , baseInventory(), shopItems, Pair(20, 34), ignoredSlots = listOf(25, 26, 27, 28))
    }

    fun getNextPage(inventory: Inventory): Inventory? {
        val indexOfPage = shopPages.pages.indexOf(inventory)

        if (indexOfPage + 1 >= shopPages.pages.size) {
            return null
        }

        return shopPages.pages[indexOfPage + 1]
    }

    fun getPreviousPage(inventory: Inventory): Inventory? {
        val indexOfPage = shopPages.pages.indexOf(inventory)

        if (indexOfPage - 1 < 0) {
            return null
        }

        return shopPages.pages[indexOfPage - 1]
    }


    fun getPurchaseAmount(player: Player, shopItem: ShopItem): Int {
        val value = file.get("purchases.${getSectionName(shopItem.item)}.${player.uniqueId}")
        if (value is Int) {
            return value
        } else if (value is Double) {
            return value.toInt()
        }
        return 0
    }

    fun setItemPurchased(player: Player, shopItem: ShopItem, amount: Int) {
        file.set("purchases.${getSectionName(shopItem.item)}.${player.uniqueId}", amount)
        file.save()
    }

}