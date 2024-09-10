package dev.jsinco.lumacarnival.shop

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
        val file  = FileManager("shop.yml")
        val yaml = file.generateYamlFile()
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
            val sectionName = item.itemMeta.displayName()
                ?.let { PlainTextComponentSerializer.plainText().serialize(it).replace(" ", "_") } ?: item.type.name
            yaml.set("$sectionName.item", item)
            yaml.set("$sectionName.command", command)
            yaml.set("$sectionName.price", price)
            yaml.set("$sectionName.slot", slot)
            file.saveFileYaml()
        }

        fun removeShopItem(sectionName: String) {
            yaml.set(sectionName, null)
            file.saveFileYaml()
        }

        fun openShop(player: Player) {
            player.openInventory(shopPages.getPage(0))
        }
    }

    override fun getInventory(): Inventory {
        return shopPages.getPage(0)
    }

    init {
        reloadShop()
    }


    fun getShopItem(sectionName: String): ShopItem {
        val item = yaml.getItemStack("$sectionName.item") ?: throw NullPointerException("Item not found")
        val command = yaml.getString("$sectionName.command") ?: throw NullPointerException("Command not found")
        val tokenPrice = yaml.getInt("$sectionName.price")
        val slot = yaml.getInt("$sectionName.slot")
        return ShopItem(item, command, tokenPrice, slot)
    }

    fun getAllShopItems(): MutableList<ShopItem> {
        val list: MutableList<ShopItem> = mutableListOf()
        for (key in yaml.getKeys(false)) {
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
        shopPages = PaginatedGui("<b><#8EC4F7>C<#B4B7E8>a<#D9A9DA>r<#FF9CCB>n<#F2BAB6>i<#E4D7A2>v<#D7F58D>a<#E4F88C>l <#FFFE8A>S<#FFF07E>h<#FFE171>o<#FFD365>p</b>"
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

}