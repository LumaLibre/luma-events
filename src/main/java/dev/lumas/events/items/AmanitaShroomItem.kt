package dev.lumas.events.items

import dev.lumas.events.EventMain
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import dev.lumas.lumaitems.util.extensions.isMatchingItem
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe

class AmanitaShroomItem : CustomItemFunctionsWithRecipe() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#da2022:#ea6867:#efa2a1:#f7f1f0:#ceda5c>Aminata Shroom</gradient></b>")
            .material(Material.RED_MUSHROOM)
            .lore(
                "A small mushroom",
                "that also smells",
                "quite odd.",
                "",
                "Trade enough of these",
                "in for special rewards."
            )
            .persistentData("aminata-shroom")
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .tier(Tier.WONDERLAND_2026)
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (item.isMatchingItem("aminata-shroom")) {
            event.isCancelled = true
        }
    }

    override fun recipe(): Pair<NamespacedKey, Recipe> {
        val key = NamespacedKey(EventMain.getInstance(), "aminata-shroom")
        val lesserToken = WaxcapShroomItem().createItem().second.asOne()
        val recipe = ShapelessRecipe(key, this.createItem().second.asOne())

        recipe.addIngredient(4, lesserToken)

        return Pair(key, recipe)
    }
}