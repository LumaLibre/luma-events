package dev.jsinco.luma.shop

import dev.jsinco.luma.Util
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class PaginatedGui (
    val name: String,
    private val base: Inventory,
    shopItems: List<ShopItem>,
    startEndSlots: Pair<Int, Int>,
    ignoredSlots: List<Int>,
) {

    val pages: MutableList<Inventory> = mutableListOf()
    val isEmpty = shopItems.isEmpty()
    var size : Int = 0
        private set


    init {
        val shopItemsOrderBySlot = shopItems.sortedBy { it.slot }
        val items: List<ItemStack> = shopItemsOrderBySlot.map { it.displayItem }

        var currentPage = newPage()
        var currentItem = 0
        var currentSlot = startEndSlots.first
        while (currentItem < items.size) {
            if (ignoredSlots.contains(currentSlot)) {
                currentSlot++
                continue
            }

            if (currentSlot == startEndSlots.second) {
                currentPage = newPage()
                currentSlot = startEndSlots.first
            }

            if (currentPage.getItem(currentSlot) == null) {
                currentPage.setItem(currentSlot, items[currentItem])
                currentItem++
            }
            currentSlot++
        }
        size = pages.size
    }

    private fun newPage(): Inventory {
        val inventory: Inventory = Bukkit.createInventory(base.holder, base.size, Util.color(name))
        for (i in 0 until base.size) {
            inventory.setItem(i, base.getItem(i))
        }
        pages.add(inventory)
        return inventory
    }


    fun getPage(page: Int): Inventory {
        return pages[page]
    }

    fun indexOf(page: Inventory): Int {
        return pages.indexOf(page)
    }
}