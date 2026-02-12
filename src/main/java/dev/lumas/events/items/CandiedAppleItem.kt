package dev.lumas.events.items

import dev.lumas.lumaitems.items.ItemFactory
import dev.lumas.lumaitems.model.CustomItemFunctions
import dev.lumas.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class CandiedAppleItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#890100:#a96632:#f7c569>Candied Apple</gradient></b>")
            .lore(
                "A tasty little apple",
                "covered in red glaze.",
                "",
                "Craft <#890100>9</#890100> of these into",
                "a different sweet!"
            )
            .material(Material.APPLE)
            .tier(Tier.VALENTIDE_2026)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("candied-apple")
            .buildPair()
    }

}
