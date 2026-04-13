package dev.lumas.events.items

import dev.lumas.events.utility.Util
import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack

class CandiedAppleItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#DD2785:#fc5b8d:#cb354e>Candied Apple</gradient></b>")
            .lore(
                "A tasty little apple",
                "covered in red glaze.",
                "",
                "Craft <#E95F76>9</#E95F76> of these into",
                "a different sweet!"
            )
            .material(Material.APPLE)
            .tier(Tier.VALENTIDE_2026)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("candied-apple")
            .buildPair()
    }

    override fun onConsumeItem(player: Player, event: PlayerItemConsumeEvent) {
        Util.sendMsg(player, "You can't eat that, silly!")
        event.isCancelled = true
    }

}
