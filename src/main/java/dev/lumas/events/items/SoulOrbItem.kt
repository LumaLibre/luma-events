package dev.lumas.events.items

import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class SoulOrbItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#3F255E:#9f2e51:#611e4e:#331D4D>Soul Orb</gradient></b>")
            .lore(
                "A faint, flickering ember",
                "torn from a wandering spirit.",
                "It hums softly when held,",
                "as if it remembers you.",
                "",
                "<#9f2e51>Trade</#9f2e51> enough of these",
                "in for special rewards!"
            )
            .material(Material.BLACK_DYE)
            .tier(Tier.LUMARES_2026)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("soul-orb")
            .buildPair()
    }

}
