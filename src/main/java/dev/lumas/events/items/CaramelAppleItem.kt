package dev.lumas.events.items

import dev.lumas.events.EventMain
import dev.lumas.events.utility.Util
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe

class CaramelAppleItem : CustomItemFunctionsWithRecipe() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#a76d3c:#d2b48c:#c79c6b:#f3e7c4:#9b7652>Caramel Apple</gradient></b>")
            .material(Material.GOLDEN_APPLE)
            .lore(
                "A tasty little apple",
                "covered in a perfect",
                "caramel bliss.",
                "",
                "Trade enough of these",
                "in for special rewards."
            )
            .persistentData("caramel-apple")
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .tier(Tier.VALENTIDE_2026)
            .buildPair()
    }

    override fun onConsumeItem(player: Player, event: PlayerItemConsumeEvent) {
        Util.sendMsg(player, "You can't eat that, silly!")
        event.isCancelled = true
    }

    override fun recipe(): Pair<NamespacedKey, Recipe> {
        val key = NamespacedKey(EventMain.getInstance(), "caramel-apple")
        val candiedApple = CandiedAppleItem().createItem().second.asOne()
        val recipe = ShapedRecipe(key, this.createItem().second.asOne())
        recipe.shape(
            "AAA",
            "AAA",
            "AAA"
        )
        recipe.setIngredient('A', candiedApple)
        return Pair(key, recipe)
    }
}