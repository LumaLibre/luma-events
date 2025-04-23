package dev.jsinco.luma.lumaevents.utility.gui

import dev.jsinco.luma.lumaevents.utility.Util
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class PaginatedGui (
    val name: String,
    private val base: Inventory,
    items: List<ItemStack>,
    startEndSlots: Pair<Int, Int>,
    ignoredSlots: List<Int>,
) {

    val pages: MutableList<Inventory> = mutableListOf()
    val isEmpty = items.isEmpty()
    var size : Int = 0
        private set


    init {
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

    fun getFirst(): Inventory {
        return pages.first()
    }

    fun getLast(): Inventory {
        return pages.last()
    }

    fun indexOf(page: Inventory): Int {
        return pages.indexOf(page)
    }

    fun getNext(page: Inventory): Inventory? {
        val index = pages.indexOf(page)
        return if (index == -1 || index + 1 >= pages.size) null else pages[index + 1]
    }

    fun getPrevious(page: Inventory): Inventory? {
        val index = pages.indexOf(page)
        return if (index <= 0) null else pages[index - 1]
    }

    class Builder {
        private var name: String = "Paginated GUI"
        private lateinit var base: Inventory
        private var items: List<ItemStack> = emptyList()
        private var startEndSlots: Pair<Int, Int> = Pair(0, 0)
        private var ignoredSlots: List<Int> = emptyList()

        fun name(name: String) = apply { this.name = name }
        fun base(base: Inventory) = apply { this.base = base }
        fun items(items: List<ItemStack>) = apply { this.items = items }
        fun startEndSlots(start: Int, end: Int) = apply { this.startEndSlots = Pair(start, end) }
        fun ignoredSlots(vararg ignoredSlots: Int) = apply { this.ignoredSlots = ignoredSlots.toList() }

        fun build() = PaginatedGui(name, base, items, startEndSlots, ignoredSlots)
    }
}