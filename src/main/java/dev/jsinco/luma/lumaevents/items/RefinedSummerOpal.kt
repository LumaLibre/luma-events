package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe

class RefinedSummerOpal : CustomItemFunctionsWithRecipe() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#DC8BE5:#F6FF5C>Refined Summer Opal</gradient></b>")
            .lore(
                "<gray>This opal has been refined to",
                "<gray>its purest form, radiating a",
                "<gray>brilliant iridescence.",
                "",
                "<gray>You might be able to break",
                "<gray>this down into multiple",
                "<gray><gradient:#DC8BE5:#F6FF5C>Summer Opals</gradient> for trading."
            )
            .material(Material.RESIN_BRICK)
            .tier(Tier.SUMMER_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("refined-summer-opal-token")
            .buildPair()
    }

    override fun recipe(): Pair<NamespacedKey, Recipe> {
        val key = NamespacedKey(EventMain.getInstance(), "refined-summer-opal")
        val summerOpalItemStack = SummerOpal().createItem().second.asQuantity(3)
        val shapelessRecipe = ShapelessRecipe(key, summerOpalItemStack)
        shapelessRecipe.addIngredient(createItem().second)
        return Pair(key, shapelessRecipe)
    }

}
